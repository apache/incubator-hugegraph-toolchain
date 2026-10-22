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

// Source health includes collection quality; a collection failure alone does
// not establish that the nodes reported by the source are down.
export const sourceHealthStatus = (source = {}) => {
    const {status = 'UNKNOWN', availability = 'UNSUPPORTED', stale = false} = source;
    if (status === 'DOWN') {
        return 'DOWN';
    }
    if (status === 'DEGRADED') {
        return 'DEGRADED';
    }
    if (availability === 'UNSUPPORTED') {
        return 'UNKNOWN';
    }
    if (availability === 'PARTIAL' || stale) {
        return 'DEGRADED';
    }
    if (status !== 'UP') {
        return 'UNKNOWN';
    }
    return availability === 'AVAILABLE' ? 'UP' : 'DEGRADED';
};
