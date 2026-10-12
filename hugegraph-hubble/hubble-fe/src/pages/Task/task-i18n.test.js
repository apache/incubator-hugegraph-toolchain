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

test('Task UI does not contain hard-coded Chinese copy', () => {
    const directories = [__dirname, path.join(__dirname, '..', 'TaskDetail')];
    const offenders = directories.flatMap(collectJavaScript).flatMap(file => {
        const lines = fs.readFileSync(file, 'utf8').split('\n');
        return lines.flatMap((line, index) => {
            return /[\u3400-\u9fff]/u.test(line)
                ? [`${path.relative(path.join(__dirname, '..'), file)}:${index + 1}`]
                : [];
        });
    });

    expect(offenders).toEqual([]);
});

test('Task translations are symmetric and English values contain no Chinese copy', () => {
    const enEntries = flatten(enPages.task);
    const zhEntries = flatten(zhPages.task);
    const enKeys = enEntries.map(([key]) => key);
    const zhKeys = zhEntries.map(([key]) => key);

    expect(enKeys.filter(key => !zhKeys.includes(key))).toEqual([]);
    expect(zhKeys.filter(key => !enKeys.includes(key))).toEqual([]);
    expect(enEntries.filter(([, value]) => /[\u3400-\u9fff]/u.test(value))).toEqual([]);
});

test('Task status labels localize common Loader backend states', () => {
    expect(zhPages.task.status).toEqual({
        pending: '待执行',
        running: '运行中',
        success: '已完成',
        failed: '失败',
        paused: '已暂停',
        stopped: '已停止',
        initializing: '初始化中',
        cancelling: '正在取消',
        cancelled: '已取消',
        unknown: '未知状态',
    });
    expect(enPages.task.status).toEqual({
        pending: 'Pending',
        running: 'Running',
        success: 'Completed',
        failed: 'Failed',
        paused: 'Paused',
        stopped: 'Stopped',
        initializing: 'Initializing',
        cancelling: 'Cancelling',
        cancelled: 'Cancelled',
        unknown: 'Unknown',
    });
});
