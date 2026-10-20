#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Invalidate native compiler state only when its external inputs change.

Compiler 3.x catches reactor files changed during the current Maven invocation,
not JARs replaced before it started. Keep source/resource handling in Maven.
"""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shlex
import shutil
import sys


OUTPUTS = ("classes", "test-classes", "generated-sources", "generated-test-sources", "maven-status")
NON_COMPILER_PROPERTIES = {"skipTests", "maven.test.skip", "maven.javadoc.skip", "checkstyle.skip",
                           "rat.skip", "skipRat", "jacoco.skip", "maven.compiler.showCompilationChanges",
                           "test", "it.test", "skipITs", "groups", "excludedGroups"}
# Test execution settings do not alter compiler state; model/version properties
# such as maven.surefire.plugin.version remain part of the identity.
NON_COMPILER_PREFIXES = ("surefire.", "failsafe.")


def digest(file):
    checksum = hashlib.sha256()
    with file.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(chunk)
    return checksum.hexdigest()


def compiler_arguments(command_line):
    """Only retain model/compiler inputs; never persist the command line itself."""
    result = []
    arguments = iter(shlex.split(command_line))
    for argument in arguments:
        if argument in ("-D", "-P"):
            argument += next(arguments, "")
        if argument.startswith("-D"):
            name = argument[2:].split("=", 1)[0]
            if name not in NON_COMPILER_PROPERTIES and not name.startswith(NON_COMPILER_PREFIXES):
                result.append(argument)
        elif argument.startswith("-P"):
            result.append(argument)
    return sorted(result)


def input_identity(project, classpath, sdk_identity, java_identity, arguments=()):
    project = Path(project).resolve()
    target = project / "target"
    if target.is_symlink():
        raise RuntimeError("Refusing a symlinked Java build directory")
    dependencies = []
    for entry in classpath:
        file = Path(entry).resolve()
        # The current module's output is expected to change during compilation.
        # Other reactor modules remain external inputs and must be fingerprinted.
        if file.is_relative_to(target):
            continue
        if file.is_file():
            dependencies.append([str(file), digest(file)])
        elif file.is_dir():
            dependencies.append([str(file), [[p.relative_to(file).as_posix(), digest(p)]
                                           for p in sorted(file.rglob("*")) if p.is_file()]])
        else:
            raise RuntimeError("Missing resolved Java classpath entry: " + str(file))
    poms = [[str(directory / "pom.xml"), digest(directory / "pom.xml")]
            for directory in (project, project.parent, project.parent.parent)]
    inputs = {"schema": 1, "dependencies": dependencies, "poms": poms,
              "sdk": sdk_identity, "java": java_identity, "arguments": list(arguments),
              "guard": digest(Path(__file__))}
    return hashlib.sha256(json.dumps(inputs, sort_keys=True).encode()).hexdigest()


def invalidate(project, identity):
    project = Path(project).resolve()
    target = project / "target"
    if target.is_symlink():
        raise RuntimeError("Refusing a symlinked Java build directory")
    target.mkdir(exist_ok=True)
    stamp = target / "external-build-inputs.json"
    try:
        previous = json.loads(stamp.read_text())
    except (FileNotFoundError, ValueError):
        previous = {}
    if previous == {"schema": 1, "identity": identity}:
        return False
    for name in OUTPUTS:
        output = target / name
        if output.is_symlink():
            output.unlink()
        elif output.exists():
            shutil.rmtree(output)
    # This records input identity, not successful compilation or reusable output.
    # All Maven compiler, resource, test and packaging goals still execute.
    temporary = stamp.with_suffix(f".{os.getpid()}.tmp")
    temporary.write_text(json.dumps({"schema": 1, "identity": identity}) + "\n")
    temporary.replace(stamp)
    return True


def sdk_identity(repository, mode, version):
    repository = Path(repository)
    verifier_path = Path(__file__).resolve().parents[4] / \
        "hugegraph-client/assembly/travis/verify_candidate_image_sdk.py"
    spec = importlib.util.spec_from_file_location("hubble_sdk_verifier", verifier_path)
    verifier = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(verifier)
    if mode == "candidate":
        manifest, jars = verifier.validate_sdk(repository, version)
        return {"mode": mode, "manifest": manifest, "jars": jars}
    if mode == "release":
        return {"mode": mode, "jars": verifier.validate_release_sdk(repository, version, module="hubble")}
    raise RuntimeError("Unsupported SDK validation mode: " + mode)


def main():
    if sys.version_info < (3, 9):
        raise RuntimeError("Hubble Java build guard requires Python 3.9 or newer")
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("project", "repository", "sdk-mode", "sdk-version", "java-home", "java-version", "classpath"):
        parser.add_argument("--" + name, required=True)
    args = parser.parse_args()
    # Validate SDK bytes before trusting their manifest as a cache identity.
    sdk = sdk_identity(args.repository, args.sdk_mode, args.sdk_version)
    identity = input_identity(args.project, args.classpath.split(os.pathsep), sdk,
                              [args.java_home, args.java_version],
                              compiler_arguments(" ".join(os.environ.get(name, "") for name in
                                                           ("MAVEN_CMD_LINE_ARGS", "MAVEN_OPTS"))))
    if invalidate(args.project, identity):
        print("External Java inputs changed: invalidated native compiler outputs.")
    else:
        print("External Java inputs unchanged: keeping native compiler state.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
