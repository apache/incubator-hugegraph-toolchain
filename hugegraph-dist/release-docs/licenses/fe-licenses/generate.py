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

# Requires an existing frozen pnpm install. No network fetch or package mutation.
import hashlib
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[4]
FRONTEND = ROOT / 'hugegraph-hubble/hubble-fe'
DESTINATION = pathlib.Path(__file__).resolve().parent
LICENSE_NAME = re.compile(
    r'^(?:licen[cs]e(?:[._-].*)?|copying(?:[._-].*)?|notice(?:[._-].*)?'
    r'|copyright(?:notice)?(?:\.(?:txt|md|rst))?)$', re.I)
README_LICENSES = {'agent-base@6.0.2', 'https-proxy-agent@5.0.1', 'is-mobile@5.0.0'}
UPSTREAM = {
    '@ant-design/icons-svg@4.6.0': {
        'url': 'https://raw.githubusercontent.com/ant-design/ant-design-icons/'
               '7f2516ac91226d2b41f93b35cb5197c8d94f7189/LICENSE',
        'file': DESTINATION / 'upstream/ant-design-icons-svg-4.6.0-LICENSE.txt',
    },
}
PUBLISHING_COMMITS = {
    '@antv/algorithm@0.1.26': '58b99ab9c937b8ec3e5e99b57624ad5a007682e5',
    '@antv/event-emitter@0.1.3': 'f46ff7712f332ef1c8b7601eb88b5784ad4edaa1',
    'cron-expression-validator@1.0.20': 'a796b1e1a28846194e1397c93713d5a3e88527dd',
}
DISCREPANCIES = {'@antv/util@2.0.17', 'cron-expression-validator@1.0.20'}


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def installed_packages():
    expected_manager = json.loads((FRONTEND / 'package.json').read_text())['packageManager']
    # Corepack selects the pinned manager before pnpm processes its arguments.
    actual_manager = subprocess.check_output([
        'pnpm', '--version'], cwd=FRONTEND).decode().strip()
    if expected_manager != 'pnpm@' + actual_manager:
        raise ValueError(f'Use the pinned package manager: {expected_manager}')
    source_lock = (FRONTEND / 'pnpm-lock.yaml').read_bytes()
    installed_lock = (FRONTEND / 'node_modules/.pnpm/lock.yaml').read_bytes()
    # pnpm 12 stores package-manager dependencies in an initial YAML document;
    # the installed lock contains the final production/development document.
    package_lock = re.split(rb'(?m)^---\r?\n', source_lock)[-1]
    if package_lock != installed_lock:
        raise ValueError('Installed lock differs from source; run a frozen pnpm install first')
    tree = json.loads(subprocess.check_output([
        'pnpm', 'list', '--prod', '--depth', 'Infinity', '--json'], cwd=FRONTEND))[0]
    packages = {}

    def visit(item):
        for name, package in item.get('dependencies', {}).items():
            key = f"{name}@{package['version']}"
            if key in packages:
                first = pathlib.Path(packages[key]['path']) / 'package.json'
                other = pathlib.Path(package['path']) / 'package.json'
                if first.read_bytes() != other.read_bytes():
                    raise ValueError(f'Different installed manifests for {key}')
            else:
                packages[key] = {'name': name, **package}
            visit(package)

    visit(tree)
    return packages


def license_texts(key, directory):
    # rglob does not traverse directory symlinks. Dependency node_modules trees
    # belong to separate package entries, not this package's vendored notices.
    files = sorted(file for file in directory.rglob('*')
                   if file.is_file() and not file.is_symlink()
                   and 'node_modules' not in file.relative_to(directory).parts
                   and LICENSE_NAME.fullmatch(file.name))
    texts = [(str(file.relative_to(directory)), file.read_bytes(), 'installed package')
             for file in files]
    if not texts and key in README_LICENSES:
        raw = (directory / 'README.md').read_bytes()
        heading = b'License\n-------' if b'License\n-------' in raw else b'## License'
        start = raw.index(heading)
        stop = raw.find(b'\n[', start)
        data = raw[start:stop if stop != -1 else len(raw)]
        if b'Permission is hereby granted' not in data:
            raise ValueError(f'Missing full README license grant for {key}')
        texts.append(('README.md (License section)', data, 'installed package'))
    if not texts and key in UPSTREAM:
        source = UPSTREAM[key]
        texts.append(('LICENSE', source['file'].read_bytes(), source['url']))
    return texts


