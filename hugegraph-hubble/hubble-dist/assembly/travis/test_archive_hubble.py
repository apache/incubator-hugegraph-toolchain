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
"""Exercise the actual archive script without building Hubble or deleting data."""

import hashlib
import importlib.util
import json
import os
import pathlib
import shlex
import shutil
import socket
import stat
import subprocess
import sys
import tarfile
import tempfile
import time
import unittest
from unittest import mock


ARCHIVER = pathlib.Path(__file__).with_name("archive-hubble.sh")
SPEC = importlib.util.spec_from_file_location("archive_hubble", ARCHIVER.with_name("archive_hubble.py"))
ARCHIVE_MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ARCHIVE_MODULE)


class HubbleArchiveTest(unittest.TestCase):
    def test_runtime_data_is_preserved_and_excluded_from_image_input(self):
        with tempfile.TemporaryDirectory(prefix="hubble-archive-") as directory:
            root = pathlib.Path(directory)
            release = root / "apache-hugegraph-hubble-1.8.0"
            runtime_files = (
                "data/hubble-v2.mv.db", "data/hubble-v2.trace.db",
                "db.mv.db", "db.trace.db", "logs/application.log",
                "upload-files/private.csv", "hubble.pid", "bin/pid",
                "metadata.lock",
            )
            for relative in runtime_files:
                path = release / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(("preserve:" + relative).encode())
            sentinels = {relative: (release / relative).read_bytes()
                         for relative in runtime_files}
            (release / "lib").mkdir()
            (release / "lib/hubble.jar").write_bytes(b"application")
            sidecar = release / "lib/._hubble.jar"
            sidecar.write_bytes(b"existing AppleDouble metadata")
            (release / "ui").mkdir()
            (release / "ui/index.html").write_text("<html>Hubble</html>")
            launcher = release / "bin/start-hubble.sh"
            launcher.write_text("#!/bin/sh\nexit 0\n")
            launcher.chmod(0o755)
            archive = root / "target/hubble.tar.gz"

            for attempt in range(2):
                subprocess.run(["bash", str(ARCHIVER), str(release), str(archive)],
                               check=True)
                with tarfile.open(archive) as package:
                    names = package.getnames()
                    for relative in runtime_files:
                        self.assertNotIn(f"{release.name}/{relative}", names)
                    for runtime_dir in ("data", "logs", "upload-files"):
                        self.assertNotIn(f"{release.name}/{runtime_dir}", names)
                    self.assertIn(f"{release.name}/lib/hubble.jar", names)
                    self.assertIn(f"{release.name}/ui/index.html", names)
                    self.assertEqual(0o755, package.getmember(
                        f"{release.name}/bin/start-hubble.sh").mode & 0o777)
                    self.assertFalse(any(pathlib.PurePosixPath(name).name.startswith("._")
                                         for name in names))

                # Docker uses the archive in a fresh build-stage directory.
                image_input = root / f"fresh-image-{attempt}"
                image_input.mkdir()
                subprocess.run(["tar", "-xzf", str(archive),
                                "--strip-components=1", "-C", str(image_input)],
                               check=True)
                self.assertEqual(b"application",
                                 (image_input / "lib/hubble.jar").read_bytes())
                self.assertEqual(0o755,
                                 (image_input / "bin/start-hubble.sh").stat().st_mode & 0o777)
                for relative, original in sentinels.items():
                    self.assertFalse((image_input / relative).exists())
                    self.assertEqual(original, (release / relative).read_bytes())
                self.assertEqual(b"existing AppleDouble metadata", sidecar.read_bytes())

    @unittest.skipUnless(sys.platform == "darwin", "requires macOS extended attributes")
    def test_extended_attributes_do_not_generate_appledouble_members(self):
        with tempfile.TemporaryDirectory(prefix="hubble-archive-xattr-") as directory:
            root = pathlib.Path(directory)
            release = root / "hubble"
            release.mkdir()
            payload = release / "application.jar"
            payload.write_bytes(b"application")
            attribute = "com.apple.metadata:hugegraph-archive-test"
            subprocess.run(["xattr", "-w", attribute, "preserve attribute", str(payload)],
                           check=True)
            archive = root / "hubble.tar.gz"
            subprocess.run(["bash", str(ARCHIVER), str(release), str(archive)], check=True)
            with tarfile.open(archive) as package:
                self.assertEqual(["hubble", "hubble/application.jar"], package.getnames())
                self.assertEqual(b"application",
                                 package.extractfile("hubble/application.jar").read())
            self.assertEqual(b"preserve attribute", subprocess.check_output(
                ["xattr", "-p", attribute, str(payload)]).strip())

    def test_missing_release_fails_without_creating_archive(self):
        with tempfile.TemporaryDirectory(prefix="hubble-archive-") as directory:
            root = pathlib.Path(directory)
            archive = root / "hubble.tar.gz"
            result = subprocess.run(["bash", str(ARCHIVER), str(root / "missing"),
                                     str(archive)], capture_output=True)
            self.assertNotEqual(0, result.returncode)
            self.assertFalse(archive.exists())


