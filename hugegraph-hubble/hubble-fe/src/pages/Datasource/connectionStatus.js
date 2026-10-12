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

const LOCAL_JDBC_UNSUPPORTED_MESSAGE = 'JDBC datasource check is not supported locally';

export const resolveJdbcConnectionStatus = (res, t) => {
    const messageOf = status => t(`datasource.form.connection_${status}`);

    if (res?.status === 200) {
        const rawResult = `${res.data?.result || ''}`.trim();
        const normalized = rawResult.toLowerCase();

        if (normalized === 'success' || normalized === 'failed') {
            return {
                type: normalized,
                message: messageOf(normalized),
            };
        }

        return {
            type: 'failed',
            message: rawResult || messageOf('failed'),
        };
    }

    return {
        type: 'failed',
        message: res?.message === LOCAL_JDBC_UNSUPPORTED_MESSAGE
            ? messageOf('unsupported')
            : (res?.message || messageOf('unsupported')),
    };
};
