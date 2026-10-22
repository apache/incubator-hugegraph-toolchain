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

"""Keep Corepack's package-manager selection inside the frontend project."""
import importlib.util
import json
import os
import pathlib
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('frontend_notices', pathlib.Path(__file__).with_name('generate.py'))
generator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(generator)


class GeneratorTest(unittest.TestCase):
    def test_package_manager_runs_from_frontend_when_generator_is_called_from_repository_root(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            frontend = root / 'hubble-fe'
            frontend.mkdir()
            (root / 'package.json').write_text(json.dumps({'packageManager': 'pnpm@12.15.0'}))
            (frontend / 'package.json').write_text(json.dumps({'packageManager': 'pnpm@12.14.0'}))
            lock = b'lockfileVersion: 9.0\n'
            (frontend / 'pnpm-lock.yaml').write_bytes(lock)
            installed = frontend / 'node_modules/.pnpm'
            installed.mkdir(parents=True)
            (installed / 'lock.yaml').write_bytes(lock)
            binaries = root / 'bin'
            binaries.mkdir()
            launcher = binaries / 'pnpm'
            # Model Corepack resolving packageManager before pnpm handles --dir.
            launcher.write_text("""#!/usr/bin/env python3
import json
import pathlib
import sys
project = pathlib.Path.cwd()
while not (project / 'package.json').exists():
    project = project.parent
manager = json.loads((project / 'package.json').read_text())['packageManager']
if '--version' in sys.argv:
    print(manager.removeprefix('pnpm@'))
else:
    assert project.name == 'hubble-fe', 'Dependency enumeration escaped frontend cwd'
    assert sys.argv[1:] == ['list', '--prod', '--depth', 'Infinity', '--json']
    print('[{"dependencies": {"sample": {"version": "1.0.0", "path": "/sample"}}}]')
""")
            launcher.chmod(0o755)
            previous = pathlib.Path.cwd()
            try:
                os.chdir(root)
                with patch.object(generator, 'FRONTEND', frontend), patch.dict(
                        os.environ, {'PATH': str(binaries) + os.pathsep + os.environ['PATH']}):
                    self.assertEqual(generator.installed_packages()['sample@1.0.0']['name'], 'sample')
            finally:
                os.chdir(previous)


if __name__ == '__main__':
    unittest.main()
