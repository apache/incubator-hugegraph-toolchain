/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const test = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync, spawnSync } = require('node:child_process');
const { mkdtempSync, mkdirSync, rmSync, writeFileSync, readFileSync, readdirSync, existsSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { createHash } = require('node:crypto');

function cacheStamp(cache, repository, sha, archive) {
  const digest = createHash('sha256').update(readFileSync(join(cache, archive))).digest('hex');
  writeFileSync(join(cache, 'server-provenance'), `${repository}\n${sha}\n${archive}\n${digest}\n`);
}

function temp(t) {
  const root = mkdtempSync(join(tmpdir(), 'server-install-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  return root;
}

for (const installer of [
  join(__dirname, 'install-hugegraph-from-source.sh'),
  join(__dirname, '../../../hugegraph-tools/assembly/travis/install-hugegraph-from-source.sh'),
  join(__dirname, '../../../hugegraph-spark-connector/assembly/travis/install-hugegraph-from-source.sh')
]) {
test(`build-only ${installer} preserves source selection and packages without dependency installation`, t => {
  const root = temp(t);
  const source = join(root, 'source');
  const git = (...args) => execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  git('init', source);
  git('-C', source, 'config', 'user.name', 'Fixture');
  git('-C', source, 'config', 'user.email', 'fixture@example.invalid');
  writeFileSync(join(source, 'pom.xml'), 'fixture');
  git('-C', source, 'add', 'pom.xml');
  git('-C', source, 'commit', '-m', 'initial');
  const sha = git('-C', source, 'rev-parse', 'HEAD');
  const bin = join(root, 'bin');
  mkdirSync(bin);
  writeFileSync(join(bin, 'curl'), '#!/bin/bash\nexit 0\n', { mode: 0o755 });
  writeFileSync(join(bin, 'mvn'), '#!/bin/bash\nprintf "%s\\n" "$@" > "$BUILD_ARGS"\nprintf "%s" "$MAVEN_ARGS" > "$BUILD_REPO"\n', { mode: 0o755 });
  const env = { ...process.env, SERVER_REPOSITORY: 'example/server', SERVER_FETCH_REF: sha,
    PATH: `${bin}:${process.env.PATH}`, BUILD_ARGS: join(root, 'args'), BUILD_REPO: join(root, 'repo'),
    MAVEN_ARGS: `-Dmaven.repo.local=${root}/isolated-m2`, GIT_CONFIG_COUNT: '1',
    GIT_CONFIG_KEY_0: `url.file://${source}.insteadOf`, GIT_CONFIG_VALUE_0: 'https://github.com/example/server.git' };
  const run = args => spawnSync('bash', [installer, ...args],
    { cwd: root, env, encoding: 'utf8' });
  assert.notEqual(run([sha, '--unknown']).status, 0);
  assert.equal(existsSync(join(root, 'hugegraph')), false);
  const result = run([sha, '--build-only']);
  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(readFileSync(env.BUILD_ARGS, 'utf8').trim().split('\n'),
    ['package', '-DskipTests', '-Dmaven.javadoc.skip=true', '-ntp']);
  assert.equal(readFileSync(env.BUILD_REPO, 'utf8'), env.MAVEN_ARGS);
  // No archive exists: success proves build-only did not attempt extraction/startup.
  assert.equal(readdirSync(root).some(name => name.startsWith('hugegraph-servers.')), false);
});
}

function serverFixture(fixture) {
  mkdirSync(join(fixture, 'conf/graphs'), { recursive: true });
  mkdirSync(join(fixture, 'bin'));
  writeFileSync(join(fixture, 'conf/graphs/hugegraph.properties'),
    'gremlin.graph=org.apache.hugegraph.HugeFactory\n');
  writeFileSync(join(fixture, 'conf/rest-server.properties'),
    'restserver.url=http://127.0.0.1:8080\nrpc.server_port=8091\n#auth.authenticator=none\n#auth.admin_pa=none\n');
  writeFileSync(join(fixture, 'conf/gremlin-server.yaml'), '#port: 8182\n');
  writeFileSync(join(fixture, 'bin/init-store.sh'),
    '#!/bin/bash\nset -e\nshopt -s dotglob\nread -r password\n[[ "$password" == pa ]]\n' +
    'for file in conf/graphs/*.properties; do\n' +
    '  name=$(basename "$file" .properties)\n' +
    '  if [[ ! "$name" =~ ^[a-zA-Z][a-zA-Z0-9_]{0,47}$ ]]; then\n' +
    '    echo "Invalid graph name: $name" >&2\n    exit 1\n  fi\n' +
    'done\nprintf "%s" "$JAVA_HOME" > initialized-with-jvm\n', { mode: 0o755 });
  writeFileSync(join(fixture, 'bin/start-hugegraph.sh'),
    '#!/bin/bash\nprintf "%s" "$JAVA_HOME" > started-with-jvm\n', { mode: 0o755 });
}

function fixtureJavaHome(root) {
  const home = join(root, 'fixture-java11');
  mkdirSync(join(home, 'bin'), { recursive: true });
  writeFileSync(join(home, 'bin/keytool'), '#!/bin/bash\nset -e\noperation=$1\nshift\n' +
    'while [[ $# -gt 0 ]]; do\ncase "$1" in\n' +
    '  -keystore) store=$2; shift 2;;\n  -file) file=$2; shift 2;;\n  *) shift;;\nesac\ndone\n' +
    'if [[ "$operation" == -exportcert ]]; then printf "fixture cert" > "$file";\n' +
    'else printf "fixture store" > "$store"; fi\n', { mode: 0o755 });
  return home;
}

function runStarter(root, entries, extraEnv = {}) {
  const archive = join(root, 'server.tar.gz');
  // Keep metadata entries as files on macOS too, matching the Linux CI extractor.
  const env = { ...process.env, COPYFILE_DISABLE: '1', RUNNER_TEMP: root, JAVA_HOME: fixtureJavaHome(root),
    GITHUB_ENV: join(root, 'github-env'), ...extraEnv };
  const bin = join(root, 'tar-bin');
  mkdirSync(bin);
  writeFileSync(join(bin, 'curl'), '#!/bin/bash\nexit 0\n', { mode: 0o755 });
  env.PATH = `${bin}:${env.PATH}`;
  if (process.platform === 'darwin') {
    writeFileSync(join(bin, 'tar'),
      '#!/bin/bash\n[[ "$1" == -* ]] || set -- "-$1" "${@:2}"\n' +
      'exec /usr/bin/tar --no-mac-metadata "$@"\n', { mode: 0o755 });
    env.PATH = `${bin}:${env.PATH}`;
  }
  execFileSync('tar', ['czf', archive, '-C', root, ...entries], { env });
  const result = spawnSync('bash', [join(__dirname, 'start-hugegraph-servers.sh'), archive],
    { env, encoding: 'utf8' });
  const serverRoot = join(root, readdirSync(root).find(name => name.startsWith('hugegraph-servers.')));
  return { result, serverRoot, env, archive };
}

test('legacy Loader caller transfers the generated truststore to Maven without GITHUB_ENV', t => {
  const root = temp(t);
  const top = 'apache-hugegraph-fixture';
  serverFixture(join(root, top));
  const { result, env, archive } = runStarter(root, [top], { GITHUB_ENV: '' });
  assert.equal(result.status, 0, result.stderr);
  assert.equal(existsSync(join(root, 'github-env')), false);
  const bin = join(root, 'tar-bin');
  writeFileSync(join(bin, 'install-hugegraph-from-source.sh'),
    '#!/bin/bash\nexec bash "$REAL_STARTER" "$FIXTURE_ARCHIVE"\n', { mode: 0o755 });
  writeFileSync(join(bin, 'mvn'), '#!/bin/bash\nset -e\n' +
    'test -f "$HUGEGRAPH_TEST_TRUST_STORE_FILE"\n' +
    'printf "%s\\n" "$HUGEGRAPH_TEST_TRUST_STORE_FILE" >> "$CONSUMED_TRUST_STORE"\n', { mode: 0o755 });
  const legacy = readFileSync(join(__dirname, '../../../hugegraph-loader/assembly/travis/.travis.yml'), 'utf8');
  const before = legacy.match(/before_script:\n  - \|\n((?:    .*\n)+)/)[1].replace(/^    /gm, '');
  const script = legacy.match(/^script:\n((?:  - .*\n)+)/m)[1].replace(/^  - /gm, '');
  const consumed = join(root, 'consumed-trust-store');
  const run = spawnSync('bash', ['-c', `set -eo pipefail\n${before}\n${script}`], {
    env: { ...env, TRAVIS_DIR: bin, TRAVIS_BRANCH: 'fixture', SOURCE_TYPE: 'file',
      REAL_STARTER: join(__dirname, 'start-hugegraph-servers.sh'), FIXTURE_ARCHIVE: archive,
      CONSUMED_TRUST_STORE: consumed }, encoding: 'utf8'
  });
  assert.equal(run.status, 0, run.stderr);
  const paths = readFileSync(consumed, 'utf8').trim().split('\n');
  assert.equal(paths.length, 2);
  assert.equal(paths[0], paths[1]);
  assert.ok(paths[0].startsWith(root));
  assert.ok(existsSync(paths[0]));
  assert.equal(existsSync(join(root, 'github-env')), false);
});

for (const status of [0, 1]) {
  test(`legacy Loader removes the transfer marker when startup exits ${status} without a truststore`, t => {
    const root = temp(t);
    const bin = join(root, 'bin');
    mkdirSync(bin);
    writeFileSync(join(bin, 'install-hugegraph-from-source.sh'), '#!/bin/bash\n' +
      'printf "%s" "$HUGEGRAPH_TEST_TRUST_STORE_PATH_FILE" > "$MARKER_RECORD"\n' +
      'echo "Fixture did not become ready"\nexit "$STARTUP_EXIT"\n', { mode: 0o755 });
    const legacy = readFileSync(join(__dirname, '../../../hugegraph-loader/assembly/travis/.travis.yml'), 'utf8');
    const before = legacy.match(/before_script:\n  - \|\n((?:    .*\n)+)/)[1].replace(/^    /gm, '');
    const record = join(root, 'marker-record');
    const run = spawnSync('bash', ['-c', `set -eo pipefail\n${before}`], {
      env: { ...process.env, GITHUB_ENV: '', TRAVIS_DIR: bin, TRAVIS_BRANCH: 'fixture',
        MARKER_RECORD: record, STARTUP_EXIT: String(status) }, encoding: 'utf8'
    });
    assert.notEqual(run.status, 0, 'A missing generated truststore must fail setup');
    const marker = readFileSync(record, 'utf8');
    assert.ok(marker.length > 0);
    assert.equal(existsSync(marker), false, 'Failed setup must remove its temporary transfer marker');
  });
}

for (const layout of [
  { name: 'direct candidate', top: 'apache-hugegraph-fixture', server: 'apache-hugegraph-fixture' },
  { name: 'official aggregate', top: 'apache-hugegraph-incubating-1.7.0',
    server: 'apache-hugegraph-incubating-1.7.0/apache-hugegraph-server-incubating-1.7.0' }
]) {
  test(`shared starter launches ${layout.name} HTTP/auth and HTTPS with caller JVM`, t => {
    const root = temp(t);
    serverFixture(join(root, layout.server));
    if (layout.server !== layout.top) {
      // Sibling components in the real release are not server candidates.
      mkdirSync(join(root, layout.top, 'apache-hugegraph-pd-incubating-1.7.0'));
      mkdirSync(join(root, layout.top, 'apache-hugegraph-store-incubating-1.7.0'));
    }
    const { result, serverRoot } = runStarter(root, [layout.top]);
    assert.equal(result.status, 0, result.stderr);
    for (const dir of [layout.server, 'hugegraph_https']) {
      const deployed = join(serverRoot, dir);
      assert.equal(readFileSync(join(deployed, 'started-with-jvm'), 'utf8'), join(root, 'fixture-java11'));
      assert.equal(readFileSync(join(deployed, 'initialized-with-jvm'), 'utf8'), join(root, 'fixture-java11'));
      assert.match(readFileSync(join(deployed, 'conf/graphs/hugegraph.properties'), 'utf8'), /HugeFactoryAuthProxy/);
      const rest = readFileSync(join(deployed, 'conf/rest-server.properties'), 'utf8');
      assert.match(rest, /auth.authenticator=org.apache.hugegraph.auth.StandardAuthenticator/);
      assert.match(rest, /auth.admin_pa=pa/);
      assert.match(rest, /batch.max_vertices_per_batch=500/);
      assert.match(rest, /batch.max_edges_per_batch=500/);
      assert.match(rest, dir === 'hugegraph_https' ? /rpc.server_port=8092/ : /rpc.server_port=8091/);
      assert.match(rest, dir === 'hugegraph_https' ? /https:\/\/127.0.0.1:8443/ : /http:\/\/127.0.0.1:8080/);
    }
    assert.match(readFileSync(join(serverRoot, 'hugegraph_https/conf/gremlin-server.yaml'), 'utf8'), /^port: 8282/m);
    assert.equal(existsSync(join(serverRoot, 'hugegraph_https/apache-hugegraph-pd-incubating-1.7.0')), false);
    assert.equal(readFileSync(join(serverRoot, 'hugegraph_https/conf/hugegraph-server.keystore'), 'utf8'),
      readFileSync(join(serverRoot, 'test-tls/hugegraph-server.keystore'), 'utf8'));
    assert.equal(readFileSync(join(root, 'github-env'), 'utf8'),
      `HUGEGRAPH_TEST_TRUST_STORE_FILE=${join(serverRoot, 'test-tls/hugegraph.truststore')}\n`);
  });
}

test('HTTPS fixture creates fresh matching stores with localhost identities', t => {
  const root = temp(t);
  const destination = join(root, 'TLS fixture with spaces');
  const helper = join(__dirname, 'tls-fixture.sh');
  execFileSync('bash', [helper, destination], { stdio: ['ignore', 'pipe', 'pipe'] });
  const keytool = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin/keytool') : 'keytool';
  const inspect = file => execFileSync(keytool, ['-J-Duser.language=en', '-J-Duser.country=US',
    '-J-Duser.timezone=UTC', '-list', '-v', '-keystore', join(destination, file),
    '-storepass', 'hugegraph'], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  const server = inspect('hugegraph-server.keystore');
  const trust = inspect('hugegraph.truststore');
  assert.match(server, /PrivateKeyEntry/);
  assert.match(trust, /trustedCertEntry/);
  assert.doesNotMatch(trust, /PrivateKeyEntry/);
  assert.match(server, /SHA256withRSA/);
  assert.match(server, /2048-bit RSA/);
  assert.match(server, /DNSName: localhost/);
  assert.match(server, /IPAddress: 127\.0\.0\.1/);
  assert.equal(server.match(/SHA256: (\S+)/)[1], trust.match(/SHA256: (\S+)/)[1]);
  const validity = server.match(/Valid from: (.+) until: (.+)/);
  const now = Date.now();
  assert.ok(Date.parse(validity[1]) <= now);
  assert.ok(Date.parse(validity[2]) > now + 86400000);
  assert.ok(Date.parse(validity[2]) <= now + 2 * 86400000);
  const probe = join(root, 'TlsFixtureProbe.java');
  writeFileSync(probe, `
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

class TlsFixtureProbe {
    static SSLContext context(String path, boolean server) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            store.load(input, "hugegraph".toCharArray());
        }
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        KeyManager[] keys = null;
        if (server) {
            KeyManagerFactory manager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            manager.init(store, "hugegraph".toCharArray());
            keys = manager.getKeyManagers();
        }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys, trust.getTrustManagers(), null);
        return context;
    }
    public static void main(String[] args) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context(args[0], true)));
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, 2);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(new byte[]{'o', 'k'});
            }
        });
        server.start();
        try {
            SSLContext trust = context(args[1], false);
            HttpClient client = HttpClient.newBuilder().sslContext(trust).build();
            int port = server.getAddress().getPort();
            for (String host : new String[]{"localhost", "127.0.0.1"}) {
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + host + ":" + port)).build();
                if (!client.send(request, HttpResponse.BodyHandlers.ofString()).body().equals("ok")) {
                    throw new AssertionError("HTTPS fixture request failed");
                }
            }
            try (Socket plain = new Socket("127.0.0.1", port);
                 SSLSocket tls = (SSLSocket) trust.getSocketFactory().createSocket(
                         plain, "wrong.example", port, true)) {
                SSLParameters parameters = tls.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                tls.setSSLParameters(parameters);
                try {
                    tls.startHandshake();
                    throw new AssertionError("Wrong hostname was accepted");
                } catch (SSLHandshakeException expected) {
                    // The trusted fixture must still reject identities outside its SAN.
                }
            }
        } finally {
            server.stop(0);
        }
    }
}
`);
  const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin/java') : 'java';
  execFileSync(java, [probe, join(destination, 'hugegraph-server.keystore'),
    join(destination, 'hugegraph.truststore')], { timeout: 30000, stdio: ['ignore', 'pipe', 'pipe'] });
  const rerun = spawnSync('bash', [helper, destination], { encoding: 'utf8' });
  assert.notEqual(rerun.status, 0, 'Existing fixture stores must not be silently overwritten');
});

test('shared starter rejects multiple runnable server distributions before starting either', t => {
  const root = temp(t);
  const top = 'apache-hugegraph-ambiguous';
  const paths = [top, `${top}/apache-hugegraph-server-nested`];
  paths.forEach(path => serverFixture(join(root, path)));
  const { result, serverRoot } = runStarter(root, [top]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Expected exactly one runnable server distribution, found 2/);
  for (const path of paths) {
    assert.equal(existsSync(join(serverRoot, path, 'initialized-with-jvm')), false);
    assert.equal(existsSync(join(serverRoot, path, 'started-with-jvm')), false);
  }
  assert.equal(existsSync(join(serverRoot, 'hugegraph_https')), false);
});

test('shared starter rejects an incomplete server before initialization', t => {
  const root = temp(t);
  const top = 'apache-hugegraph-incomplete';
  serverFixture(join(root, top));
  rmSync(join(root, top, 'conf/graphs/hugegraph.properties'));
  const { result, serverRoot } = runStarter(root, [top]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Expected exactly one runnable server distribution, found 0/);
  assert.equal(existsSync(join(serverRoot, top, 'initialized-with-jvm')), false);
  assert.equal(existsSync(join(serverRoot, top, 'started-with-jvm')), false);
  assert.equal(existsSync(join(serverRoot, 'hugegraph_https')), false);
});


test('shared starter removes AppleDouble sidecars without replacing released content', t => {
  const root = temp(t);
  const top = 'apache-hugegraph-incubating-1.7.0';
  const server = `${top}/apache-hugegraph-server-incubating-1.7.0`;
  serverFixture(join(root, server));
  mkdirSync(join(root, server, 'lib'));
  // GNU tar retains these macOS sidecars; ConfigUtil scans every *.properties.
  writeFileSync(join(root, server, 'conf/graphs/._hugegraph.properties'), 'metadata sidecar');
  writeFileSync(join(root, server, 'lib/._released.jar'), 'metadata sidecar');
  writeFileSync(join(root, server, 'lib/released.jar'), 'original released binary');
  writeFileSync(join(root, server, 'conf/.keep'), 'ordinary dotfile');
  writeFileSync(join(root, '._outside.properties'), 'outside the extracted distribution');
  const { result, serverRoot } = runStarter(root, [top]);
  assert.equal(result.status, 0, result.stderr);
  for (const dir of [server, 'hugegraph_https']) {
    const deployed = join(serverRoot, dir);
    assert.equal(readFileSync(join(deployed, 'started-with-jvm'), 'utf8'), join(root, 'fixture-java11'));
    assert.equal(existsSync(join(deployed, 'conf/graphs/._hugegraph.properties')), false);
    assert.equal(existsSync(join(deployed, 'lib/._released.jar')), false);
    assert.equal(readFileSync(join(deployed, 'lib/released.jar'), 'utf8'), 'original released binary');
    assert.equal(readFileSync(join(deployed, 'conf/.keep'), 'utf8'), 'ordinary dotfile');
    assert.match(readFileSync(join(deployed, 'conf/graphs/hugegraph.properties'), 'utf8'), /HugeFactoryAuthProxy/);
  }
  assert.equal(readFileSync(join(root, '._outside.properties'), 'utf8'), 'outside the extracted distribution');
});

test('shared starter still rejects invalid non-metadata graph names', t => {
  const root = temp(t);
  const top = 'apache-hugegraph-invalid-graph';
  serverFixture(join(root, top));
  writeFileSync(join(root, top, 'conf/graphs/9invalid.properties'), 'invalid graph configuration');
  const { result, serverRoot } = runStarter(root, [top]);
  assert.notEqual(result.status, 0);
  assert.equal(existsSync(join(serverRoot, top, 'conf/graphs/9invalid.properties')), true);
  assert.equal(existsSync(join(serverRoot, top, 'started-with-jvm')), false);
});


test('Loader reports ambiguous archives before shared startup', t => {
  const root = temp(t);
  const cache = join(root, 'cache');
  mkdirSync(cache);
  writeFileSync(join(cache, 'apache-hugegraph-cached.tar.gz'), 'cached archive');
  cacheStamp(cache, 'apache/hugegraph', 'a'.repeat(40), 'apache-hugegraph-cached.tar.gz');
  writeFileSync(join(root, 'apache-hugegraph-leftover.tar.gz'), 'leftover archive');
  const installer = join(__dirname, '../../../hugegraph-loader/assembly/travis/install-hugegraph-from-source.sh');
  const result = spawnSync('bash', [installer, 'a'.repeat(40)], {
    cwd: root, env: { ...process.env, SERVER_CACHE_DIR: cache }, encoding: 'utf8'
  });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /Expected exactly one server archive, found:/);
  assert.match(result.stderr, /apache-hugegraph-cached\.tar\.gz/);
  assert.match(result.stderr, /apache-hugegraph-leftover\.tar\.gz/);
  assert.equal(readdirSync(root).some(name => name.startsWith('hugegraph-servers.')), false);
});

test('Loader retains its source cache while using the selected repository and shared startup', t => {
  const root = temp(t);
  const source = join(root, 'source');
  const git = (...args) => execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  git('init', source);
  git('-C', source, 'config', 'user.name', 'Fixture');
  git('-C', source, 'config', 'user.email', 'fixture@example.invalid');
  writeFileSync(join(source, 'pom.xml'), 'fixture');
  git('-C', source, 'add', 'pom.xml');
  git('-C', source, 'commit', '-m', 'initial');
  const sha = git('-C', source, 'rev-parse', 'HEAD');
  const top = 'apache-hugegraph-loader-fixture';
  writeFileSync(join(source, 'pom.xml'), 'another commit');
  git('-C', source, 'commit', '-am', 'another commit');
  const anotherSha = git('-C', source, 'rev-parse', 'HEAD');
  serverFixture(join(root, top));
  const archive = join(root, 'fixture.tar.gz');
  execFileSync('tar', ['czf', archive, '-C', root, top],
    { env: { ...process.env, COPYFILE_DISABLE: '1' } });
  const bin = join(root, 'bin');
  mkdirSync(bin);
  writeFileSync(join(bin, 'curl'), '#!/bin/bash\nexit 0\n', { mode: 0o755 });
  writeFileSync(join(bin, 'mvn'), '#!/bin/bash\nprintf "%s\\n" "$@" > "$BUILD_ARGS"\n' +
    'mkdir -p hugegraph-server\ncp "$FIXTURE_ARCHIVE" hugegraph-server/apache-hugegraph-fixture.tar.gz\n' +
    'if [[ "$DUPLICATE_ARCHIVE" == true ]]; then\n' +
    '  cp "$FIXTURE_ARCHIVE" hugegraph-server/apache-hugegraph-other.tar.gz\nfi\n',
    { mode: 0o755 });
  const env = { ...process.env, SERVER_REPOSITORY: 'example/server', SERVER_FETCH_REF: sha,
    SERVER_CACHE_DIR: join(root, 'cache'), FIXTURE_ARCHIVE: archive, BUILD_ARGS: join(root, 'args'),
    PATH: `${bin}:${process.env.PATH}`, RUNNER_TEMP: root, JAVA_HOME: fixtureJavaHome(root),
    GITHUB_ENV: join(root, 'github-env'),
    GIT_CONFIG_COUNT: '1', GIT_CONFIG_KEY_0: `url.file://${source}.insteadOf`,
    GIT_CONFIG_VALUE_0: 'https://github.com/example/server.git' };
  const installer = join(__dirname, '../../../hugegraph-loader/assembly/travis/install-hugegraph-from-source.sh');
  for (const failure of [
    { name: 'checkout', extra: { SERVER_FETCH_REF: anotherSha }, error: /Server checkout mismatch/ },
    { name: 'build', extra: { DUPLICATE_ARCHIVE: 'true' }, error: /Expected exactly one built server archive/ }
  ]) {
    const cwd = join(root, `run-failed-${failure.name}`);
    const cache = join(root, `cache-failed-${failure.name}`);
    mkdirSync(cwd);
    const result = spawnSync('bash', [installer, sha], {
      cwd, env: { ...env, ...failure.extra, SERVER_CACHE_DIR: cache }, encoding: 'utf8'
    });
    assert.equal(result.status, 1);
    assert.match(result.stderr, failure.error);
    assert.deepEqual(readdirSync(cache), []);
    assert.equal(readdirSync(root).some(name => name.startsWith('hugegraph-servers.')), false);
  }
  for (const pass of ['build', 'cache']) {
    const cwd = join(root, `run-${pass}`);
    mkdirSync(cwd);
    const result = spawnSync('bash', [installer, pass === 'cache' ? sha.toUpperCase() : sha],
      { cwd, env, encoding: 'utf8' });
    assert.equal(result.status, 0, result.stderr);
    assert.deepEqual(readFileSync(env.BUILD_ARGS, 'utf8').trim().split('\n'),
      ['package', '-DskipTests', '-Dmaven.javadoc.skip=true', '-ntp']);
    assert.equal(existsSync(join(cwd, 'hugegraph')), false);
    if (pass === 'build') {
      assert.deepEqual(readdirSync(env.SERVER_CACHE_DIR).sort(),
        ['apache-hugegraph-fixture.tar.gz', 'server-provenance']);
      assert.equal(readFileSync(join(env.SERVER_CACHE_DIR, 'server-provenance'), 'utf8'),
        `example/server\n${sha}\napache-hugegraph-fixture.tar.gz\n` +
        `${createHash('sha256').update(readFileSync(archive)).digest('hex')}\n`);
      // A cache hit must not execute Maven or reach the source repository again.
      writeFileSync(join(bin, 'mvn'), '#!/bin/bash\nexit 91\n', { mode: 0o755 });
      rmSync(source, { recursive: true, force: true });
    }
  }
  const otherCwd = join(root, 'run-other-commit');
  mkdirSync(otherCwd);
  const other = spawnSync('bash', [installer, anotherSha], { cwd: otherCwd, env, encoding: 'utf8' });
  assert.equal(other.status, 1);
  assert.match(other.stderr, /source or archive provenance mismatch/);
  assert.deepEqual(readdirSync(otherCwd), []);
  const deployments = readdirSync(root).filter(name => name.startsWith('hugegraph-servers.'));
  assert.equal(deployments.length, 2);
  for (const deployment of deployments) {
    for (const dir of [top, 'hugegraph_https']) {
      assert.equal(readFileSync(join(root, deployment, dir, 'started-with-jvm'), 'utf8'),
        join(root, 'fixture-java11'));
    }
  }
});

const candidateSdkModules = [
  ['pom.xml', 'hugegraph', 'pom'],
  ['hugegraph-commons/pom.xml', 'hugegraph-commons', 'pom'],
  ['hugegraph-server/pom.xml', 'hugegraph-server', 'pom'],
  ['hugegraph-pd/pom.xml', 'hugegraph-pd', 'pom'],
  ['hugegraph-store/pom.xml', 'hugegraph-store', 'pom'],
  ['hugegraph-commons/hugegraph-common/pom.xml', 'hugegraph-common', 'jar'],
  ['hugegraph-server/hugegraph-core/pom.xml', 'hugegraph-core', 'jar'],
  ['hugegraph-struct/pom.xml', 'hugegraph-struct', 'jar'],
  ['hugegraph-pd/hg-pd-common/pom.xml', 'hg-pd-common', 'jar'],
  ['hugegraph-pd/hg-pd-client/pom.xml', 'hg-pd-client', 'jar'],
  ['hugegraph-pd/hg-pd-grpc/pom.xml', 'hg-pd-grpc', 'jar'],
  ['hugegraph-store/hg-store-common/pom.xml', 'hg-store-common', 'jar'],
  ['hugegraph-store/hg-store-client/pom.xml', 'hg-store-client', 'jar'],
  ['hugegraph-store/hg-store-grpc/pom.xml', 'hg-store-grpc', 'jar']
];

function candidateSdkFixture(t) {
  const root = temp(t);
  const source = join(root, 'source');
  const git = (...args) => execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  git('init', source);
  git('-C', source, 'config', 'user.name', 'Fixture');
  git('-C', source, 'config', 'user.email', 'fixture@example.invalid');
  const revision = '9.8.7'; // A source-derived version, deliberately different from the current SDK.
  const version = '${revision}';
  for (const [pomPath, artifactId, packaging] of candidateSdkModules) {
    const parts = pomPath.split('/');
    const parent = parts.length > 2 ? parts[parts.length - 3] : 'hugegraph';
    const coordinates = pomPath === 'pom.xml'
      ? '<groupId>org.apache.hugegraph</groupId>'
      : `<parent><groupId>org.apache.hugegraph</groupId><artifactId>${parent}</artifactId><version>${version}</version></parent>`;
    const properties = pomPath === 'pom.xml' ? `<properties><revision>${revision}</revision></properties>` : '';
    mkdirSync(join(source, ...parts.slice(0, -1)), { recursive: true });
    writeFileSync(join(source, pomPath),
      `<project xmlns="http://maven.apache.org/POM/4.0.0">${coordinates}<artifactId>${artifactId}</artifactId>` +
      (packaging === 'pom' ? `<version>${version}</version><packaging>pom</packaging>` : '') + properties + '</project>');
  }
  git('-C', source, 'add', '.');
  git('-C', source, 'commit', '-m', 'initial');
  const sha = git('-C', source, 'rev-parse', 'HEAD');
  const bin = join(root, 'bin');
  const javaHome = join(root, 'jdk');
  const repo = join(root, 'm2');
  mkdirSync(bin);
  mkdirSync(join(javaHome, 'bin'), { recursive: true });
  mkdirSync(repo);
  const coordinates = join(root, 'coordinates');
  writeFileSync(coordinates, candidateSdkModules.map(([, id, packaging]) => `${id} ${packaging}`).join('\n') + '\n');
  // Check the CLI contract without compiling a synthetic reactor or starting a server.
  writeFileSync(join(javaHome, 'bin/java'), '#!/bin/bash\nprintf "    java.specification.version = %s\\n" "$SDK_JAVA_VERSION" >&2\n', { mode: 0o755 });
  writeFileSync(join(bin, 'mvn'), `#!/bin/bash
set -e
printf '%s\\n' "$@" > "$BUILD_ARGS"
repo="\${1#-Dmaven.repo.local=}"
while read -r id packaging; do
  [[ "$SDK_CASE" != partial || "$packaging" == pom || "$id" == hugegraph-common ]] || continue
  artifact="$repo/org/apache/hugegraph/$id/$SDK_REVISION"
  mkdir -p "$artifact"
  for ext in pom jar; do
    [[ "$ext" != jar || "$packaging" == jar ]] || continue
    case "$SDK_CASE:$id:$ext" in
      missing-jar:hg-pd-grpc:jar|missing-pom:hg-store-common:pom) continue ;;
    esac
    file="$id-$SDK_REVISION.$ext"
    printf 'same-source fixture' > "$artifact/$file"
    case "$SDK_CASE:$id:$ext" in
      remote-jar:hg-pd-client:jar|remote-pom:hg-store-client:pom|remote-parent:hugegraph-commons:pom)
        printf '%s>central=\\n' "$file" >> "$artifact/_remote.repositories" ;;
      *) printf '%s>=\\n' "$file" >> "$artifact/_remote.repositories" ;;
    esac
  done
done < "$SDK_COORDINATES"
`, { mode: 0o755 });
  const env = { ...process.env, JAVA_HOME: javaHome, SDK_JAVA_VERSION: '17', SDK_CASE: 'complete',
    SDK_REVISION: revision, SDK_COORDINATES: coordinates,
    SERVER_REPOSITORY: 'example/server', SERVER_FETCH_REF: sha,
    PATH: `${bin}:${process.env.PATH}`, BUILD_ARGS: join(root, 'args'),
    GIT_CONFIG_COUNT: '1', GIT_CONFIG_KEY_0: `url.file://${source}.insteadOf`,
    GIT_CONFIG_VALUE_0: 'https://github.com/example/server.git' };
  const run = (name, commit = sha, extra = {}) => spawnSync('bash',
    [join(__dirname, 'install-candidate-sdk.sh'), commit, join(root, name), repo],
    { cwd: root, env: { ...env, ...extra }, encoding: 'utf8' });
  return { root, repo, sha, env, revision, run };
}

test('candidate SDK installs the verified reactor into an explicit repository and records provenance', t => {
  const { root, repo, sha, env, revision, run } = candidateSdkFixture(t);
  const result = run('checkout');
  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(readFileSync(env.BUILD_ARGS, 'utf8').trim().split('\n'),
    [`-Dmaven.repo.local=${repo}`, '-pl',
      'hugegraph-server/hugegraph-dist,hugegraph-pd/hg-pd-client,hugegraph-store/hg-store-client,hugegraph-struct',
      '-am', 'org.codehaus.mojo:flatten-maven-plugin:1.3.0:flatten', 'install',
      '-Dflatten.mode=resolveCiFriendliesOnly', '-DupdatePomFile=true',
      '-DskipTests', '-Dmaven.javadoc.skip=true', '-ntp']);
  const manifest = JSON.parse(readFileSync(join(repo, 'candidate-sdk-manifest.json'), 'utf8'));
  assert.equal(manifest.repository, 'example/server');
  assert.equal(manifest.commit, sha);
  assert.equal(manifest.source_dir, join(root, 'checkout'));
  assert.equal(manifest.java_home, env.JAVA_HOME);
  assert.equal(manifest.java_version, '17');
  assert.equal(manifest.source_revision, revision);
  assert.equal(manifest.required_sdk_modules.length, candidateSdkModules.length);
  assert.ok(manifest.required_sdk_modules.every(module => module.version === revision));
  assert.equal(manifest.required_sdk_modules.reduce((count, module) => count + module.files.length, 0), 23);
  assert.equal(manifest.artifacts.length, 23);
  assert.ok(manifest.artifacts.every(artifact => artifact.source_reactor_install));
  const jar = manifest.artifacts.find(artifact => artifact.path.endsWith('.jar'));
  assert.equal(jar.sha256, require('node:crypto').createHash('sha256').update('same-source fixture').digest('hex'));
  assert.equal(readFileSync(join(repo, 'candidate-source.txt'), 'utf8'), `example/server@${sha}\n`);
  assert.equal(readdirSync(root).some(name => name.startsWith('hugegraph-servers.')), false);
});

test('candidate SDK rejects a moved source before Maven or manifest publication', t => {
  const { repo, env, run } = candidateSdkFixture(t);
  const result = run('moved', '0'.repeat(40));
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Server checkout mismatch/);
  assert.equal(existsSync(env.BUILD_ARGS), false);
  assert.equal(existsSync(join(repo, 'candidate-sdk-manifest.json')), false);
});

test('candidate SDK refuses missing JVMs, unsupported build JVMs and existing artifact repositories', t => {
  const { root, repo, env, run } = candidateSdkFixture(t);
  assert.notEqual(run('missing-jvm', undefined, { JAVA_HOME: '' }).status, 0);
  assert.notEqual(run('legacy-jvm', undefined, { SDK_JAVA_VERSION: '11' }).status, 0);
  for (const version of ['18', '21']) {
    const result = run(`unsupported-jvm-${version}`, undefined, { SDK_JAVA_VERSION: version });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /requires JDK 17/);
    assert.equal(existsSync(join(root, `unsupported-jvm-${version}`)), false);
  }
  assert.equal(existsSync(env.BUILD_ARGS), false);
  assert.equal(existsSync(join(repo, 'candidate-sdk-manifest.json')), false);
  mkdirSync(join(repo, 'org/apache/hugegraph'), { recursive: true });
  const result = run('existing-repo');
  assert.notEqual(result.status, 0);
  assert.equal(existsSync(join(root, 'existing-repo')), false);
  assert.equal(existsSync(env.BUILD_ARGS), false);
});

for (const scenario of ['partial', 'missing-jar', 'missing-pom', 'remote-jar', 'remote-pom', 'remote-parent']) {
  test(`candidate SDK rejects ${scenario} reactor artifacts without publishing provenance`, t => {
    const { repo, run } = candidateSdkFixture(t);
    const result = run('checkout', undefined, { SDK_CASE: scenario });
    assert.notEqual(result.status, 0, result.stderr);
    assert.match(result.stderr, /required candidate SDK artifact/i);
    assert.equal(existsSync(join(repo, 'candidate-sdk-manifest.json')), false);
    assert.equal(existsSync(join(repo, 'candidate-source.txt')), false);
  });
}

for (const scenario of [
  { name: 'another commit', commit: 'b'.repeat(40), error: /provenance mismatch/ },
  { name: 'another repository', repository: 'example/other', error: /provenance mismatch/ },
  { name: 'a missing stamp', mutate: cache => rmSync(join(cache, 'server-provenance')),
    error: /missing provenance/ },
  { name: 'a tampered archive', mutate: cache => writeFileSync(join(cache, 'apache-hugegraph-fixture.tar.gz'),
    'modified archive'), error: /provenance mismatch/ },
  { name: 'multiple cached archives', mutate: cache => writeFileSync(join(cache, 'apache-hugegraph-other.tar.gz'),
    'another archive'), error: /exactly one regular server archive/ },
  { name: 'extra provenance data', mutate: cache => writeFileSync(join(cache, 'server-provenance'),
    readFileSync(join(cache, 'server-provenance'), 'utf8') + 'unexpected'), error: /provenance mismatch/ },
  { name: 'binary provenance data', mutate: cache => writeFileSync(join(cache, 'server-provenance'),
    readFileSync(join(cache, 'server-provenance'), 'utf8').replace('example/server', 'example/\0server')),
    error: /provenance mismatch/ },
  { name: 'executable metadata', mutate: cache => writeFileSync(join(cache, 'server-provenance'),
    '$(touch should-not-exist)\n' + 'a'.repeat(40) + '\napache-hugegraph-fixture.tar.gz\n' + '0'.repeat(64) + '\n'),
    error: /provenance mismatch/ }
]) {
  test(`Loader rejects ${scenario.name} without altering the custom cache or starting a server`, t => {
    const root = temp(t);
    const cache = join(root, 'cache');
    mkdirSync(cache);
    writeFileSync(join(cache, 'apache-hugegraph-fixture.tar.gz'), 'original archive');
    writeFileSync(join(cache, 'unrelated-file'), 'preserve this file');
    cacheStamp(cache, 'example/server', 'a'.repeat(40), 'apache-hugegraph-fixture.tar.gz');
    if (scenario.mutate) scenario.mutate(cache);
    const files = readdirSync(cache).map(name => [name, readFileSync(join(cache, name), 'utf8')]);
    const installer = join(__dirname, '../../../hugegraph-loader/assembly/travis/install-hugegraph-from-source.sh');
    const result = spawnSync('bash', [installer, scenario.commit || 'a'.repeat(40)], {
      cwd: root, env: { ...process.env, SERVER_CACHE_DIR: cache,
        SERVER_REPOSITORY: scenario.repository || 'example/server' }, encoding: 'utf8'
    });
    assert.equal(result.status, 1);
    assert.match(result.stderr, scenario.error);
    assert.match(result.stderr, /Use a fresh or separate SERVER_CACHE_DIR/);
    assert.deepEqual(readdirSync(cache).map(name => [name, readFileSync(join(cache, name), 'utf8')]), files);
    assert.equal(existsSync(join(root, 'hugegraph')), false);
    assert.equal(existsSync(join(root, 'should-not-exist')), false);
    assert.equal(readdirSync(root).some(name => name.startsWith('hugegraph-servers.')), false);
  });
}
