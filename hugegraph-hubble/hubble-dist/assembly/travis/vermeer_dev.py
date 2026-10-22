#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Retained cloud development environment: Hubble + Vermeer + HugeGraph hstore."""

import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import secrets
import shutil
import signal
import socket
import subprocess
import tarfile
import time
import urllib.request

REPO = Path(__file__).resolve().parents[4]
SETUP = Path(os.environ.get('VERMEER_DEV_HOME', '/workspace/.onboarding'))
RUN = SETUP / 'integration'
SOURCE = Path(os.environ.get('VERMEER_SOURCE_DIR', str(REPO.parent / 'hugegraph-computer/vermeer')))
GO = SETUP / 'tools/go1.23.7'
COMPUTER_COMMIT = '554daabae5b5ac2cadd3f23d464f54ec96d05eeb'
IMAGES = {
    'pd': 'hugegraph/pd@sha256:1ca2c236cba50ac27f47e9db4c9a44f1f50bb9df85a07e837b7c25ba7a2704a8',
    'store': 'hugegraph/store@sha256:1cc8d6a1e1624328b91e5346731b57c68569de435400981ff7287150ae63651c',
    'server': 'hugegraph/server@sha256:14b00b6edab43560c630326016e3a07579cc2a30b045b7edd3dc1c62a1ef1d98',
}
HUBBLE = 'http://127.0.0.1:8088'
VERMEER = os.environ.get('VERMEER_URL', 'http://127.0.0.1:6688')
API = HUBBLE + '/api/v1.3'
GRAPH_NAME = os.environ.get('VERMEER_GRAPH', 'vermeer_demo')
GRAPH = API + '/graphspaces/DEFAULT/graphs/' + GRAPH_NAME


def environment():
    env = os.environ.copy()
    env.update({
        'JAVA_HOME': str(SETUP / 'tools/jdk17'),
        'PATH': f'{GO}/bin:{SETUP}/bin:{SETUP}/tools/jdk17/bin:' + env['PATH'],
        'GOTOOLCHAIN': 'local',
        'GOPATH': env.get('GOPATH', str(SETUP / 'go')),
        'GOMODCACHE': env.get('GOMODCACHE', str(SETUP / 'go-mod')),
        'GOCACHE': env.get('GOCACHE', str(SETUP / 'go-build')),
    })
    return env


def command(args, cwd=REPO, **kwargs):
    return subprocess.run([str(x) for x in args], cwd=cwd, env=environment(),
                          check=True, **kwargs)


def request(url, method='GET', data=None, hubble=False):
    body = None if data is None else json.dumps(data).encode()
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    req = urllib.request.Request(url, data=body, method=method,
                                 headers={'Content-Type': 'application/json'})
    with opener.open(req, timeout=35) as response:
        payload = response.read()
        value = json.loads(payload) if payload else {'ready': True}
    if hubble:
        if value.get('status') != 200:
            raise RuntimeError(f'{url}: {value.get("message")}')
        return value['data']
    if value.get('errcode', 0) != 0:
        raise RuntimeError(f'{url}: {value.get("message")}')
    return value


def wait_ready(check, name, seconds=120):
    deadline = time.monotonic() + seconds
    error = None
    while time.monotonic() < deadline:
        try:
            value = check()
            if value:
                print(f'{name}: ready', flush=True)
                return value
        except Exception as exc:
            error = exc
        time.sleep(1)
    raise RuntimeError(f'{name} did not become ready: {error}; inspect {RUN}')