def generate():
    entries = []
    blocks = []
    for key, package in sorted(installed_packages().items()):
        directory = pathlib.Path(package['path'])
        manifest_bytes = (directory / 'package.json').read_bytes()
        manifest = json.loads(manifest_bytes)
        if manifest['version'] != package['version']:
            raise ValueError(f'Installed version does not match pnpm tree: {key}')
        entry = {
            'name': package['name'], 'version': package['version'],
            'declaredLicense': manifest.get('license'),
            'packageJsonSha256': sha256(manifest_bytes), 'texts': [],
            'repository': manifest.get('repository'),
        }
        texts = license_texts(key, directory)
        if not texts:
            entry.update({
                'status': 'upstream-license-text-required',
                'publishingGitHead': PUBLISHING_COMMITS.get(key),
                'evidence': 'Published package has no LICENSE/COPYING/NOTICE or full README license grant.',
            })
            if key in DISCREPANCIES:
                entry['discrepancy'] = 'package.json declares ISC; README declares MIT.'
            body = (f"\n{'=' * 72}\n{key}\nDeclared package license: {entry['declaredLicense']}\n"
                    f"UNRESOLVED: {entry['evidence']}\n")
            if 'discrepancy' in entry:
                body += entry['discrepancy'] + '\n'
            blocks.append(body.encode())
        else:
            entry['status'] = 'text-preserved'
            for source, data, origin in texts:
                entry['texts'].append({'source': source, 'origin': origin,
                                       'sha256': sha256(data), 'bytes': len(data)})
                prefix = (f"\n{'=' * 72}\n{key}\nDeclared package license: {entry['declaredLicense']}\n"
                          f"Source: {source}\nOrigin: {origin}\nSHA-256: {sha256(data)}\n{'-' * 72}\n")
                blocks.append(prefix.encode() + data + b'\n')
        entries.append(entry)

    patch = 'hugegraph-hubble/hubble-fe/patches/@antv__g6@5.1.1.patch'
    result = {
        'schema': 1,
        'scope': 'pnpm list --prod --depth Infinity; conservative production dependency closure, '
                 'including modules pruned by bundling',
        'lockfile': 'hugegraph-hubble/hubble-fe/pnpm-lock.yaml',
        'lockfileSha256': sha256((FRONTEND / 'pnpm-lock.yaml').read_bytes()),
        'packageManager': json.loads((FRONTEND / 'package.json').read_text())['packageManager'],
        'packageCount': len(entries),
        'localPatches': [{'package': '@antv/g6@5.1.1', 'path': patch,
                          'sha256': sha256((ROOT / patch).read_bytes())}],
        'packages': entries,
    }
    header = (b'Hubble frontend third-party license texts\n\n'
              b'This file preserves license/notice bytes from the locked production dependency\n'
              b'closure, including nested vendored notices. Package metadata is descriptive and\n'
              b'does not replace the original grants. Unresolved upstream texts are marked below\n'
              b'and tracked in README.md; this inventory is not release legal approval.\n')
    (DESTINATION / 'THIRD-PARTY.txt').write_bytes(header + b''.join(blocks))
    (DESTINATION / 'inventory.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps({
        'packages': len(entries), 'textCount': sum(len(entry['texts']) for entry in entries),
        'unresolved': [entry['name'] + '@' + entry['version'] for entry in entries if not entry['texts']],
        'bytes': (DESTINATION / 'THIRD-PARTY.txt').stat().st_size,
        'lock': result['lockfileSha256'],
    }, indent=2))


if __name__ == '__main__':
    generate()