class HubbleArchiveCacheTest(unittest.TestCase):
    def setUp(self):
        self.sandbox = tempfile.TemporaryDirectory(prefix="hubble-archive-cache-")
        self.addCleanup(self.sandbox.cleanup)
        self.root = pathlib.Path(self.sandbox.name)
        self.release = self.root / "hubble"
        self.release.mkdir()
        self.payload = self.release / "application.jar"
        self.payload.write_bytes(b"application")
        self.output = self.root / "target/hubble.tar.gz"
        self.manifest = self.output.with_name(self.output.name + ".cache.json")
        self.counter = self.root / "tar-invocations"
        binary = self.root / "bin"
        binary.mkdir()
        self.tar_wrapper = binary / "tar"
        self.tar_wrapper.write_text("#!/bin/sh\n"
            'printf "tar\\n" >> "$ARCHIVE_TEST_COUNTER"\n'
            'if [ -n "$ARCHIVE_TEST_BLOCK" ]; then\n'
            '  touch "$ARCHIVE_TEST_BLOCK.ready"\n'
            '  while [ ! -e "$ARCHIVE_TEST_BLOCK.release" ]; do sleep 0.02; done\n'
            'fi\n'
            'if [ -n "$ARCHIVE_TEST_FAIL" ]; then printf partial > "$2"; exit 19; fi\n'
            'if [ -n "$ARCHIVE_TEST_MUTATE" ]; then printf changed >> "$ARCHIVE_TEST_MUTATE"; fi\n'
            'if [ -n "$ARCHIVE_TEST_RESTORE" ]; then\n'
            '  cp -p "$ARCHIVE_TEST_RESTORE" "$ARCHIVE_TEST_BACKUP"\n'
            '  printf temporarily-changed > "$ARCHIVE_TEST_RESTORE"\n'
            "  " + shlex.quote(shutil.which("tar")) + ' "$@"\n'
            '  status=$?\n'
            '  cp -p "$ARCHIVE_TEST_BACKUP" "$ARCHIVE_TEST_RESTORE"\n'
            '  rm "$ARCHIVE_TEST_BACKUP"\n'
            '  exit "$status"\n'
            'fi\n'
            "exec " + shlex.quote(shutil.which("tar")) + ' "$@"\n')
        self.tar_wrapper.chmod(0o755)
        self.env = {**os.environ, "PATH": str(binary) + os.pathsep + os.environ["PATH"],
                    "ARCHIVE_TEST_COUNTER": str(self.counter)}

    def invoke(self, success=True, **environment):
        result = subprocess.run(["bash", str(ARCHIVER), str(self.release), str(self.output)],
                                env={**self.env, **environment}, capture_output=True, text=True)
        self.assertEqual(success, result.returncode == 0, result.stdout + result.stderr)
        return result

    def calls(self):
        return len(self.counter.read_text().splitlines()) if self.counter.exists() else 0

    def archive_digest(self):
        return hashlib.sha256(self.output.read_bytes()).hexdigest()

    def assert_no_staging_files(self):
        self.assertFalse(list(self.output.parent.glob(".*.tmp")))
        self.assertFalse(self.output.with_name(self.output.name + ".archive-lock").exists())

    def test_unchanged_mtime_and_content_identical_assembly_recopy_reuse(self):
        self.invoke()
        fingerprint = self.archive_digest()
        stamp = self.output.stat().st_mtime_ns
        os.utime(self.payload, ns=(1_000_000_000, 1_000_000_000))
        os.utime(self.release, ns=(2_000_000_000, 2_000_000_000))
        self.assertIn("Archive unchanged", self.invoke().stdout)
        backup = self.root / "recopy"
        shutil.copytree(self.release, backup)
        shutil.rmtree(self.release)
        backup.rename(self.release)
        self.assertIn("Archive unchanged", self.invoke().stdout)
        self.assertEqual(1, self.calls())
        self.assertEqual(fingerprint, self.archive_digest())
        self.assertEqual(stamp, self.output.stat().st_mtime_ns)

    def test_add_change_delete_rename_and_permissions_invalidate(self):
        self.invoke()
        added = self.release / "extra.txt"
        mutations = [lambda: added.write_bytes(b"extra"),
                     lambda: self.payload.write_bytes(b"revised"),
                     lambda: self.payload.chmod(0o755),
                     lambda: added.rename(self.release / "renamed.txt"),
                     lambda: (self.release / "renamed.txt").unlink()]
        for expected, mutate in enumerate(mutations, 2):
            mutate()
            self.invoke()
            self.assertEqual(expected, self.calls())
        with tarfile.open(self.output) as package:
            self.assertEqual(b"revised", package.extractfile("hubble/application.jar").read())
            self.assertEqual(0o755, package.getmember("hubble/application.jar").mode)
            self.assertNotIn("hubble/extra.txt", package.getnames())
            self.assertNotIn("hubble/renamed.txt", package.getnames())

    def test_missing_corrupt_output_and_invalid_manifest_rebuild(self):
        self.invoke()
        self.output.unlink()
        self.invoke()
        corrupt = bytearray(self.output.read_bytes())
        corrupt[len(corrupt) // 2] ^= 1
        self.output.write_bytes(corrupt)
        self.invoke()
        for document in ("not json", "[]", "null", json.dumps({"schema": 0})):
            self.manifest.write_text(document)
            self.invoke()
        self.assertEqual(7, self.calls())
        self.assert_no_staging_files()

    def test_failure_does_not_replace_old_archive_or_certify_it(self):
        self.invoke()
        original = self.output.read_bytes()
        self.payload.write_bytes(b"changed")
        self.invoke(success=False, ARCHIVE_TEST_FAIL="1")
        self.assertEqual(original, self.output.read_bytes())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()
        self.invoke()
        self.assertEqual(3, self.calls())
        self.assertNotEqual(original, self.output.read_bytes())

    def assert_publication_failure_preserves_original(self, phase):
        self.invoke()
        original = self.output.read_bytes()
        self.payload.write_bytes(b"changed source requiring new archive")
        original_create = ARCHIVE_MODULE.tempfile.mkstemp
        original_fdopen = ARCHIVE_MODULE.os.fdopen
        original_replace = pathlib.Path.replace

        def create(*args, **kwargs):
            if phase == "manifest_create" and kwargs.get("prefix") == "." + self.manifest.name + ".":
                raise OSError("Injected manifest creation failure")
            return original_create(*args, **kwargs)

        class FailedWriter:
            def __init__(self, stream):
                self.stream = stream

            def __enter__(self):
                self.stream.__enter__()
                return self

            def write(self, _value):
                raise OSError("Injected manifest write failure")

            def __exit__(self, *args):
                return self.stream.__exit__(*args)

        def fdopen(descriptor, mode, *args, **kwargs):
            stream = original_fdopen(descriptor, mode, *args, **kwargs)
            return FailedWriter(stream) if phase == "manifest_write" and mode == "w" else stream

        def replace(source, destination):
            if phase == "manifest_replace" and destination == self.manifest:
                raise OSError("Injected manifest replace failure")
            if phase == "archive_replace" and destination == self.output:
                # The new certificate was already atomically published. It must
                # be removed when the archive publication itself fails.
                self.assertTrue(self.manifest.is_file())
                raise OSError("Injected archive replace failure")
            return original_replace(source, destination)

        with mock.patch.dict(os.environ, self.env), \
                mock.patch.object(ARCHIVE_MODULE.tempfile, "mkstemp", side_effect=create), \
                mock.patch.object(ARCHIVE_MODULE.os, "fdopen", side_effect=fdopen), \
                mock.patch.object(pathlib.Path, "replace", new=replace):
            with self.assertRaisesRegex(OSError, "Injected"):
                ARCHIVE_MODULE.archive(self.release, self.output)
        self.assertEqual(original, self.output.read_bytes())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()
        # A later successful invocation must rebuild rather than authenticate
        # the preserved old archive for the changed source.
        self.invoke()
        self.assertEqual(3, self.calls())
        self.assertNotEqual(original, self.output.read_bytes())

    def test_manifest_creation_failure_preserves_original_archive(self):
        self.assert_publication_failure_preserves_original("manifest_create")

    def test_manifest_write_failure_preserves_original_archive(self):
        self.assert_publication_failure_preserves_original("manifest_write")

    def test_manifest_replace_failure_preserves_original_archive(self):
        self.assert_publication_failure_preserves_original("manifest_replace")

    def test_archive_replace_failure_preserves_original_archive(self):
        self.assert_publication_failure_preserves_original("archive_replace")

    def test_source_mutation_during_generation_is_not_published(self):
        self.invoke()
        original = self.output.read_bytes()
        self.payload.write_bytes(b"before")
        result = self.invoke(success=False, ARCHIVE_TEST_MUTATE=str(self.payload))
        self.assertIn("Release changed during archive generation", result.stderr)
        self.assertEqual(original, self.output.read_bytes())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()

    def test_source_change_and_restore_during_tar_cannot_publish_mixed_archive(self):
        self.invoke()
        original = self.output.read_bytes()
        self.payload.write_bytes(b"changed input requiring tar generation")
        contents = self.payload.read_bytes()
        timestamp = self.payload.stat().st_mtime_ns
        with mock.patch.dict(os.environ, self.env):
            identity, _ = ARCHIVE_MODULE.input_identity(self.release)
        result = self.invoke(success=False, ARCHIVE_TEST_RESTORE=str(self.payload),
                             ARCHIVE_TEST_BACKUP=str(self.root / "source-backup"))
        self.assertIn("Release changed during archive generation", result.stderr)
        with mock.patch.dict(os.environ, self.env):
            restored_identity, _ = ARCHIVE_MODULE.input_identity(self.release)
        # Contents, mode, and mtime are restored: persistent content identity
        # alone would miss the temporary change that tar actually archived.
        self.assertEqual(identity, restored_identity)
        self.assertEqual(contents, self.payload.read_bytes())
        self.assertEqual(timestamp, self.payload.stat().st_mtime_ns)
        self.assertEqual(original, self.output.read_bytes())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()

    def assert_cache_verification_detects_restored_change(self, directory=False):
        self.invoke()
        original = self.output.read_bytes()
        original_digest = ARCHIVE_MODULE.digest
        target = self.release if directory else self.payload
        before = target.stat()

        def digest(path, expected=None):
            if path == self.output:
                if directory:
                    target.chmod(before.st_mode ^ 0o100)
                    target.chmod(before.st_mode)
                else:
                    contents = target.read_bytes()
                    target.write_bytes(b"temporarily changed")
                    target.write_bytes(contents)
                os.utime(target, ns=(before.st_atime_ns, before.st_mtime_ns))
            return original_digest(path, expected)

        with mock.patch.dict(os.environ, self.env):
            identity, _ = ARCHIVE_MODULE.input_identity(self.release)
            with mock.patch.object(ARCHIVE_MODULE, "digest", side_effect=digest):
                with self.assertRaisesRegex(RuntimeError, "Release changed while verifying"):
                    ARCHIVE_MODULE.archive(self.release, self.output)
            restored_identity, _ = ARCHIVE_MODULE.input_identity(self.release)
        self.assertEqual(identity, restored_identity)
        self.assertEqual(original, self.output.read_bytes())
        self.assertFalse(self.manifest.exists())
        self.assertEqual(1, self.calls())
        self.assert_no_staging_files()

    def test_source_change_and_restore_during_cache_verification_is_rejected(self):
        self.assert_cache_verification_detects_restored_change()

    def test_directory_change_and_restore_during_cache_verification_is_rejected(self):
        self.assert_cache_verification_detects_restored_change(directory=True)

    def test_concurrent_invocation_cannot_publish_or_remove_winners_result(self):
        marker = self.root / "blocked"
        process = subprocess.Popen(["bash", str(ARCHIVER), str(self.release), str(self.output)],
                                   env={**self.env, "ARCHIVE_TEST_BLOCK": str(marker)},
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        try:
            deadline = time.monotonic() + 5
            while not marker.with_suffix(".ready").exists() and process.poll() is None and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertTrue(marker.with_suffix(".ready").exists())
            result = self.invoke(success=False)
            self.assertIn("Archive generation is locked", result.stderr)
            marker.with_suffix(".release").touch()
            stdout, stderr = process.communicate(timeout=5)
            self.assertEqual(0, process.returncode, stdout + stderr)
            certificate = self.manifest.read_bytes()
            fingerprint = self.archive_digest()
            self.invoke()
            self.assertEqual(certificate, self.manifest.read_bytes())
            self.assertEqual(fingerprint, self.archive_digest())
            self.assertEqual(1, self.calls())
        finally:
            if process.poll() is None:
                process.kill()
                process.communicate()
        self.assert_no_staging_files()

    def test_symlink_hardlink_and_special_sources_fail_closed(self):
        self.invoke()
        original = self.output.read_bytes()
        outside = self.root / "outside"
        outside.write_bytes(b"private")
        link = self.release / "alias"
        link.symlink_to(outside)
        self.invoke(success=False)
        self.assertFalse(self.manifest.exists())
        link.unlink()
        os.link(self.payload, link)
        self.invoke(success=False)
        self.assertFalse(self.manifest.exists())
        link.unlink()
        os.mkfifo(link)
        self.invoke(success=False)
        self.assertEqual(original, self.output.read_bytes())
        self.assert_no_staging_files()

    def test_missing_source_invalidates_previous_certificate(self):
        self.invoke()
        shutil.rmtree(self.release)
        self.invoke(success=False)
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()

    def test_excluded_private_runtime_and_appledouble_changes_reuse(self):
        self.invoke()
        runtime = self.release / "data"
        runtime.mkdir()
        private = runtime / "private.db"
        private.write_bytes(b"private")
        (runtime / "outside-link").symlink_to(self.root)
        (self.release / "._application.jar").write_bytes(b"AppleDouble")
        (self.release / "process.pid").write_bytes(b"1234")
        self.invoke()
        self.assertEqual(1, self.calls())
        self.assertEqual(b"private", private.read_bytes())

    def test_custom_native_tar_options_do_not_cache_external_inputs(self):
        # --exclude-from and other native options can depend on arbitrary files.
        # The helper preserves native behavior while conservatively rebuilding.
        self.invoke(TAR_OPTIONS="--exclude=never-present")
        self.invoke(TAR_OPTIONS="--exclude=never-present")
        self.assertEqual(2, self.calls())

    def test_archive_and_helper_tool_identity_changes_invalidate(self):
        self.invoke()
        self.tar_wrapper.write_text(self.tar_wrapper.read_text() + "# tool identity changed\n")
        self.invoke()
        self.assertEqual(2, self.calls())
        helper_dir = self.root / "helper"
        helper_dir.mkdir()
        wrapper = helper_dir / ARCHIVER.name
        shutil.copy2(ARCHIVER, wrapper)
        helper = helper_dir / "archive_hubble.py"
        shutil.copy2(ARCHIVER.with_name(helper.name), helper)
        helper.write_text(helper.read_text() + "\n# Helper identity changed.\n")
        result = subprocess.run(["bash", str(wrapper), str(self.release), str(self.output)],
                                env=self.env, capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(3, self.calls())

    def prepare_special_output(self, kind):
        if kind == "socket":
            # Keep the UNIX-domain path below the macOS sun_path limit.
            self.output = self.root / "socket"
            self.manifest = self.output.with_name(self.output.name + ".cache.json")
            server = socket.socket(socket.AF_UNIX)
            self.addCleanup(server.close)
            return lambda: server.bind(str(self.output))
        return lambda: os.mkfifo(self.output)

    def assert_existing_special_output_is_preserved(self, kind):
        create = self.prepare_special_output(kind)
        self.output.parent.mkdir(parents=True, exist_ok=True)
        create()
        before = self.output.lstat()
        result = self.invoke(success=False)
        self.assertIn("Existing archive output must be a regular file", result.stderr)
        after = self.output.lstat()
        self.assertEqual((before.st_dev, before.st_ino, before.st_mode),
                         (after.st_dev, after.st_ino, after.st_mode))
        self.assertTrue(stat.S_ISFIFO(after.st_mode) if kind == "fifo" else stat.S_ISSOCK(after.st_mode))
        self.assertEqual(0, self.calls())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()

    def test_existing_fifo_output_is_preserved(self):
        self.assert_existing_special_output_is_preserved("fifo")

    def test_existing_socket_output_is_preserved(self):
        self.assert_existing_special_output_is_preserved("socket")

    def assert_special_output_inserted_during_generation_is_preserved(self, kind):
        create = self.prepare_special_output(kind)
        self.invoke()
        self.payload.write_bytes(b"changed input requiring archive generation")
        original_run = subprocess.run
        inserted = None

        def run(command, *args, **kwargs):
            nonlocal inserted
            result = original_run(command, *args, **kwargs)
            if command[0] == "tar":
                self.output.unlink()
                create()
                inserted = self.output.lstat()
            return result

        with mock.patch.dict(os.environ, self.env), \
                mock.patch.object(ARCHIVE_MODULE.subprocess, "run", side_effect=run):
            with self.assertRaisesRegex(RuntimeError, "Existing archive output must be a regular file"):
                ARCHIVE_MODULE.archive(self.release, self.output)
        self.assertIsNotNone(inserted)
        after = self.output.lstat()
        self.assertEqual((inserted.st_dev, inserted.st_ino, inserted.st_mode),
                         (after.st_dev, after.st_ino, after.st_mode))
        self.assertTrue(stat.S_ISFIFO(after.st_mode) if kind == "fifo" else stat.S_ISSOCK(after.st_mode))
        self.assertEqual(2, self.calls())
        self.assertFalse(self.manifest.exists())
        self.assert_no_staging_files()

    def test_fifo_output_inserted_during_generation_is_preserved(self):
        self.assert_special_output_inserted_during_generation_is_preserved("fifo")

    def test_socket_output_inserted_during_generation_is_preserved(self):
        self.assert_special_output_inserted_during_generation_is_preserved("socket")

    def test_output_permissions_are_preserved_and_unsafe_targets_rejected(self):
        self.invoke()
        self.assertEqual(0o666 & ~self.current_umask(), self.output.stat().st_mode & 0o777)
        self.output.chmod(0o640)
        self.payload.write_bytes(b"changed")
        self.invoke()
        self.assertEqual(0o640, self.output.stat().st_mode & 0o777)
        original_output = self.output
        self.output = self.release / "new/nested/recursive.tar.gz"
        self.invoke(success=False)
        self.assertFalse((self.release / "new").exists())
        self.output = self.root / "symlink.tar.gz"
        self.output.symlink_to(original_output)
        self.invoke(success=False)
        self.assertTrue(self.output.is_symlink())

    @staticmethod
    def current_umask():
        mask = os.umask(0)
        os.umask(mask)
        return mask

    @unittest.skipUnless(sys.platform == "darwin", "requires macOS metadata")
    def test_extended_attribute_and_acl_changes_invalidate_without_modifying_source(self):
        self.invoke()
        attribute = "com.apple.metadata:hugegraph-archive-test"
        subprocess.run(["xattr", "-w", attribute, "retained", str(self.payload)], check=True)
        self.invoke()
        subprocess.run(["chmod", "+a", "everyone allow read", str(self.payload)], check=True)
        self.invoke()
        self.assertEqual(3, self.calls())
        self.assertEqual(b"retained", subprocess.check_output(["xattr", "-p", attribute, str(self.payload)]).strip())
        self.assertIn("everyone allow read", subprocess.check_output(["ls", "-le", str(self.payload)], text=True))


if __name__ == "__main__":
    unittest.main()
