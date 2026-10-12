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
import i18n from '../../../../i18n';

const validationMessage = key => i18n.t(`analysis.algorithm.validation.${key}`);

export const maxDegreeValidator = (rule, value) => {
    if (_.isNumber(value)) {
        if (value <= 0 && value !== -1) {
            return Promise.reject(new Error(validationMessage('range_or_minus_one')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const maxDepthValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 0 || value > 5000) {
            return Promise.reject(new Error(validationMessage('max_depth_range')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const maxDegreeValidatorForCrossPoint = (rule, value) => {
    if (!_.isNull(value)) {
        if (value <= 0 || value >= 800000) {
            return Promise.reject(new Error(validationMessage('max_limit_range')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('max_limit_range')));
        }
    }
    return Promise.resolve();
};

export const propertiesValidator = (rule, value) => {
    if (!value) {
        return Promise.resolve();
    }
    const propertiesArr = value.trim().split('\n') || [];
    for (const item of propertiesArr) {
        const [key, value] = item?.split('=');
        if (!key || !value) {
            return Promise.reject(new Error(validationMessage('properties_format')));
        }
        const valueLength = value.length;
        if (!(valueLength > 2 && value[0] === '\'' && value[valueLength - 1] === '\'')
            && isNaN(+value)
        ) {
            return Promise.reject(new Error(validationMessage('string_value_quote')));
        }
    }
    return Promise.resolve();
};

export const integerValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value < 0) {
            return Promise.reject(new Error(validationMessage('non_negative_integer')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const positiveIntegerValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 0) {
            return Promise.reject(new Error(validationMessage('positive_integer')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};


export const skipDegreeValidator = (_, value) => {
    if (value !== null) {
        if (value < 0 || value > 10000000) {
            return Promise.reject(new Error(validationMessage('skip_degree_range')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const groupPropertyValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 2) {
            return Promise.reject(new Error(validationMessage('group_property_range')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const includeZeroNumberValidator = (rule, value) => {
    if (!_.isNull(value)) {
        if (value < 0) {
            return Promise.reject(new Error(validationMessage('non_negative_integer')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

export const alphaValidator =  (rule, value) => {
    if (!_.isNull(value)) {
        if (value <= 0 || value > 1) {
            return Promise.reject(new Error(validationMessage('alpha_range')));
        }
    }
    return Promise.resolve();
};


export const topValidator = (_, value) => {
    if (value !== null) {
        if (value <= 0 || value >= 1000) {
            return Promise.reject(new Error(validationMessage('top_range')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('top_range')));
        }
    }
    return Promise.resolve();
};


export const maxDiffValidator = (rule, value) => {
    if (_.isNumber(value) && (value <= 0 || value >= 1)) {
        return Promise.reject(new Error(validationMessage('open_unit_range')));
    }
    return Promise.resolve();
};

export const maxDepthForRankValidator = (rule, value) => {
    if (value !== null) {
        if (value < 2 || value > 50) {
            return Promise.reject(new Error(validationMessage('depth_2_50')));
        }
        if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('depth_2_50')));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneValidator = (rule, value) => {
    if (value !== null) {
        if (value <= 0 || value >= 1) {
            return Promise.reject(new Error(validationMessage('open_unit_value')));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneContainsValidator = (rule, value) => {
    if (value !== null) {
        if (value < 0 || value > 1) {
            return Promise.reject(new Error(validationMessage('closed_unit_value')));
        }
    }
    return Promise.resolve();
};


export const greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 2000 || value < 1 || value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer_1_2000')));
        }
    }
    return Promise.resolve();
};

export const greaterThanOneAndLowerThanOneHundredThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 1 || value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('value_1_100000')));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneHundredThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 0 || value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer_0_100000')));
        }
    }
    return Promise.resolve();
};

export const greaterThanOneAndLowerThanTenThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 1 && value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer_1_10000')));
        }
    }
    return Promise.resolve();
};

export const limitValidator = (rule, value) => {
    if (value !== null) {
        if (value <= 0 && value !== -1) {
            return Promise.reject(new Error(validationMessage('range_or_minus_one')));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(validationMessage('integer')));
        }
    }
    return Promise.resolve();
};

// 用于转化Properties格式, 由字符串key1=value1 \n key2=value2 转换为{key1: value1, key2: value2}格式;
export const formatPropertiesValue = properties => {
    const propertiesArr = properties?.trim().split('\n') || [];
    const propertiesValue = {};
    for (const item of propertiesArr) {
        const [key, value] = item?.split('=');
        if (key && value) {
            const valueLength = value.length;
            if (valueLength > 2 && value[0] === '\'' && value[valueLength - 1] === '\'') {
                propertiesValue[key] = value.slice(1, valueLength - 1);
            }
            else if (!isNaN(+value)) {
                propertiesValue[key] = +value;
            }
        }
    }
    return propertiesValue;
};

export const formatVerticesValue = value => {
    const {ids, label, properties} = value;
    const result = {
        ids: ids && ids.split(','),
        label,
        properties: formatPropertiesValue(properties),
    };
    return result;
};