def private_file(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', opener=lambda p, flags: os.open(p, flags, 0o600)) as output:
        output.write(value)
    path.chmod(0o600)


def configure():
    RUN.mkdir(parents=True, exist_ok=True)
    (RUN / 'vermeer').mkdir(exist_ok=True)
    (RUN / 'hubble-home/conf/graphs').mkdir(parents=True, exist_ok=True)
    password = Path(os.environ.get('VERMEER_PD_PASSWORD_FILE', str(RUN / 'pd-password')))
    if not password.exists():
        private_file(password, secrets.token_hex(24) + '\n')
    secret = password.read_text().strip()
    if not secret:
        raise RuntimeError('Empty PD password file')
    private_file(RUN / 'pd.env', f'HG_PD_AUTH_SECRET_KEY={secret}\n')
    private_file(RUN / 'server.env', f'PD_AUTH_PASSWORD={secret}\n' +
                 'HG_SERVER_BACKEND=hstore\nHG_SERVER_PD_PEERS=127.0.0.1:8686\n'
                 'HG_SERVER_USE_PD=true\nHG_SERVER_CLUSTER=hg\n'
                 'HG_SERVER_REST_URL=http://127.0.0.1:8080\nHG_SERVER_MIN_FREE_MEMORY=0\n'
                 'HG_SERVER_INIT_STORE_ENABLED=false\nHG_SERVER_STARTUP_TIMEOUT_S=180\n')
    for mode in ('master', 'worker'):
        text = (SOURCE / f'config/{mode}.ini').read_text().replace('0.0.0.0', '127.0.0.1')
        text += f'\npd_password_file={password}\n'
        metadata_credentials = os.environ.get('VERMEER_HUGEGRAPH_CREDENTIALS_FILE')
        if metadata_credentials:
            text += f'hugegraph_credentials_file={metadata_credentials}\n'
        private_file(RUN / f'vermeer/{mode}.ini', text)
    template = REPO / 'hugegraph-hubble/hubble-dist/assembly/static/conf/hugegraph-hubble.properties'
    text = template.read_text().replace('dashboard.address=127.0.0.1:8092', 'dashboard.address=')
    text = text.replace('pd.enabled=false', 'pd.enabled=true')
    text = text.replace('vermeer.url=', 'vermeer.url=' + VERMEER)
    text = text.replace('server.host=localhost', 'server.host=127.0.0.1')
    private_file(RUN / 'hubble.properties', text)
    private_file(SETUP / 'vermeer-env.sh',
                 f'export PATH={GO}/bin:$PATH\nexport GOTOOLCHAIN=local\n'
                 f'export GOPATH={SETUP}/go\nexport GOMODCACHE={SETUP}/go-mod\n'
                 f'export GOCACHE={SETUP}/go-build\n'
                 f'export VERMEER_PD_PASSWORD_FILE={password}\n')


def prepare():
    if not SOURCE.exists():
        command(['git', 'clone', '--depth', '1', 'https://github.com/hugegraph/hugegraph-computer.git',
                 SOURCE.parent])
        command(['git', 'fetch', '--depth', '1', 'origin', COMPUTER_COMMIT], cwd=SOURCE.parent)
        command(['git', 'checkout', '--detach', COMPUTER_COMMIT], cwd=SOURCE.parent)
    # Existing development changes are deliberately retained.
    if not (GO / 'bin/go').exists():
        archive = SETUP / 'tools/go1.23.7.tar.gz'
        urllib.request.urlretrieve('https://dl.google.com/go/go1.23.7.linux-amd64.tar.gz', archive)
        expected = '4741525e69841f2e22f9992af25df0c1112b07501f61f741c12c6389fcb119f3'
        if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
            raise RuntimeError('Go archive checksum mismatch')
        GO.mkdir(parents=True, exist_ok=True)
        with tarfile.open(archive) as bundle:
            for member in bundle.getmembers():
                if member.name.startswith('go/'):
                    member.name = member.name[3:]
                    if member.name:
                        bundle.extract(member, GO, filter='data')
    for image in IMAGES.values():
        inspected = subprocess.run(['docker', 'image', 'inspect', image], capture_output=True)
        if inspected.returncode:
            command(['docker', 'pull', image])
    configure()
    build('all')


def build(component):
    if component in ('all', 'vermeer'):
        command(['go', 'mod', 'download'], cwd=SOURCE)
        command(['go', 'mod', 'verify'], cwd=SOURCE)
        command(['go', 'build', '-tags=dev', '-o', SETUP / 'vermeer-bin', '.'], cwd=SOURCE)
        command(['go', 'test', './apps/common', '-run', 'TestPDAuthority', '-count=1'], cwd=SOURCE)
    if component in ('all', 'hubble'):
        args = ['mvn', '-B', '-ntp', f'-Dmaven.repo.local={SETUP}/active-m2',
                '-Dsdk.validation.mode=candidate', '-pl', 'hugegraph-hubble/hubble-be']
        command(args + ['-am', 'install', '-DskipTests', '-Dloader.distribution.skip=true',
                        '-Dmaven.javadoc.skip=true'])
        command(args + ['org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath',
                        f'-Dmdep.outputFile={RUN}/hubble-classpath'])


def container(name, options):
    full_name = 'hg-dev-' + name
    inspected = subprocess.run(['docker', 'inspect', full_name], capture_output=True, text=True)
    if inspected.returncode == 0:
        info = json.loads(inspected.stdout)[0]
        if info['Config']['Image'] != IMAGES[name]:
            raise RuntimeError(f'{full_name} uses another image; preserve it before replacing')
        if not info['State']['Running']:
            command(['docker', 'start', full_name])
        return
    command(['docker', 'run', '-d', '--name', full_name, '--network', 'host',
             '--label', 'hugegraph.vermeer-dev=true', *options, IMAGES[name]])


def process(name, args, cwd, port, prepare=None):
    pidfile = RUN / (f'vermeer/{name}.pid' if name in ('master', 'worker') else f'{name}.pid')
    if pidfile.exists():
        proc = Path('/proc') / pidfile.read_text().strip() / 'cmdline'
        if proc.exists() and proc.read_bytes():
            if str(args[0]).encode() not in proc.read_bytes():
                raise RuntimeError(f'{pidfile} belongs to another process')
            return
    with socket.socket() as probe:
        if probe.connect_ex(('127.0.0.1', port)) == 0:
            raise RuntimeError(f'Port {port} is already in use; inspect its owner')
    if prepare:
        prepare()
    log = pidfile.with_suffix('.log').open('ab')
    child = subprocess.Popen([str(x) for x in args], cwd=cwd, env=environment(),
                             stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    log.close()
    pidfile.write_text(str(child.pid))


def snapshot_hubble_classes():
    # Maven can invalidate target/classes during tests. A running JVM must not
    # lazily load classes from a directory that a separate build is replacing.
    source = REPO / 'hugegraph-hubble/hubble-be/target/classes'
    if not (source / 'org/apache/hugegraph/HugeGraphHubble.class').is_file():
        raise RuntimeError('Build Hubble before starting the development service')
    target = RUN / 'hubble-classes'
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(source, target)


def start():
    configure()
    container('pd', ['--env-file', RUN / 'pd.env', '-e', 'HG_PD_GRPC_HOST=127.0.0.1',
                     '-e', 'HG_PD_RAFT_ADDRESS=127.0.0.1:8610',
                     '-e', 'HG_PD_RAFT_PEERS_LIST=127.0.0.1:8610',
                     '-e', 'HG_PD_INITIAL_STORE_LIST=127.0.0.1:8500', '-e', 'JAVA_OPTS=-Xmx512m',
                     '-v', f'{RUN}/pd:/hugegraph-pd/pd_data'])
    wait_ready(lambda: request('http://127.0.0.1:8620/v1/health'), 'PD')
    container('store', ['-e', 'HG_STORE_PD_ADDRESS=127.0.0.1:8686',
                        '-e', 'HG_STORE_GRPC_HOST=127.0.0.1', '-e', 'JAVA_OPTS=-Xmx768m',
                        '-e', 'HG_STORE_RAFT_ADDRESS=127.0.0.1:8510',
                        '-v', f'{RUN}/store:/hugegraph-store/storage'])
    wait_ready(lambda: request('http://127.0.0.1:8520/v1/health'), 'Store')
    container('server', ['--env-file', RUN / 'server.env',
                         '-v', f'{RUN}/server-data:/hugegraph-server/data'])
    wait_ready(lambda: request('http://127.0.0.1:8080/versions'), 'Server')
    for mode, port in [('master', 6688), ('worker', 6788)]:
        process(mode, [SETUP / 'vermeer-bin', f'--env={RUN}/vermeer/{mode}.ini'], RUN / 'vermeer', port)
        health = '/api/v1/healthcheck' if mode == 'master' else '/healthcheck'
        wait_ready(lambda p=port, h=health: request(f'http://127.0.0.1:{p}{h}'), mode)
    wait_ready(lambda: request(VERMEER + '/api/v1/workers').get('workers'), 'Worker registration')
    # The example worker is in group "default"; without allocation tasks stay queued.
    request(VERMEER + '/api/v1/admin/workers/alloc/default/%24DEFAULT', 'POST')
    classpath = (RUN / 'hubble-classpath').read_text().strip()
    process('hubble', [SETUP / 'tools/jdk17/bin/java', '-Dfile.encoding=UTF-8',
                       '--add-opens=java.base/java.net=ALL-UNNAMED', '-Dserver.address=127.0.0.1',
                       f'-Dhubble.home.path={RUN}/hubble-home',
                       f'-Dspring.datasource.url=jdbc:h2:file:{RUN}/hubble-data',
                       '-cp', f'{RUN}/hubble-classes:{classpath}',
                       'org.apache.hugegraph.HugeGraphHubble', RUN / 'hubble.properties'], REPO, 8088,
                       prepare=snapshot_hubble_classes)
    wait_ready(lambda: request(HUBBLE + '/about', hubble=True), 'Hubble')
    capabilities = request(API + '/config', hubble=True)
    if capabilities.get('auth_enabled'):
        print('Hubble authentication ready; sign in to verify Vermeer capability', flush=True)
    else:
        wait_ready(lambda: request(API + '/vermeer', hubble=True).get('enable'), 'Vermeer capability')


def wait_task(task_id):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        task = request(f'{GRAPH}/async-tasks/{task_id}', hubble=True)
        if task['task_status'] in ('failed', 'cancelled'):
            raise RuntimeError(f'Task {task_id} failed: {task.get("task_result")}')
        if task['task_status'] == 'success':
            print(f'task {task_id}: success', flush=True)
            return task
        time.sleep(1)
    raise RuntimeError(f'Task {task_id} did not complete within 90 seconds')


def smoke():
    graphs = 'http://127.0.0.1:8080/graphspaces/DEFAULT/graphs'
    if GRAPH_NAME not in request(graphs)['graphs']:
        request(graphs + '/' + GRAPH_NAME, 'POST',
                {'backend': 'hstore', 'serializer': 'binary', 'store': GRAPH_NAME,
                 'pd.peers': '127.0.0.1:8686'})
    server = graphs + '/' + GRAPH_NAME
    properties = request(server + '/schema/propertykeys')['propertykeys']
    if not any(prop['name'] == 'demo_name' for prop in properties):
        request(server + '/schema/propertykeys', 'POST',
                {'name': 'demo_name', 'data_type': 'TEXT', 'cardinality': 'SINGLE'})
    labels = request(server + '/schema/vertexlabels')['vertexlabels']
    if not any(label['name'] == 'vermeer_demo' for label in labels):
        request(server + '/schema/vertexlabels', 'POST',
                {'name': 'vermeer_demo', 'id_strategy': 'CUSTOMIZE_STRING', 'properties': ['demo_name']})
    edges = request(server + '/schema/edgelabels')['edgelabels']
    if not any(label['name'] == 'vermeer_link' for label in edges):
        request(server + '/schema/edgelabels', 'POST',
                {'name': 'vermeer_link', 'source_label': 'vermeer_demo', 'target_label': 'vermeer_demo'})
    # Fixed vertex IDs and SINGLE edges upsert this fixture on every run, so an
    # interrupted seed is repaired even when the Schema was already created.
    for i in range(1, 7):
        request(server + '/graph/vertices', 'POST',
                {'id': f'demo{i}', 'label': 'vermeer_demo', 'properties': {'demo_name': str(i)}})
    for a, b in [(1, 2), (2, 3), (3, 1), (3, 4), (4, 5), (5, 6), (6, 4)]:
        request(server + '/graph/edges', 'POST', {'label': 'vermeer_link', 'outV': f'demo{a}',
                'inV': f'demo{b}', 'outVLabel': 'vermeer_demo', 'inVLabel': 'vermeer_demo', 'properties': {}})
    loaded = request(API + '/vermeer/task', 'POST',
                     {'graphspace': 'DEFAULT', 'graph': GRAPH_NAME, 'task_type': 'reload'}, hubble=True)
    wait_task(loaded['task_id'])
    computed = request(GRAPH + '/algorithms/vermeer', 'POST',
                       {'params': {'compute.algorithm': 'pagerank', 'compute.max_step': 20,
                                   'pagerank.damping': 0.85}}, hubble=True)
    task = wait_task(computed['task_id'])
    values = json.loads(task['task_result'])['vertices']
    demo_values = [v for v in values if v['ID'] in {f'demo{i}' for i in range(1, 7)}]
    if len(demo_values) != 6 or any(not math.isfinite(float(v['Value'])) for v in demo_values):
        raise RuntimeError('PageRank did not return six finite demo vertex values')
    listing = request(GRAPH + '/async-tasks?status=SUCCESS&type=vermeer-task%3Acompute', hubble=True)
    if not any(t['id'] == computed['task_id'] for t in listing['records']):
        raise RuntimeError('Computed task missing from filtered task list')
    report = {'load_task': loaded['task_id'], 'compute_task': computed['task_id'],
              'demo_vertices': len(demo_values), 'values': demo_values}
    (RUN / 'smoke-result.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


def stop(component):
    names = ('hubble', 'master', 'worker') if component == 'all' else (
        ('master', 'worker') if component == 'vermeer' else ('hubble',))
    for name in names:
        pidfile = RUN / (f'vermeer/{name}.pid' if name in ('master', 'worker') else f'{name}.pid')
        if not pidfile.exists():
            continue
        pid = int(pidfile.read_text())
        proc = Path(f'/proc/{pid}/cmdline')
        expected = b'vermeer-bin' if name in ('master', 'worker') else b'org.apache.hugegraph.HugeGraphHubble'
        if proc.exists() and expected in proc.read_bytes():
            os.kill(pid, signal.SIGTERM)
            deadline = time.monotonic() + 15
            while proc.exists() and proc.read_bytes() and time.monotonic() < deadline:
                time.sleep(0.2)
            if proc.exists() and proc.read_bytes():
                raise RuntimeError(f'{name} did not stop; inspect PID {pid}')
        pidfile.unlink()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'build', 'start', 'smoke', 'stop'])
    parser.add_argument('--component', choices=['all', 'vermeer', 'hubble'], default='all')
    args = parser.parse_args()
    if args.action in ('build', 'stop'):
        globals()[args.action](args.component)
    else:
        globals()[args.action]()
