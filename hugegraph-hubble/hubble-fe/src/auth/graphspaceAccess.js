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

import {useAuthContext} from './AuthContext';
import {isPdEnabled} from '../utils/config';

const includes = (values, target) => Array.isArray(values)
    && values.includes(target);

const resolveGraphspaceAccess = (context, graphspace, pdEnabled) => {
    if (!context) {
        return {canManage: false, canWrite: false};
    }

    const scopes = context.scopes ?? {};
    const anonymousStandalone = context.mode === 'NON_AUTH' && !pdEnabled;
    const anonymousPd = context.mode === 'NON_AUTH' && pdEnabled;
    const canManage = anonymousStandalone
        || (!anonymousPd && (
            context.role === 'SUPERADMIN'
            || scopes.all_graphspaces === true
            || includes(scopes.admin_graphspaces, graphspace)
        ));
    const canWrite = canManage
        || anonymousPd
        || context.mode === 'NON_PD'
        || includes(scopes.write_graphspaces, graphspace);
    return {canManage, canWrite};
};

const useGraphspaceAccess = graphspace => {
    const {context} = useAuthContext();
    return resolveGraphspaceAccess(context, graphspace, isPdEnabled());
};

export {resolveGraphspaceAccess, useGraphspaceAccess};
