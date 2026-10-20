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
"""Content-validated archive generation. Distribution audits are never cached.

Reuse preserves the original archive's member timestamps. Source mtime-only
changes and identical-content assembly recopy do not require fresh timestamps.
Per-invocation stat snapshots still detect source changes during verification.
"""

import argparse
import fnmatch
import grp
import hashlib
import json
import os
from pathlib import Path
import pwd
import shutil
import stat
import subprocess
import sys
import tempfile


RUNTIME_DIRECTORIES = {"data", "logs", "upload-files"}
RUNTIME_PATTERNS = ("*.db", "*.lock", "*.log", "*.pid")
SCHEMA = 1


def digest(path, expected=None):
    checksum = hashlib.sha256()
    descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(descriptor, "rb") as stream:
        before = os.fstat(stream.fileno())
        if not stat.S_ISREG(before.st_mode) or (expected is not None and signature(before) != signature(expected)):
            raise RuntimeError("Archive input changed before hashing: " + str(path))
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(block)
        if signature(os.fstat(stream.fileno())) != signature(before) or signature(path.lstat()) != signature(before):
            raise RuntimeError("Archive input changed during hashing: " + str(path))
    return checksum.hexdigest()


def excluded(parts):
    return (parts[0] in RUNTIME_DIRECTORIES or any(
        name.startswith("._") or name == "pid" or any(fnmatch.fnmatchcase(name, p) for p in RUNTIME_PATTERNS)
        for name in parts))


def signature(value):
    return (value.st_dev, value.st_ino, value.st_mode, value.st_uid, value.st_gid,
            value.st_size, value.st_nlink, value.st_mtime_ns, value.st_ctime_ns)


def owner_name(identifier, group=False):
    try:
        return grp.getgrgid(identifier).gr_name if group else pwd.getpwuid(identifier).pw_name
    except KeyError:
        return ""


def inventory(release):
    files = {}
    snapshots = {}
    inodes = set()
    locations = {}

    def walk(path, relative):
        if any(ord(character) < 32 or ord(character) == 127 for character in relative):
            raise RuntimeError("Control characters are not allowed in archive paths")
        before = path.lstat()
        snapshots[relative] = signature(before)
        regular = stat.S_ISREG(before.st_mode)
        if not regular and not stat.S_ISDIR(before.st_mode):
            raise RuntimeError("Archive sources must be regular files/directories: " + relative)
        entry = {"type": "file" if regular else "directory", "mode": stat.S_IMODE(before.st_mode),
                 "uid": before.st_uid, "gid": before.st_gid,
                 "uname": owner_name(before.st_uid), "gname": owner_name(before.st_gid, True),
                 "flags": getattr(before, "st_flags", 0)}
        if hasattr(os, "listxattr"):
            entry["xattrs"] = {name: hashlib.sha256(os.getxattr(path, name, follow_symlinks=False)).hexdigest()
                              for name in sorted(os.listxattr(path, follow_symlinks=False))}
        locations[str(path)] = relative
        if regular:
            inode = (before.st_dev, before.st_ino)
            if inode in inodes:
                raise RuntimeError("Archive sources must not contain hard-link aliases: " + relative)
            inodes.add(inode)
            entry.update({"size": before.st_size, "links": before.st_nlink, "sha256": digest(path, before)})
            if signature(path.lstat()) != signature(before):
                raise RuntimeError("Archive source changed during hashing: " + relative)
        files[relative] = entry
        if not regular:
            for child in sorted(path.iterdir()):
                parts = child.relative_to(release).parts
                if not excluded(parts):
                    walk(child, "/".join(parts))
    walk(release, ".")
    # Linux ACLs are xattrs above; macOS exposes extended ACLs through ls -e.
    # Ignore ls's timestamp columns so ordinary assembly recopy does not miss.
    if sys.platform == "darwin":
        # CPython on macOS does not expose os.listxattr/getxattr. Read all
        # included paths in one native invocation, with hex values kept private.
        attributes = subprocess.check_output(["xattr", "-lsx", *locations])
        files["."]["nativeXattrs"] = hashlib.sha256(attributes).hexdigest()
        output = subprocess.check_output(["ls", "-lde", *locations], text=True,
                                         env={**os.environ, "LC_ALL": "C"})
        current = None
        for line in output.splitlines():
            if line.lstrip()[:1].isdigit() and ": " in line and line[:1].isspace():
                if current is None:
                    raise RuntimeError("Unable to associate extended ACL with an archive source")
                files[current].setdefault("acl", []).append(line.strip())
            else:
                current = next((relative for path, relative in locations.items() if line.endswith(path)), None)
                if current is None:
                    raise RuntimeError("Unable to parse archive source ACL metadata")
    return files, snapshots


def tool_identity(name):
    executable = shutil.which(name)
    if not executable:
        raise RuntimeError("Required archive tool not found: " + name)
    return {"name": name, "binary": digest(Path(executable).resolve())}


