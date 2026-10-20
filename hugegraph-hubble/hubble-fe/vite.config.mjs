/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {fileURLToPath} from 'node:url';
import {defineConfig, loadEnv, transformWithOxc} from 'vite';
import react from '@vitejs/plugin-react';
import {createDevProxy} from './scripts/dev-proxy.cjs';

const root = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig(({mode}) => ({
    root,
    plugins: [
        {
            name: 'hubble-javascript-jsx',
            enforce: 'pre',
            async transform(code, id) {
                if (id.startsWith(root) && /\/src\/.*\.js(?:\?.*)?$/.test(id)) {
                    return transformWithOxc(code, id, {
                        lang: 'jsx',
                        jsx: {runtime: 'automatic', development: mode !== 'production'},
                    });
                }
            },
        },
        react(),
    ],
    optimizeDeps: {
        rolldownOptions: {moduleTypes: {'.js': 'jsx'}},
    },
    server: {
        host: '127.0.0.1',
        port: 3000,
        strictPort: true,
        proxy: createDevProxy({...loadEnv(mode, root, 'HUBBLE_'), ...process.env}),
    },
    build: {
        outDir: 'build',
        emptyOutDir: true,
        sourcemap: false,
        manifest: true,
        rolldownOptions: {input: {
            main: fileURLToPath(new URL('./index.html', import.meta.url)),
            visualization: fileURLToPath(new URL('./visualization.html', import.meta.url)),
        }},
    },
}));
