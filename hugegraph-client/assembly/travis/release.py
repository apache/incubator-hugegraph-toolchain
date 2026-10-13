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
"""Pinned ASF binary release identity and bounded download, without source fallback."""
import hashlib
import os
import pathlib
import subprocess
import sys
import tempfile

VERSION = "1.7.0"
REPOSITORY = "apache/hugegraph"
COMMIT = "b12425c2032bf0d21a97b8221f42a18055c2982f"
ARCHIVE_NAME = "apache-hugegraph-incubating-1.7.0.tar.gz"
SOURCE_URL = "https://downloads.apache.org/hugegraph/1.7.0/" + ARCHIVE_NAME
ARCHIVE_URL = "https://archive.apache.org/dist/hugegraph/1.7.0/" + ARCHIVE_NAME
# Verified against SOURCE_URL + ".sha512" in the official ASF distribution.
SHA512 = ("ba093203e817f17582895ff10ceb0458498c886ec7a687fd0f3e5f56ba739454"
          "ec928a592bdad8a3685651dc9f8ef904d082f9530d4aae6a83b5d9d9626ad13f")


def identity(version, repository=REPOSITORY, commit=COMMIT):
    if version != VERSION:
        raise ValueError("unsupported Server release version: " + version)
    if repository != REPOSITORY or commit != COMMIT:
        raise ValueError("official Server release requires " + REPOSITORY + "@" + COMMIT)
    return dict(source_kind="asf-release", release_version=VERSION,
                source_repository=REPOSITORY, source_commit=COMMIT,
                source_url=SOURCE_URL, official_sha512=SHA512)


def digest(path):
    result = hashlib.sha512()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            result.update(chunk)
    return result.hexdigest()


def verify(path):
    if not path.is_file() or path.is_symlink() or digest(path) != SHA512:
        raise ValueError("official Server release SHA-512 mismatch")


def download(directory):
    directory.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix=".server-release-", dir=directory)
    os.close(descriptor)
    path = pathlib.Path(temporary)
    try:
        command = ["curl", "--fail", "--location", "--show-error", "--silent",
                   "--connect-timeout", "20", "--max-time", "900", "--retry", "2",
                   "--proto", "=https", "--proto-redir", "=https",
                   "--output", str(path)]
        try:
            subprocess.run(command + [SOURCE_URL], check=True)
        except subprocess.CalledProcessError:
            # Superseded releases leave downloads but remain in the ASF archive.
            # The manifest identity stays canonical regardless of transport.
            print("ASF distribution download failed; trying the official archive", file=sys.stderr)
            subprocess.run(command + [ARCHIVE_URL], check=True)
        # A checksum mismatch is fatal; never hide it by trying another source.
        verify(path)
        path.replace(directory / "server.tar.gz")
    finally:
        path.unlink(missing_ok=True)


if __name__ == "__main__":
    try:
        identity(os.environ.get("FIXTURE_RELEASE_VERSION", VERSION),
                 os.environ["FIXTURE_REPOSITORY"], os.environ["FIXTURE_COMMIT"])
        if sys.argv[1] == "validate":
            pass
        elif sys.argv[1] == "download":
            download(pathlib.Path(os.environ["FIXTURE_DIR"]))
        elif sys.argv[1] == "name":
            print(ARCHIVE_NAME)
        else:
            raise ValueError("invalid release helper command")
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
