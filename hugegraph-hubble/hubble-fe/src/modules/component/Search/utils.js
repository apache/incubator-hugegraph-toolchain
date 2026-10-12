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

export const BOOLEAN_TRUE = 'boolean_true';
export const BOOLEAN_FALSE = 'boolean_false';

export const getRuleOptions = ruleType => {
    switch (ruleType.toLowerCase()) {
        case 'float':
        case 'double':
        case 'byte':
        case 'int':
        case 'long':
        case 'date':
            return ['gt', 'gte', 'lt', 'lte', 'eq'];
        case 'object':
        case 'text':
        case 'blob':
        case 'uuid':
            return ['eq'];
        case 'boolean':
            return [BOOLEAN_TRUE, BOOLEAN_FALSE];
        default:
            return [];
    }
};

export const getRuleOptionLabelKey = option => {
    if (option === BOOLEAN_TRUE) {
        return 'true';
    }

    if (option === BOOLEAN_FALSE) {
        return 'false';
    }

    return option;
};

const isEmptyValue = value => value === undefined || value === null || value === '';

export const normalizeFilterCondition = condition => {
    if (!condition?.key || !condition?.operator) {
        return null;
    }

    if (condition.operator === BOOLEAN_TRUE) {
        return {
            key: condition.key,
            operator: 'eq',
            value: true,
        };
    }

    if (condition.operator === BOOLEAN_FALSE) {
        return {
            key: condition.key,
            operator: 'eq',
            value: false,
        };
    }

    if (isEmptyValue(condition.value)) {
        return null;
    }

    let normalizedValue = condition.value;

    if (condition.value?._isAMomentObject) {
        normalizedValue = condition.value.format('YYYY-MM-DD HH:mm:ss');
    }

    return {
        key: condition.key,
        operator: condition.operator,
        value: normalizedValue,
    };
};
