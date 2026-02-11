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

import _ from 'lodash';

export default function removeNilKeys(obj) {
    if (_.isString(obj)) {
        return sanitizeString(obj);
    }
    if (_.isArray(obj)) {
        return sanitizeArray(obj);
    }
    if (_.isPlainObject(obj)) {
        return sanitizeObject(obj);
    }
    return obj;
}

function isProvided(value) {
    const typeIsNotSupported = !_.isNil(value) && !_.isString(value)
    && !_.isArray(value) && !_.isPlainObject(value);
    return typeIsNotSupported || !_.isEmpty(value);
}

function transToNumber(val) {
    const matchVal = val.match(/\`(.*)\`/);

    return matchVal ? Number(matchVal[1]) : val;
}

function sanitizeString(str) {
    return _.isEmpty(str) ? null : transToNumber(str);
}

function sanitizeArray(arr) {
    return _.filter(_.map(arr, removeNilKeys), isProvided);
}

function sanitizeObject(obj) {
    return _.pickBy(_.mapValues(obj, removeNilKeys), isProvided);
}

