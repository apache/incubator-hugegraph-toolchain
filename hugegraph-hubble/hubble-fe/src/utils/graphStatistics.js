/*
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

// Counts can be unavailable while the graph itself is ready. Preserve the
// statistics metadata when adapting the API names for shared canvas state.
const toGraphCounts = data => {
    const {vertexcount = null, edgecount = null, ...metadata} = data || {};
    return {vertexCount: vertexcount, edgeCount: edgecount, ...metadata};
};

const isKnownCount = value => value != null && String(value).trim() !== ''
    && Number.isFinite(Number(value)) && Number(value) >= 0;

const isKnownZeroCount = value => isKnownCount(value) && Number(value) === 0;

export {toGraphCounts, isKnownCount, isKnownZeroCount};
