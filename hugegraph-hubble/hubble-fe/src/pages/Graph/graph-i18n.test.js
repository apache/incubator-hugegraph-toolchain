/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fs from 'fs';
import path from 'path';
import enPages from '../../i18n/resources/en-US/modules/pages.json';
import zhPages from '../../i18n/resources/zh-CN/modules/pages.json';

const collectJavaScript = directory => fs.readdirSync(directory, {withFileTypes: true})
    .flatMap(entry => {
        const entryPath = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            return collectJavaScript(entryPath);
        }
        return entry.name.endsWith('.js') && !entry.name.endsWith('.test.js') ? [entryPath] : [];
    });

const flatten = (value, prefix = '') => Object.entries(value).flatMap(([key, child]) => {
    const current = `${prefix}${key}`;
    return child && typeof child === 'object'
        ? flatten(child, `${current}.`)
        : [[current, child]];
});

test('Graph UI does not contain hard-coded Chinese copy', () => {
    const offenders = collectJavaScript(__dirname).flatMap(file => {
        const lines = fs.readFileSync(file, 'utf8').split('\n');
        return lines.flatMap((line, index) => {
            return /[\u3400-\u9fff]/u.test(line)
                ? [`${path.relative(__dirname, file)}:${index + 1}`]
                : [];
        });
    });

    expect(offenders).toEqual([]);
});

test('Graph translations are symmetric and English values contain no Chinese copy', () => {
    const enEntries = flatten(enPages.graph);
    const zhEntries = flatten(zhPages.graph);
    const enKeys = enEntries.map(([key]) => key);
    const zhKeys = zhEntries.map(([key]) => key);

    expect(enKeys.filter(key => !zhKeys.includes(key))).toEqual([]);
    expect(zhKeys.filter(key => !enKeys.includes(key))).toEqual([]);
    expect(enEntries.filter(([, value]) => /[\u3400-\u9fff]/u.test(value))).toEqual([]);
});