def input_identity(release):
    helper = Path(__file__).resolve()
    tools = [tool_identity("tar"), tool_identity("gzip")]
    if sys.platform == "darwin":
        tools.extend([tool_identity("ls"), tool_identity("xattr")])
    # Values remain inside a digest; no environment contents are persisted.
    environment = {name: os.environ.get(name) for name in
                   ("TAR_OPTIONS", "TAR_READER_OPTIONS", "TAR_WRITER_OPTIONS", "GZIP", "POSIXLY_CORRECT")}
    files, snapshots = inventory(release)
    values = {"schema": SCHEMA, "root": release.name, "files": files, "tools": tools,
              "environment": environment, "platform": sys.platform,
              "helper": digest(helper), "wrapper": digest(helper.with_name("archive-hubble.sh")),
              "excludedDirectories": sorted(RUNTIME_DIRECTORIES), "excludedPatterns": RUNTIME_PATTERNS,
              "copyfileDisable": "1"}
    # Persist content identity only; within one invocation also compare every
    # included file/directory's inode/mtime/ctime to detect change-and-restore.
    return hashlib.sha256(json.dumps(values, sort_keys=True).encode()).hexdigest(), snapshots


def validate_output_type(output):
    try:
        mode = output.lstat().st_mode
    except FileNotFoundError:
        return
    if not stat.S_ISREG(mode):
        raise RuntimeError("Existing archive output must be a regular file: " + str(output))


def archive(release, output):
    release = Path(os.path.abspath(release))
    output = Path(os.path.abspath(output))
    validate_output_type(output)
    if output.resolve().is_relative_to(release.resolve()):
        raise RuntimeError("Archive output must be a regular file outside its release tree")
    output.parent.mkdir(parents=True, exist_ok=True)
    manifest = output.with_name(output.name + ".cache.json")
    lock = output.with_name(output.name + ".archive-lock")
    try:
        lock.mkdir()
    except FileExistsError:
        raise RuntimeError("Archive generation is locked; verify the other process stopped before removing "
                           + str(lock))
    temporary = None
    manifest_temporary = None
    try:
        if release.is_symlink() or not release.is_dir() or release.name.startswith("._"):
            raise RuntimeError("Release must be a real, non-excluded directory")
        identity, snapshots = input_identity(release)
        try:
            saved = json.loads(manifest.read_text()) if not manifest.is_symlink() else {}
        except (FileNotFoundError, ValueError):
            saved = {}
        if not isinstance(saved, dict):
            saved = {}
        # Custom native options can reference files outside the release tree
        # (for example --exclude-from). Preserve them, but never cache them.
        cacheable = not any(os.environ.get(name) for name in
                            ("TAR_OPTIONS", "TAR_READER_OPTIONS", "TAR_WRITER_OPTIONS"))
        if (cacheable and saved.get("schema") == SCHEMA and saved.get("input") == identity and output.is_file() and
                saved.get("size") == output.stat().st_size and saved.get("sha256") == digest(output)):
            if input_identity(release) != (identity, snapshots):
                raise RuntimeError("Release changed while verifying its existing archive")
            print("Archive unchanged: verified complete tar.gz SHA256; reusing " + str(output))
            return
        manifest.unlink(missing_ok=True)
        descriptor, name = tempfile.mkstemp(prefix="." + output.name + ".", suffix=".tmp", dir=output.parent)
        temporary = Path(name)
        # Match tar's normal create/truncate permissions despite atomic staging.
        mask = os.umask(0)
        os.umask(mask)
        mode = stat.S_IMODE(output.stat().st_mode) if output.exists() else 0o666 & ~mask
        os.fchmod(descriptor, mode)
        os.close(descriptor)
        excludes = ["._*", "*/._*"] + [release.name + "/" + name for name in sorted(RUNTIME_DIRECTORIES)]
        excludes += list(RUNTIME_PATTERNS) + ["*/pid"]
        command = ["tar", "-czf", str(temporary)] + ["--exclude=" + value for value in excludes]
        command += ["-C", str(release.parent), "--", release.name]
        subprocess.run(command, check=True, env={**os.environ, "COPYFILE_DISABLE": "1"})
        if input_identity(release) != (identity, snapshots):
            raise RuntimeError("Release changed during archive generation; output was not published")
        certificate = {"schema": SCHEMA, "input": identity, "size": temporary.stat().st_size,
                       "sha256": digest(temporary)}
        # Finish every certificate write before replacing the existing archive.
        # Publish its manifest first: until the archive rename, complete-output
        # SHA verification rejects it (unless the output is already identical).
        # A failed final rename removes the manifest and leaves the old archive.
        descriptor, name = tempfile.mkstemp(prefix="." + manifest.name + ".", suffix=".tmp", dir=output.parent)
        manifest_temporary = Path(name)
        with os.fdopen(descriptor, "w") as stream:
            json.dump(certificate, stream, sort_keys=True)
            stream.write("\n")
        manifest_temporary.replace(manifest)
        manifest_temporary = None
        validate_output_type(output)
        temporary.replace(output)
        temporary = None
        print("Archive generated and fingerprinted: " + str(output))
    except BaseException:
        # A failed invocation must never certify an old archive for new input.
        manifest.unlink(missing_ok=True)
        raise
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
        if manifest_temporary is not None:
            manifest_temporary.unlink(missing_ok=True)
        lock.rmdir()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("release")
    parser.add_argument("archive")
    args = parser.parse_args()
    archive(args.release, args.archive)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
