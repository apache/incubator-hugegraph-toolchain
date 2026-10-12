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

const DELETE_TABLES = ['Property', 'Vertex', 'Edge'];

test.each(DELETE_TABLES)('%s deletion owns errors without exposing raw responses', table => {
    const source = fs.readFileSync(path.join(__dirname, table, 'index.js'), 'utf8');

    expect(source).not.toContain('message.error(res.message)');
    expect(source).toContain("message.error(t('schema.delete_failed'))");
    expect(source).toContain('suppressBusinessErrorToast: true');
    expect(source).toContain('.catch(() =>');
});
