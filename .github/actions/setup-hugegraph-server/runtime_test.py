#!/bin/bash
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
"""Exercise fixture identity, bounded downloads and service readiness offline."""
import hashlib
import json
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import shutil
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SHA = 'b12425c2032bf0d21a97b8221f42a18055c2982f'

class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.dir = Path(self.temp.name)
        # Offline archives use a test-local checksum pin, never a production bypass.
        self.action = self.dir / 'action'
        self.action.mkdir()
        for name in ('fixture.sh', 'manifest.py', 'release.py', 'service-wait.sh'):
            shutil.copyfile(HERE / name, self.action / name)
        spec = importlib.util.spec_from_file_location('fixture_release', HERE / 'release.py')
        self.release = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.release)
        checksum = hashlib.sha512(b'archive').hexdigest()
        release_code = (self.action / 'release.py').read_text()
        release_code = release_code.replace(self.release.SHA512[:64], checksum[:64])
        release_code = release_code.replace(self.release.SHA512[64:], checksum[64:])
        (self.action / 'release.py').write_text(release_code)
        self.bin = self.dir / 'bin'
        self.bin.mkdir()
        (self.bin/'java').write_text("#!/bin/bash\necho 'openjdk version \"11.0.1\"' >&2\n")
        (self.bin/'java').chmod(0o755)
        self.env = dict(os.environ, HOME=str(self.dir), PATH=f'{self.bin}:{os.environ["PATH"]}',
                        FIXTURE_DIR=str(self.dir / 'fixture'), FIXTURE_REPOSITORY='apache/hugegraph',
                        FIXTURE_COMMIT=SHA, FIXTURE_JAVA='11',
                        FIXTURE_BUILD_INPUTS='test-build-contract', FIXTURE_PLATFORM='Linux-X64',
                        FIXTURE_RELEASE_VERSION='1.7.0',
                        FIXTURE_CONFIG='asf-release-1.7.0;schema=2;inputs=test-build-contract;platform=Linux-X64')
        Path(self.env['FIXTURE_DIR']).mkdir()
    def command(self, name, body):
        script = self.bin / name
        script.write_text('#!/bin/bash\n' + body)
        script.chmod(0o755)
    def run_script(self, script, *args, env=None):
        return subprocess.run(['bash', str(script), *args], env=env or self.env,
                              cwd=self.dir, text=True, capture_output=True, timeout=20)
    def manifest(self, **extra):
        root = Path(self.env['FIXTURE_DIR'])
        data = {k.lower(): self.env['FIXTURE_'+k] for k in ('REPOSITORY','COMMIT','JAVA','CONFIG')}
        (root/'server.tar.gz').write_bytes(b'archive')
        if extra.get('java', self.env['FIXTURE_JAVA']) == '11':
            data.update(self.release.identity('1.7.0'))
            data['official_sha512'] = hashlib.sha512(b'archive').hexdigest()
            name = self.release.ARCHIVE_NAME
        else:
            data.update(source_kind='candidate-sdk')
            name = 'apache-hugegraph-fixture.tar.gz'
        data.update(archive_name=name, sha256=hashlib.sha256(b'archive').hexdigest())
        data.update(extra)
        (root/'manifest.json').write_text(json.dumps(data))
    def test_identity_checksum_and_regular_files(self):
        self.manifest()
        env = dict(self.env, FIXTURE_MODE='start')
        self.assertEqual(self.run_script(self.action/'fixture.sh', env=env).returncode, 0)
        for field, wrong in [('repository','fork/hugegraph'), ('commit','b'*40), ('java','17'),
                              ('config','different'), ('source_url','https://example.invalid/release'),
                              ('release_version','1.8.0'), ('source_repository','fork/hugegraph'),
                              ('source_commit','b'*40), ('official_sha512','bad'), ('sha256','bad')]:
            self.manifest(**{field:wrong})
            self.assertNotEqual(self.run_script(self.action/'fixture.sh', env=env).returncode, 0, field)
        self.manifest()
        self.command('java', "echo 'openjdk version \"17.0.1\"' >&2\n")
        self.assertNotEqual(self.run_script(self.action/'fixture.sh', env=env).returncode, 0)
        self.command('java', "echo 'openjdk version \"11.0.1\"' >&2\n")
        self.manifest(archive_name='../escape.tar.gz')
        self.assertNotEqual(self.run_script(self.action/'fixture.sh', env=env).returncode, 0)
        self.manifest()
        archive = Path(self.env['FIXTURE_DIR'])/'server.tar.gz'
        archive.unlink()
        archive.symlink_to(self.dir/'missing')
        self.assertNotEqual(self.run_script(self.action/'fixture.sh', env=env).returncode, 0)
    def test_composite_step_isolates_fixture_jdk_from_caller(self):
        action = (HERE / 'action.yml').read_text()
        block = action.split('      run: |\n', 1)[1].split('    - name:', 1)[0]
        step = self.dir / 'action-step.sh'
        step.write_text('\n'.join(line[8:] for line in block.splitlines()) + '\n')
        jdk11 = self.dir / 'jdk11'
        jdk17 = self.dir / 'jdk17'
        for version, root in ((11, jdk11), (17, jdk17)):
            (root / 'bin').mkdir(parents=True)
            for name, body in [('java', f'echo \'openjdk version "{version}.0.1"\' >&2'), ('javac', 'exit 0')]:
                binary = root / 'bin' / name
                binary.write_text('#!/bin/bash\n' + body + '\n')
                binary.chmod(0o755)
        travis = self.dir / 'service'
        travis.mkdir()
        installer = travis / 'install-hugegraph-from-source.sh'
        installer.write_text('#!/bin/bash\nset -e\nsource "$FIXTURE_ACTION/fixture.sh"\n'
                             'java -version 2> "$HOME/started-java"\n')
        for fixture_version, fixture_jdk, caller_version, caller_jdk in (
                (11, jdk11, 17, jdk17), (17, jdk17, 11, jdk11)):
            with self.subTest(fixture=fixture_version, caller=caller_version):
                config = self.env['FIXTURE_CONFIG']
                if fixture_version == 17:
                    config = 'candidate-sdk-install;' + config.split(';', 1)[1]
                self.manifest(java=str(fixture_version), config=config)
                env = dict(self.env, JAVA_HOME=str(caller_jdk),
                           PATH=f'{caller_jdk}/bin:{self.env["PATH"]}',
                           FIXTURE_JAVA=str(fixture_version), FIXTURE_JAVA_HOME=str(fixture_jdk),
                           FIXTURE_MODE='start', FIXTURE_ARTIFACT='shared', FIXTURE_ACTION=str(self.action),
                           ISOLATE_FIXTURE_REPO='false', TRAVIS_DIR=str(travis), RUNNER_TEMP=str(self.dir))
                result = subprocess.run(
                    ['bash', '-c', 'bash "$HOME/action-step.sh" && java -version 2> "$HOME/caller-java"'],
                    cwd=self.dir, env=env, text=True, capture_output=True, timeout=20)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertIn(f'{fixture_version}.0.1', (self.dir / 'started-java').read_text())
                self.assertIn(f'{caller_version}.0.1', (self.dir / 'caller-java').read_text())
                # A fixture must reject accidentally inheriting the caller's different JDK.
                env['FIXTURE_JAVA_HOME'] = str(caller_jdk)
                result = subprocess.run(['bash', str(step)], cwd=self.dir, env=env,
                                        text=True, capture_output=True, timeout=20)
                self.assertNotEqual(0, result.returncode)
                self.assertIn('Fixture JDK mismatch', result.stderr)

    def test_legacy_bash_service_deadline_and_success(self):
        helper = str(HERE/'service-wait.sh')
        self.command('curl', 'exit 1\n')
        result = subprocess.run(
            ['bash','-c',f'source "{helper}"; fixture_wait http://localhost:1'],
            env=dict(self.env, SERVICE_READY_TIMEOUT='0'), text=True, capture_output=True, timeout=2)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('timed out', result.stderr)
        self.command('curl', 'printf "%s\\n" "$@" > "$HOME/curl-args"; exit 0\n')
        result = subprocess.run(['bash','-c',f'source "{helper}"; fixture_wait http://localhost:1'],
                                env=self.env, capture_output=True, timeout=2)
        self.assertEqual(result.returncode, 0)
        args = (self.dir/'curl-args').read_text()
        self.assertIn('--max-time\n5', args)
        self.assertIn('admin:pa', args)
    def test_hadoop_invalid_cache_retries_at_most_twice_and_stops(self):
        (self.dir/'hadoop-3.3.6.tar.gz').write_text('tampered')
        self.command('curl', 'printf "%s\\n" "$@" >> "$HOME/download-args"; exit 22\n')
        self.command('sudo', 'echo unexpected-install >> "$HOME/installed"; exit 99\n')
        result = self.run_script(ROOT/'hugegraph-loader/assembly/travis/install-hadoop.sh')
        self.assertNotEqual(result.returncode, 0)
        args = (self.dir/'download-args').read_text()
        self.assertEqual(args.count('--max-time\n900'), 2)
        self.assertFalse((self.dir/'installed').exists())
        self.assertFalse((self.dir/'hadoop-3.3.6.tar.gz').exists())
    def test_hdfs_container_exit_fails_before_tests(self):
        self.command('timeout', 'shift; exec "$@"\n')
        self.command('docker', '''
case "$1" in
  inspect) echo false ;;
  logs) echo 'NameNode startup failed' >&2 ;;
esac
''')
        result = self.run_script(ROOT/'hugegraph-loader/assembly/travis/start-hdfs-ci.sh')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('NameNode startup failed', result.stderr)
        self.assertIn('exited before readiness', result.stderr)
    def test_hdfs_live_datanode_with_failed_write_is_not_ready(self):
        self.command('timeout', 'shift; exec "$@"\n')
        self.command('curl', '''echo '{"beans":[{"NumLiveDataNodes":1}]}'\n''')
        self.command('docker', '''
case "$1" in
  inspect) echo true ;;
  exec) if [[ "$*" == *"-put"* ]]; then echo 'Block write failed' >&2; exit 1; fi ;;
esac
''')
        result = self.run_script(ROOT/'hugegraph-loader/assembly/travis/start-hdfs-ci.sh')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Block write failed', result.stderr)
    def test_real_http_readiness_requires_successful_auth_response(self):
        from http.server import BaseHTTPRequestHandler, HTTPServer
        from threading import Thread
        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                if self.path == '/graphs' and self.headers.get('Authorization') == 'Basic YWRtaW46cGE=':
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b'{"graphs":[]}')
                else:
                    self.send_response(401)
                    self.end_headers()
            def log_message(self, *args):
                pass
        server = HTTPServer(('127.0.0.1', 0), Handler)
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        Thread(target=server.serve_forever, daemon=True).start()
        helper = str(HERE/'service-wait.sh')
        result = subprocess.run(['bash','-c',f'source "{helper}"; fixture_wait http://127.0.0.1:{server.server_port}'],
                                env=self.env, capture_output=True, timeout=5)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_exact_cache_hit_never_builds_or_starts(self):
        self.manifest()
        identity = '\n'.join(self.env['FIXTURE_'+k] for k in ('REPOSITORY','COMMIT','JAVA','CONFIG'))+'\n'
        cache = self.dir/'hugegraph-fixture-cache'/hashlib.sha256(identity.encode()).hexdigest()
        cache.mkdir(parents=True)
        for name in ('server.tar.gz','manifest.json'):
            (cache/name).write_bytes((Path(self.env['FIXTURE_DIR'])/name).read_bytes())
            (Path(self.env['FIXTURE_DIR'])/name).unlink()
        self.command('git', 'echo forbidden-git >> "$HOME/forbidden"; exit 99\n')
        self.command('mvn', 'echo forbidden-build >> "$HOME/forbidden"; exit 99\n')
        self.command('curl', 'echo download >> "$HOME/downloads"; exit 22\n')
        result = self.run_script(self.action/'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.dir/'forbidden').exists())
        self.assertFalse((self.dir/self.release.ARCHIVE_NAME).exists())
        # An incomplete release cache must download again, never rebuild old source.
        (cache/'manifest.json').unlink()
        result = self.run_script(self.action/'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.dir/'forbidden').exists())
        self.assertTrue((self.dir/'downloads').exists())

    def test_release_pin_matches_official_identity_and_rejects_wrong_bytes(self):
        self.assertEqual('1.7.0', self.release.VERSION)
        self.assertEqual('https://downloads.apache.org/hugegraph/1.7.0/' +
                         self.release.ARCHIVE_NAME, self.release.SOURCE_URL)
        self.assertEqual('apache/hugegraph', self.release.REPOSITORY)
        self.assertEqual(SHA, self.release.COMMIT)
        self.assertEqual(128, len(self.release.SHA512))
        self.assertEqual(self.release.SHA512,
                         self.release.identity('1.7.0')['official_sha512'])
        with self.assertRaises(ValueError):
            self.release.identity('1.8.0')
        archive = self.dir / 'invalid-release.tar.gz'
        archive.write_bytes(b'archive')
        with self.assertRaisesRegex(ValueError, 'SHA-512 mismatch'):
            self.release.verify(archive)

    def test_release_rejects_wrong_caller_source_before_cache_or_download(self):
        for name in ('curl', 'git', 'mvn'):
            self.command(name, 'echo forbidden >> "$HOME/forbidden"; exit 99\n')
        for field, wrong in (('FIXTURE_REPOSITORY', 'fork/hugegraph'),
                             ('FIXTURE_COMMIT', 'a' * 40)):
            with self.subTest(field=field):
                env = dict(self.env, FIXTURE_MODE='build', **{field: wrong})
                result = self.run_script(self.action / 'fixture.sh', env=env)
                self.assertNotEqual(0, result.returncode)
                self.assertIn('official Server release requires', result.stderr)
                self.assertFalse((self.dir / 'forbidden').exists())
                self.assertFalse((self.dir / 'hugegraph-fixture-cache').exists())
                self.assertFalse((Path(self.env['FIXTURE_DIR']) / 'manifest.json').exists())
                # Direct manifest callers must reject the same false source claim.
                result = subprocess.run(['python3', str(self.action / 'manifest.py'), 'create'],
                                        env=env, text=True, capture_output=True, timeout=20)
                self.assertNotEqual(0, result.returncode)
                self.assertIn('official Server release requires', result.stderr)
                self.assertFalse((Path(self.env['FIXTURE_DIR']) / 'manifest.json').exists())

    def test_release_download_keeps_official_identity_and_never_builds(self):
        self.command('curl', 'printf "%s\\n" "$@" > "$HOME/download-args"\n'
                     'while [[ $# -gt 0 ]]; do\n'
                     '  if [[ "$1" == --output ]]; then printf archive > "$2"; exit 0; fi\n'
                     '  shift\ndone\nexit 1\n')
        for name in ('git', 'mvn'):
            self.command(name, 'echo forbidden >> "$HOME/forbidden"; exit 99\n')
        result = self.run_script(self.action / 'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse((self.dir / 'forbidden').exists())
        manifest = json.loads((Path(self.env['FIXTURE_DIR']) / 'manifest.json').read_text())
        self.assertEqual('asf-release', manifest['source_kind'])
        self.assertEqual(self.release.REPOSITORY, manifest['source_repository'])
        self.assertEqual(self.release.COMMIT, manifest['source_commit'])
        self.assertEqual(self.release.SOURCE_URL, manifest['source_url'])
        self.assertEqual(hashlib.sha512(b'archive').hexdigest(), manifest['official_sha512'])
        args = (self.dir / 'download-args').read_text()
        self.assertIn('--max-time\n900', args)
        self.assertIn('--proto\n=https', args)
        self.assertIn(self.release.SOURCE_URL, args)

    def test_release_archive_fallback_keeps_canonical_identity(self):
        self.command('curl', 'printf "%s\\n" "$@" >> "$HOME/download-args"\n'
                     'if [[ ! -f "$HOME/primary-failed" ]]; then\n'
                     '  touch "$HOME/primary-failed"; exit 22\nfi\n'
                     'while [[ $# -gt 0 ]]; do\n'
                     '  if [[ "$1" == --output ]]; then printf archive > "$2"; exit 0; fi\n'
                     '  shift\ndone\nexit 1\n')
        result = self.run_script(self.action / 'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertEqual(0, result.returncode, result.stderr)
        args = (self.dir / 'download-args').read_text()
        self.assertLess(args.index(self.release.SOURCE_URL), args.index(self.release.ARCHIVE_URL))
        manifest = json.loads((Path(self.env['FIXTURE_DIR']) / 'manifest.json').read_text())
        self.assertEqual(self.release.SOURCE_URL, manifest['source_url'])
        self.assertEqual(hashlib.sha512(b'archive').hexdigest(), manifest['official_sha512'])

    def test_release_bad_checksum_does_not_try_another_source(self):
        self.command('curl', 'printf "%s\\n" "$@" >> "$HOME/download-args"\n'
                     'while [[ $# -gt 0 ]]; do\n'
                     '  if [[ "$1" == --output ]]; then printf tampered > "$2"; exit 0; fi\n'
                     '  shift\ndone\nexit 1\n')
        result = self.run_script(self.action / 'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertNotEqual(0, result.returncode)
        self.assertIn('SHA-512 mismatch', result.stderr)
        self.assertNotIn(self.release.ARCHIVE_URL, (self.dir / 'download-args').read_text())
        self.assertFalse((Path(self.env['FIXTURE_DIR']) / 'server.tar.gz').exists())

    def test_release_failure_never_falls_back_to_source_or_publishes_cache(self):
        self.command('curl', 'echo download >> "$HOME/downloads"; exit 22\n')
        for name in ('git', 'mvn'):
            self.command(name, 'echo forbidden >> "$HOME/forbidden"; exit 99\n')
        result = self.run_script(self.action / 'fixture.sh', env=dict(self.env, FIXTURE_MODE='build'))
        self.assertNotEqual(0, result.returncode)
        self.assertFalse((self.dir / 'forbidden').exists())
        self.assertFalse((self.dir / 'hugegraph-fixture-cache').exists())
        self.assertEqual([], list(Path(self.env['FIXTURE_DIR']).iterdir()))

    def test_release_rejects_tampering_even_with_updated_local_sha256(self):
        self.manifest()
        fixture = Path(self.env['FIXTURE_DIR'])
        (fixture / 'server.tar.gz').write_bytes(b'changed archive')
        manifest = json.loads((fixture / 'manifest.json').read_text())
        manifest['sha256'] = hashlib.sha256(b'changed archive').hexdigest()
        (fixture / 'manifest.json').write_text(json.dumps(manifest))
        result = self.run_script(self.action / 'fixture.sh', env=dict(self.env, FIXTURE_MODE='start'))
        self.assertNotEqual(0, result.returncode)
        self.assertIn('official Server release SHA-512 mismatch', result.stderr)


if __name__ == '__main__':
    unittest.main()
