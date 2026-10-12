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

import fs from 'fs';
import path from 'path';

const component = fs.readFileSync(path.join(__dirname, 'index.js'), 'utf8');
const stylesheet = fs.readFileSync(path.join(__dirname, 'index.module.scss'), 'utf8');

const widthConfig = component.match(
    /const TASK_COLUMN_WIDTHS = \{([\s\S]*?)\};/
)?.[1];
const widths = Object.fromEntries(
    [...(widthConfig?.matchAll(/(\w+):\s*(\d+)/g) ?? [])]
        .map(([, key, value]) => [key, Number(value)])
);

test('declares bounded task column widths and nowrap overflow behavior', () => {
    // This is a static source contract. It does not prove that every column is
    // visible at 1280px; viewport visibility requires browser evidence.
    expect(component).toContain('className={style.task_table}');
    expect(component).toContain("scroll={{x: 'max-content'}}");
    expect(component).not.toContain("fixed: 'right'");
    expect(component).toContain('const hasCreator = data.some');
    expect(component).toContain('...(hasCreator ? [{');
    expect(component.match(/className: style\.no_wrap/g)?.length).toBeGreaterThanOrEqual(8);
    expect(component.match(/ellipsis: true/g)?.length).toBeGreaterThanOrEqual(7);
    expect(Object.keys(widths)).toEqual([
        'name',
        'source',
        'graphspace',
        'graph',
        'created',
        'creator',
        'status',
        'sync',
        'actions',
    ]);
    const declaredPrimaryWidth = widths.name + widths.source + widths.graphspace
        + widths.graph + widths.created + widths.status + widths.sync;
    expect(declaredPrimaryWidth).toBeLessThanOrEqual(780);
    expect(widths.actions).toBeGreaterThanOrEqual(180);
    expect(widths.actions).toBeLessThanOrEqual(200);
    expect(declaredPrimaryWidth + widths.actions).toBeLessThanOrEqual(950);
    expect(declaredPrimaryWidth + widths.creator + widths.actions)
        .toBeLessThanOrEqual(1020);
    expect(component).toMatch(
        /title: t\('graphspace\.col\.operation'\),\s*align: 'center',\s*width: TASK_COLUMN_WIDTHS\.actions/
    );
    expect(stylesheet).toMatch(
        /\.task_table\s*\{[\s\S]*\.ant-table-cell[\s\S]*white-space:\s*nowrap/
    );
    expect(stylesheet).toMatch(
        /\.no_wrap\s*\{[\s\S]*text-overflow:\s*ellipsis/
    );
});
