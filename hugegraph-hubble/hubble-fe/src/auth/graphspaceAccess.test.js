/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {resolveGraphspaceAccess} from './graphspaceAccess';

test.each([
    [null, true, false, false],
    [{mode: 'PD', role: 'USER', scopes: {}}, true, false, false],
    [{
        mode: 'PD',
        role: 'USER',
        scopes: {write_graphspaces: ['space']},
    }, true, false, true],
    [{
        mode: 'PD',
        role: 'SPACEADMIN',
        scopes: {admin_graphspaces: ['space']},
    }, true, true, true],
    [{mode: 'NON_PD', role: 'USER', scopes: {}}, false, false, true],
    [{mode: 'NON_PD', role: 'SUPERADMIN', scopes: {}}, false, true, true],
    [{
        mode: 'NON_AUTH',
        role: 'ANONYMOUS',
        scopes: {all_graphspaces: true},
    }, true, false, true],
    [{mode: 'NON_AUTH', role: 'ANONYMOUS', scopes: {}}, false, true, true],
])(
    'resolves graphspace access for %#',
    (context, pdEnabled, canManage, canWrite) => {
        expect(resolveGraphspaceAccess(context, 'space', pdEnabled))
            .toEqual({canManage, canWrite});
    }
);
