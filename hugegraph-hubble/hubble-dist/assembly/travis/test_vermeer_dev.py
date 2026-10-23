#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Isolated synthetic fixtures only; never start or inspect the retained stack."""

import contextlib
import hashlib
import importlib.util
import io
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

SCRIPT = Path(__file__).with_name('vermeer_dev.py')
SPEC = importlib.util.spec_from_file_location('vermeer_dev_test_subject', SCRIPT)
DEV = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(DEV)
FIXTURE = 'synthetic-fixture-only'


class PrivateDevelopmentFilesTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        # No inherited credential selectors may enter a configure fixture.
        environment = mock.patch.dict(os.environ, {}, clear=True)
        environment.start()
        self.addCleanup(environment.stop)
        self.setup = self.root / 'home'
        self.run = self.setup / 'integration'
        self.source = self.root / 'source'
        self.repo = self.root / 'repo'
        paths = mock.patch.multiple(DEV, SETUP=self.setup, RUN=self.run,
                                    SOURCE=self.source, REPO=self.repo,
                                    GO=self.setup / 'tools/go')
        paths.start()
        self.addCleanup(paths.stop)

    def synthetic_file(self, name='input', mode=0o600, value=FIXTURE):
        path = self.root / name
        path.write_text(value)
        path.chmod(mode)
        return path

    def prepare_configuration_fixture(self):
        (self.source / 'config').mkdir(parents=True)
        for mode in ('master', 'worker'):
            (self.source / f'config/{mode}.ini').write_text('host=0.0.0.0\n')
        template = self.repo / 'hugegraph-hubble/hubble-dist/assembly/static/conf/hugegraph-hubble.properties'
        template.parent.mkdir(parents=True)
        template.write_text('dashboard.address=127.0.0.1:8092\npd.enabled=false\nvermeer.url=\n')

    def retained_process(self, name):
        pidfile = self.run / (f'vermeer/{name}.pid' if name in ('master', 'worker') else f'{name}.pid')
        pidfile.parent.mkdir(parents=True, exist_ok=True)
        pidfile.write_text('12345')
        command = self.root / 'proc/12345/cmdline'
        command.parent.mkdir(parents=True, exist_ok=True)
        return pidfile, command

    def synthetic_process_path(self, value):
        text = str(value)
        if text == '/proc' or text.startswith('/proc/'):
            return self.root / text.lstrip('/')
        return Path(value)

    def test_retained_start_requires_the_full_component_command(self):
        _, command = self.retained_process('master')
        expected = DEV.process_args('master')
        alternatives = [DEV.process_args('worker'),
                        [expected[0], f'--env={self.root}/other-stack/master.ini']]
        with mock.patch.object(DEV, 'Path', side_effect=self.synthetic_process_path), \
                mock.patch.object(DEV.subprocess, 'Popen') as spawn:
            for arguments in alternatives:
                command.write_bytes(b'\0'.join(os.fsencode(str(arg)) for arg in arguments) + b'\0')
                with self.assertRaises(RuntimeError):
                    DEV.process('master', expected, self.run, 6688)
            command.write_bytes(b'\0'.join(os.fsencode(str(arg)) for arg in expected) + b'\0')
            DEV.process('master', expected, self.run, 6688)
            spawn.assert_not_called()

    def test_stop_preserves_other_component_or_stack_processes(self):
        pidfile, command = self.retained_process('master')
        for arguments in (DEV.process_args('worker'),
                          [self.setup / 'vermeer-bin', f'--env={self.root}/other-stack/master.ini']):
            command.write_bytes(b'\0'.join(os.fsencode(str(arg)) for arg in arguments) + b'\0')
            with mock.patch.object(DEV, 'Path', side_effect=self.synthetic_process_path), \
                    mock.patch.object(DEV.os, 'kill') as signal:
                with self.assertRaises(RuntimeError):
                    DEV.stop('vermeer')
                signal.assert_not_called()
                self.assertTrue(pidfile.exists())

    def test_stop_signals_only_the_expected_hubble_command(self):
        pidfile, command = self.retained_process('hubble')
        (self.run / 'hubble-classpath').write_text('synthetic-classpath')
        arguments = DEV.process_args('hubble')
        command.write_bytes(b'\0'.join(os.fsencode(str(arg)) for arg in arguments) + b'\0')
        with mock.patch.object(DEV, 'Path', side_effect=self.synthetic_process_path), \
                mock.patch.object(DEV.os, 'kill', side_effect=lambda *_: command.write_bytes(b'')) as signal:
            DEV.stop('hubble')
            signal.assert_called_once_with(12345, DEV.signal.SIGTERM)
        self.assertFalse(pidfile.exists())

    def test_new_file_and_parent_are_private_with_permissive_umask(self):
        path = self.root / 'new-parent/output'
        previous = os.umask(0o022)
        try:
            DEV.private_file(path, FIXTURE)
        finally:
            os.umask(previous)
        self.assertEqual(0o700, stat.S_IMODE(path.parent.stat().st_mode))
        self.assertEqual(0o600, stat.S_IMODE(path.stat().st_mode))
        self.assertTrue(path.read_text() == FIXTURE)

    def test_existing_file_is_private_before_truncation_and_write(self):
        path = self.synthetic_file(mode=0o644)
        truncate = os.ftruncate
        fdopen = os.fdopen
        modes = []

        def checked_truncate(fd, length):
            modes.append(stat.S_IMODE(os.fstat(fd).st_mode))
            return truncate(fd, length)

        @contextlib.contextmanager
        def checked_writer(fd, *args, **kwargs):
            with fdopen(fd, *args, **kwargs) as output:
                class Writer:
                    def write(self, value):
                        modes.append(stat.S_IMODE(os.fstat(fd).st_mode))
                        return output.write(value)
                yield Writer()

        with mock.patch.object(DEV.os, 'ftruncate', side_effect=checked_truncate), \
                mock.patch.object(DEV.os, 'fdopen', side_effect=checked_writer):
            DEV.private_file(path, FIXTURE)
        self.assertEqual([0o600, 0o600], modes)

    def test_exclusive_creation_never_overwrites_an_existing_file(self):
        path = self.synthetic_file()
        before = path.read_bytes()
        with self.assertRaises(FileExistsError):
            DEV.private_file(path, '', exclusive=True)
        self.assertTrue(path.read_bytes() == before)

    def test_symlink_output_does_not_change_its_target(self):
        target = self.synthetic_file(mode=0o644)
        link = self.root / 'link'
        link.symlink_to(target)
        with self.assertRaises(OSError):
            DEV.private_file(link, '')
        self.assertTrue(target.read_text() == FIXTURE)
        self.assertEqual(0o644, stat.S_IMODE(target.stat().st_mode))

    def test_hard_link_output_is_rejected_before_chmod_or_write(self):
        target = self.synthetic_file(mode=0o644)
        link = self.root / 'hard-link'
        os.link(target, link)
        with self.assertRaises(RuntimeError):
            DEV.private_file(link, '')
        self.assertTrue(target.read_text() == FIXTURE)
        self.assertEqual(0o644, stat.S_IMODE(target.stat().st_mode))

    def test_symlink_parent_is_rejected(self):
        target = self.root / 'directory'
        target.mkdir()
        link = self.root / 'directory-link'
        link.symlink_to(target, target_is_directory=True)
        with self.assertRaises(OSError):
            DEV.private_file(link / 'output', '')
        self.assertFalse(any(target.iterdir()))

    def test_managed_directory_is_private_and_shared_setup_is_not_repermissioned(self):
        directory = self.root / 'directory'
        directory.mkdir(mode=0o755)
        directory.chmod(0o755)
        DEV.owned_directory(directory)
        self.assertEqual(0o755, stat.S_IMODE(directory.stat().st_mode))
        DEV.owned_directory(directory, private=True)
        self.assertEqual(0o700, stat.S_IMODE(directory.stat().st_mode))
        directory.chmod(0o777)
        with self.assertRaises(RuntimeError):
            DEV.private_file(directory / 'output', '')
        self.assertFalse((directory / 'output').exists())

    def test_nonregular_files_are_rejected_without_blocking(self):
        fifo = self.root / 'fifo'
        os.mkfifo(fifo, 0o600)
        code = '''
import importlib.util
from pathlib import Path
import sys
spec = importlib.util.spec_from_file_location('subject', sys.argv[1])
subject = importlib.util.module_from_spec(spec)
spec.loader.exec_module(subject)
for operation in (lambda: subject.private_file(Path(sys.argv[2]), ''),
                  lambda: subject.read_private_secret(Path(sys.argv[2]))):
    try:
        operation()
    except (OSError, RuntimeError):
        continue
    raise SystemExit(2)
'''
        result = subprocess.run([sys.executable, '-c', code, str(SCRIPT), str(fifo)],
                                capture_output=True, timeout=3)
        self.assertEqual(0, result.returncode)
        self.assertEqual(0, len(result.stdout) + len(result.stderr))
        directory = self.root / 'directory'
        directory.mkdir()
        with self.assertRaises(OSError):
            DEV.private_file(directory, '')
        with self.assertRaises(RuntimeError):
            DEV.read_private_secret(self.root)

    def test_external_file_permissions_are_checked_before_read(self):
        path = self.synthetic_file(mode=0o644)
        with mock.patch.object(DEV.os, 'fdopen', side_effect=AssertionError('Unsafe input read')):
            with self.assertRaises(RuntimeError):
                DEV.read_private_secret(path)
        self.assertEqual(0o644, stat.S_IMODE(path.stat().st_mode))

    def test_external_private_read_preserves_permissions(self):
        for mode in (0o400, 0o600):
            with self.subTest(mode=mode):
                path = self.synthetic_file(name=f'input-{mode:o}', mode=mode)
                self.assertTrue(DEV.read_private_secret(path) == FIXTURE)
                self.assertEqual(mode, stat.S_IMODE(path.stat().st_mode))

    def test_external_symlink_and_missing_input_are_rejected(self):
        target = self.synthetic_file()
        link = self.root / 'link'
        link.symlink_to(target)
        with self.assertRaises(OSError):
            DEV.read_private_secret(link)
        missing = self.root / 'missing'
        with self.assertRaises(FileNotFoundError):
            DEV.read_private_secret(missing)
        self.assertFalse(missing.exists())

    def test_invalid_external_values_have_sanitized_errors(self):
        for index, value in enumerate((b'', b'first\nsecond', b'first\0second', b'\xff')):
            with self.subTest(case=index):
                path = self.root / 'invalid'
                path.write_bytes(value)
                path.chmod(0o600)
                with self.assertRaises(RuntimeError) as raised:
                    DEV.read_private_secret(path)
                self.assertFalse(FIXTURE in str(raised.exception))

    def test_descriptors_close_after_validation_failure(self):
        path = self.synthetic_file(mode=0o644)
        close = os.close
        closed = []

        def checked_close(fd):
            close(fd)
            closed.append(fd)

        with mock.patch.object(DEV.os, 'close', side_effect=checked_close):
            with self.assertRaises(RuntimeError):
                DEV.read_private_secret(path)
        self.assertEqual(1, len(closed))
        with self.assertRaises(OSError):
            os.fstat(closed[0])

    def test_configure_reuses_default_and_stop_retains_credentials(self):
        self.prepare_configuration_fixture()
        self.setup.mkdir(mode=0o755)
        self.setup.chmod(0o755)
        cache = self.setup / 'tools/cache'
        cache.mkdir(parents=True)
        cache.chmod(0o755)
        output = io.StringIO()
        with mock.patch.object(DEV.secrets, 'token_hex', return_value=FIXTURE) as generate, \
                contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            DEV.configure()
            files = [self.run / name for name in ('pd-password', 'pd.env', 'server.env')]
            fingerprints = [hashlib.sha256(path.read_bytes()).digest() for path in files]
            DEV.configure()
            DEV.stop('all')  # The isolated fixture has no PID files or live processes.
        self.assertEqual(1, generate.call_count)
        self.assertEqual(0, len(output.getvalue()))
        self.assertTrue(fingerprints == [hashlib.sha256(path.read_bytes()).digest() for path in files])
        for path in files:
            self.assertEqual(0o600, stat.S_IMODE(path.stat().st_mode))
        for path in (self.run, self.run / 'vermeer', self.run / 'hubble-home',
                     self.run / 'hubble-home/conf', self.run / 'hubble-home/conf/graphs'):
            self.assertEqual(0o700, stat.S_IMODE(path.stat().st_mode))
        self.assertEqual(0o755, stat.S_IMODE(self.setup.stat().st_mode))
        self.assertEqual(0o755, stat.S_IMODE(cache.stat().st_mode))
        for mode in ('master', 'worker'):
            config = (self.run / f'vermeer/{mode}.ini').read_text()
            self.assertIn('pd_peers=["127.0.0.1:8686"]\n', config)

    def test_configure_uses_external_file_without_changing_or_creating_it(self):
        self.prepare_configuration_fixture()
        external = self.synthetic_file(mode=0o400)
        os.environ['VERMEER_PD_PASSWORD_FILE'] = str(external)
        with mock.patch.object(DEV.secrets, 'token_hex') as generate:
            DEV.configure()
        self.assertEqual(0, generate.call_count)
        self.assertEqual(0o400, stat.S_IMODE(external.stat().st_mode))
        self.assertFalse((self.run / 'pd-password').exists())
        external.unlink()
        with self.assertRaises(FileNotFoundError):
            DEV.configure()
        self.assertFalse(external.exists())

    def test_custom_repository_homes_ignore_generated_credentials_only(self):
        git = self.root / 'git-fixture'
        git.mkdir()
        (git / '.gitignore').write_text((SCRIPT.resolve().parents[4] / '.gitignore').read_text())
        env = dict(os.environ, GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL='/dev/null')
        subprocess.run(['git', 'init', '-q', str(git)], env=env, check=True, capture_output=True)
        for path in ('.vermeer-dev/anything', 'custom/home/integration/pd-password',
                     'custom/home/integration/pd.env', 'custom/home/integration/server.env'):
            result = subprocess.run(['git', 'check-ignore', '--no-index', path], cwd=git,
                                    env=env, capture_output=True)
            self.assertEqual(0, result.returncode)
        result = subprocess.run(['git', 'check-ignore', '--no-index',
                                 'hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py'],
                                cwd=git, env=env, capture_output=True)
        self.assertEqual(1, result.returncode)


if __name__ == '__main__':
    unittest.main()
