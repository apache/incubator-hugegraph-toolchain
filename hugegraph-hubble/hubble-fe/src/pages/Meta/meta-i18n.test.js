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

test('Meta UI does not contain hard-coded Chinese copy', () => {
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
