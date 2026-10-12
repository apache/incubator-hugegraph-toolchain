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

const collectJavaScript = directory => fs.readdirSync(directory, {withFileTypes: true})
    .flatMap(entry => {
        const entryPath = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            return collectJavaScript(entryPath);
        }
        return entry.name.endsWith('.js') && !entry.name.endsWith('.test.js') ? [entryPath] : [];
    });

test('algorithm component props are not forwarded to Collapse panel DOM', () => {
    const offenders = collectJavaScript(__dirname).flatMap(file => {
        const source = fs.readFileSync(file, 'utf8');
        return /<Collapse\.Panel[\s\S]*?\{\.\.\.props\}/u.test(source)
            ? [path.relative(__dirname, file)]
            : [];
    });

    expect(offenders).toEqual([]);
});

test('algorithm panels forward only the collapse control contract', () => {
    const offenders = collectJavaScript(__dirname).flatMap(file => {
        const source = fs.readFileSync(file, 'utf8');
        if (!source.includes('<Collapse.Panel')) {
            return [];
        }
        const hasContract = source.includes('isActive={props.isActive}')
            && source.includes('onItemClick={props.onItemClick}')
            && source.includes('panelKey={props.panelKey}');
        return hasContract ? [] : [path.relative(__dirname, file)];
    });

    expect(offenders).toEqual([]);
});

test('max_depth defaults stay optional', () => {
    const offenders = collectJavaScript(__dirname).flatMap(file => {
        const source = fs.readFileSync(file, 'utf8');
        const fields = source.match(
            /name=['"]max_depth['"][\s\S]*?<\/Form\.Item>/gu
        ) ?? [];
        return fields.some(field => field.includes('required: true'))
            ? [path.relative(__dirname, file)]
            : [];
    });

    expect(offenders).toEqual([]);
});
