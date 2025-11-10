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
const intergeMessage = '请输入整数';
const rangeVanlidatorMessage = '请输入-1或大于0的整数';
const maxDepthMessage = '请输入(0, 5000]范围的整数';
const maxLimitValidatorMessage = '请输入(0, 800000）范围的整数';
const positiveIntegerValidatorMessage = '请输入大于等于0的整数';
const integerValidatorMessage = '请输入整数';
const positiveNumberMessage = '请输入大于0的整数';
const skipDegreeRangeMessage = '请输入[0,10000000]的整数';
const groupPropertyRangeMessage = '请输入大于2的整数';
const numberMessage = '请输入大于等于0的整数';
const alphaRangeMessage = '请输入(0,1]的数';
const topValidatorMessage = '请输入(0,1000)范围的数';
const maxDiffVanlidatorMessage = '请输入(0,1)范围的数';
const maxDepthValidatorMessage = '请输入[2,50]的整数';

export const maxDegreeValidator = (rule, value) => {
    if (_.isNumber(value)) {
        if (value <= 0 && value !== -1) {
            return Promise.reject(new Error(rangeVanlidatorMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};

export const maxDepthValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 0 || value > 5000) {
            return Promise.reject(new Error(maxDepthMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};

export const maxDegreeValidatorForCrossPoint = (rule, value) => {
    if (!_.isNull(value)) {
        if (value <= 0 || value >= 800000) {
            return Promise.reject(new Error(maxLimitValidatorMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(maxLimitValidatorMessage));
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
            return Promise.reject(new Error('请按照key=value的格式换行输入'));
        }
        const valueLength = value.length;
        if (!(valueLength > 2 && value[0] === '\'' && value[valueLength - 1] === '\'')
            && isNaN(+value)
        ) {
            return Promise.reject(new Error('字符串value请用单括号\'\'包围'));
        }
    }
    return Promise.resolve();
};

export const integerValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value < 0) {
            return Promise.reject(new Error(positiveIntegerValidatorMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(integerValidatorMessage));
        }
    }
    return Promise.resolve();
};

export const positiveIntegerValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 0) {
            return Promise.reject(new Error(positiveNumberMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};


export const skipDegreeValidator = (_, value) => {
    if (value !== null) {
        if (value < 0 || value > 10000000) {
            return Promise.reject(new Error(skipDegreeRangeMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};

export const groupPropertyValidator = (rule, value) => {
    if (!_.isNull(value) && !_.isUndefined(value)) {
        if (value <= 2) {
            return Promise.reject(new Error(groupPropertyRangeMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};

export const includeZeroNumberValidator = (rule, value) => {
    if (!_.isNull(value)) {
        if (value < 0) {
            return Promise.reject(new Error(numberMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
        }
    }
    return Promise.resolve();
};

export const alphaValidator =  (rule, value) => {
    if (!_.isNull(value)) {
        if (value <= 0 || value > 1) {
            return Promise.reject(new Error(alphaRangeMessage));
        }
    }
    return Promise.resolve();
};


export const topValidator = (_, value) => {
    if (value !== null) {
        if (value <= 0 || value >= 1000) {
            return Promise.reject(new Error(topValidatorMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(topValidatorMessage));
        }
    }
    return Promise.resolve();
};


export const maxDiffValidator = (rule, value) => {
    if (_.isNumber(value) && (value <= 0 || value >= 1)) {
        return Promise.reject(new Error(maxDiffVanlidatorMessage));
    }
    return Promise.resolve();
};

export const maxDepthForRankValidator = (rule, value) => {
    if (value !== null) {
        if (value < 2 || value > 50) {
            return Promise.reject(new Error(maxDepthValidatorMessage));
        }
        if (value % 1 !== 0) {
            return Promise.reject(new Error(maxDepthValidatorMessage));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneValidator = (rule, value) => {
    if (value !== null) {
        if (value <= 0 || value >= 1) {
            return Promise.reject(new Error('请输入(0,1)范围的值'));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneContainsValidator = (rule, value) => {
    if (value !== null) {
        if (value < 0 || value > 1) {
            return Promise.reject(new Error('请输入[0,1]范围的值'));
        }
    }
    return Promise.resolve();
};


export const greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 2000 || value < 1 || value % 1 !== 0) {
            return Promise.reject(new Error('请输入1到2000范围的整数'));
        }
    }
    return Promise.resolve();
};

export const greaterThanOneAndLowerThanOneHundredThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 1 || value % 1 !== 0) {
            return Promise.reject(new Error('请输入1到100000范围的值'));
        }
    }
    return Promise.resolve();
};

export const greaterThanZeroAndLowerThanOneHundredThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 0 || value % 1 !== 0) {
            return Promise.reject(new Error('请输入[0, 100000]范围的整数'));
        }
    }
    return Promise.resolve();
};

export const greaterThanOneAndLowerThanTenThousandIntegerValidator = (rule, value) => {
    if (value !== null) {
        if (value > 100000 || value < 1 && value % 1 !== 0) {
            return Promise.reject(new Error('请输入1-10000之间的整数值'));
        }
    }
    return Promise.resolve();
};

export const limitValidator = (rule, value) => {
    if (value !== null) {
        if (value <= 0 && value !== -1) {
            return Promise.reject(new Error(rangeVanlidatorMessage));
        }
        else if (value % 1 !== 0) {
            return Promise.reject(new Error(intergeMessage));
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
