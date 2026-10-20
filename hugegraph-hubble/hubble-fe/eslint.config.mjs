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

import js from '@eslint/js';
import globals from 'globals';
import react from '@eslint-react/eslint-plugin';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

export default [
    {ignores: ['build/**', 'node_modules/**', 'node/**', 'coverage/**']},
    {
        files: ['src/**/*.{js,jsx,ts,tsx}'],
        ...js.configs.recommended,
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module',
            globals: {...globals.browser, ...globals.es2022},
            parserOptions: {ecmaFeatures: {jsx: true}},
        },
        plugins: {...react.configs.recommended.plugins, 'react-hooks': reactHooks},
        settings: react.configs.recommended.settings,
        rules: {
            ...js.configs.recommended.rules,
            ...react.configs.recommended.rules,
            // Keep correctness checks for the maintained runtime. These
            // optional React Compiler/naming suggestions do not establish a
            // defect in the existing JS business flows.
            '@eslint-react/set-state-in-effect': 'off',
            '@eslint-react/use-state': 'off',
            '@eslint-react/no-use-context': 'off',
            '@eslint-react/no-context-provider': 'off',
            '@eslint-react/naming-convention-ref-name': 'off',
            '@eslint-react/no-unnecessary-use-prefix': 'off',
            '@eslint-react/no-clone-element': 'off',
            'react-hooks/rules-of-hooks': 'error',
            'react-hooks/exhaustive-deps': 'error',
            'no-unused-vars': ['error', {argsIgnorePattern: '^_', varsIgnorePattern: '^_'}],
        },
    },
    ...tseslint.configs.recommended.map(config => ({
        ...config,
        files: ['src/**/*.{ts,tsx}'],
    })),
    {
        files: ['src/**/*.{ts,tsx}'],
        rules: {'@typescript-eslint/no-unused-vars': ['error', {argsIgnorePattern: '^_', varsIgnorePattern: '^_'}]},
    },
    {
        files: ['src/**/*.test.{js,jsx,ts,tsx}', 'src/setupTests.js'],
        languageOptions: {globals: {...globals.jest, ...globals.node}},
    },
];
