#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
"""Verify immutable CI fixture identity and archive bytes with the standard library."""
import hashlib
import json
import os
import pathlib
import platform
import re
import shlex
import subprocess
import sys

import release

def build_inputs(include_sdk=False):
    repo = pathlib.Path(__file__).resolve().parents[3]
    patterns = (
        '.github/actions/setup-hugegraph-server/action.yml',
        'hugegraph-client/assembly/travis/fixture.sh',
        'hugegraph-client/assembly/travis/manifest.py',
        'hugegraph-client/assembly/travis/release.py',
        '.github/actions/setup-java-env/action.yml', '.github/configs/settings.xml',
        'hugegraph-client/assembly/travis/checkout-server.sh',
        'hugegraph-client/assembly/travis/install-candidate-sdk.sh',
        'hugegraph-client/assembly/travis/start-hugegraph-servers.sh',
        'hugegraph-*/assembly/travis/install-hugegraph-from-source.sh',
        'hugegraph-hubble/hubble-dist/assembly/travis/download-hugegraph.sh',
    )
    if include_sdk:
        patterns += ('.github/actions/setup-candidate-sdk/action.yml',
                     '.github/actions/get-hugegraph-commit/action.yml',
                     '.github/actions/get-hugegraph-commit/resolve.cjs',
                     'hugegraph-client/assembly/travis/verify_candidate_image_sdk.py')
    inputs = hashlib.sha256()
    files = sorted({p for pattern in patterns for p in repo.glob(pattern)})
    for path in files:
        inputs.update(str(path.relative_to(repo)).encode() + b"\0" + path.read_bytes())
    return inputs.hexdigest()


def sdk_build_identity():
    java = pathlib.Path(os.environ['JAVA_HOME']) / 'bin/java'
    result = subprocess.run([str(java), '-XshowSettings:properties', '-version'],
                            capture_output=True, text=True, check=True, timeout=30)
    names = ('java.vendor', 'java.runtime.version', 'java.vm.version', 'java.specification.version', 'os.arch')
    properties = dict(re.findall(r'^\s*(' + '|'.join(re.escape(name) for name in names) +
                                 r') = (.+)$', result.stderr, re.MULTILINE))
    if set(properties) != set(names) or properties['java.specification.version'] != '17':
        raise ValueError('SDK fixture requires complete JDK 17 build identity')
    result = subprocess.run(['mvn', '-version'], capture_output=True, text=True, check=True, timeout=30)
    version = re.search(r'^Apache Maven [^\r\n]+', re.sub(r'\x1b\[[0-9;]*m', '', result.stdout), re.MULTILINE)
    if not version:
        raise ValueError('SDK fixture requires a concrete Maven build identity')
    # Consumer coordinates and job heap limits do not change the SDK build contract.
    consumer_properties = ('-Dmaven.repo.local=', '-Dhugegraph.version=', '-Dsdk.validation.mode=')
    workspace = os.environ.get('GITHUB_WORKSPACE', '')
    arguments = [value.replace(workspace, '${WORKSPACE}') if workspace else value
                 for value in shlex.split(os.environ.get('MAVEN_ARGS', ''))
                 if not value.startswith(consumer_properties)]
    heap_budget = re.compile(r'(?:-Xm[xs]|-XX:(?:MaxHeapSize|InitialHeapSize|MaxMetaspaceSize)=)\d+[kKmMgGtT]?')
    jvm_arguments = [value.replace(workspace, '${WORKSPACE}') if workspace else value
                     for value in shlex.split(os.environ.get('MAVEN_OPTS', ''))
                     if not heap_budget.fullmatch(value) and not value.startswith(consumer_properties)]
    options = hashlib.sha256(json.dumps([arguments, jvm_arguments]).encode()).hexdigest()
    return dict(schema=1, platform=[platform.system(), platform.machine()], java=properties,
                maven=version.group(0), options_sha256=options, inputs=build_inputs(include_sdk=True))


if sys.argv[1] == "inputs":
    print(build_inputs())
    sys.exit(0)
if sys.argv[1] == "sdk-identity":
    try:
        print(hashlib.sha256(json.dumps(sdk_build_identity(), sort_keys=True).encode()).hexdigest())
    except (OSError, ValueError, KeyError, subprocess.SubprocessError):
        sys.exit('Cannot establish SDK fixture build identity')
    sys.exit(0)

root = pathlib.Path(os.environ["FIXTURE_DIR"])
identity = {key.lower(): os.environ["FIXTURE_" + key] for key in
            ("REPOSITORY", "COMMIT", "JAVA", "CONFIG")}
try:
    if identity["java"] == "11":
        identity.update(release.identity(os.environ.get("FIXTURE_RELEASE_VERSION", release.VERSION),
                                        identity["repository"], identity["commit"]))
    else:
        identity.update(source_kind="candidate-sdk")
    export_sdk = os.environ.get('FIXTURE_EXPORT_CANDIDATE_SDK', 'false') == 'true'
    if export_sdk:
        if identity['java'] != '17':
            raise ValueError('SDK fixture export requires JDK 17')
        identity['sdk_build'] = sdk_build_identity()
except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
    sys.exit(str(error))


def digest(path=None):
    h = hashlib.sha256()
    with (path or root / "server.tar.gz").open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def verify_sdk():
    repository = root / 'sdk'
    if not repository.is_dir() or repository.is_symlink() or any(p.is_symlink() for p in repository.rglob('*')):
        raise ValueError('SDK fixture must contain regular files')
    manifest = repository / 'candidate-sdk-manifest.json'
    revision = json.loads(manifest.read_text())['source_revision']
    result = subprocess.run([sys.executable, str(pathlib.Path(__file__).with_name('verify_candidate_image_sdk.py')),
                             str(repository), '--version', revision], capture_output=True, text=True,
                            env=dict(os.environ, CANDIDATE_SOURCE_COMMIT=identity['commit']), timeout=30)
    if result.returncode:
        raise ValueError('SDK fixture manifest or artifacts failed validation')
    return digest(manifest)


try:
    if root.is_symlink() or not root.is_dir():
        raise ValueError('fixture must be a regular directory')
    if identity["java"] == "11":
        release.verify(root / "server.tar.gz")
    if sys.argv[1] == "create":
        data = dict(identity, archive_name=os.environ["FIXTURE_ARCHIVE_NAME"], sha256=digest())
        if export_sdk:
            data['sdk_manifest_sha256'] = verify_sdk()
        (root / "manifest.json").write_text(json.dumps(data, sort_keys=True) + "\n")
    else:
        if (root / "manifest.json").is_symlink() or (root / "server.tar.gz").is_symlink():
            raise ValueError("fixture must contain regular files")
        data = json.loads((root / "manifest.json").read_text())
        if any(data.get(key) != value for key, value in identity.items()):
            raise ValueError("fixture origin, commit, JDK or build configuration mismatch")
        if not re.fullmatch(r"apache-hugegraph-[A-Za-z0-9_.-]+\.tar\.gz", data.get("archive_name", "")):
            raise ValueError("invalid archive name")
        if identity["java"] == "11" and data.get("archive_name") != release.ARCHIVE_NAME:
            raise ValueError("official Server release archive name mismatch")
        if data.get("sha256") != digest():
            raise ValueError("fixture checksum mismatch")
        if export_sdk and data.get('sdk_manifest_sha256') != verify_sdk():
            raise ValueError('SDK fixture manifest checksum mismatch')
        if sys.argv[1] == "name":
            print(data["archive_name"])
except (OSError, ValueError, KeyError, TypeError, subprocess.SubprocessError) as error:
    sys.exit(str(error))
