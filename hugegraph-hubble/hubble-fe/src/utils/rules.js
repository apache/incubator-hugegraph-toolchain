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

import validator from 'validator';
import cronValidator from 'cron-expression-validator';

// 必填项
const required = msg => ({
    required: true,
    message: msg ?? undefined,
});

const max = max => ({
    max,
    message: `最大长度为${max}个字符!`,
});

// ip验证
const isIP = () => ({
    validator(_, value) {
        if (validator.isIP(value)) {
            return Promise.resolve();
        }

        return Promise.reject(new Error('不是合法的IP'));
    },
});

// 端口验证
const isPort = () => ({
    validator(_, value) {
        if (validator.isPort(value)) {
            return Promise.resolve();
        }

        return Promise.reject(new Error('不是合法的端口'));
    },
});

// cron 验证
const isCron = () => ({
    validator(_, value) {
        if (cronValidator.isValidCronExpression(value)) {
            return Promise.resolve();
        }
        return Promise.reject(new Error('不是合法的quartz格式'));
    },
});

// 名称 验证
const isName = () => ({
    validator(_, value) {
        let res = /^[a-z][a-z0-9_]*$/.test(value);
        if (!res) {
            return Promise.reject('以字母开头,只能包含小写字母、数字、_');
        }

        return Promise.resolve();
    },
});

// 中文，字母，_
const isCNName = () => ({
    validator(_, value) {
        let res = /[^\u4E00-\u9FA5\uFE30-\uFFA0\_a-zA-Z]+/.test(value);
        if (res) {
            return Promise.reject('只能包含中文、字母、_');
        }

        return Promise.resolve();
    },
});

// 中文，字母，数字，_
const isPropertyName = () => ({
    validator(_, value) {
        let res = /[^\u4E00-\u9FA5\uFE30-\uFFA0\_a-zA-Z0-9]+/.test(value);
        if (res) {
            return Promise.reject('只能包含中文、字母、数字、_');
        }

        return Promise.resolve();
    },
});

// 中文，字母，数字，_
const isNoramlName = () => ({
    validator(_, value) {
        let res = /^[\u4E00-\u9FA5\uFE30-\uFFA0\_a-zA-Z0-9_]{1,20}$/.test(value);
        if (!res) {
            return Promise.reject('只能包含中文、字母、数字、_, 不能超过20个字符');
        }

        return Promise.resolve();
    },
});

// jdbc
const isJDBC = () => ({
    validator(_, value) {
        let res = /jdbc:(\w+:){1,2}(\/\/)?(.+):(\d+)/.test(value);
        if (!res) {
            return Promise.reject('请输入正确的jdbc url, 例如：jdbc:mysql://127.0.0.1:3306/db_name');
        }

        return Promise.resolve();
    },
});

// account name
const isAccountName = () => ({
    validator(_, value) {
        let res = /^[\u4E00-\u9FA5\uFE30-\uFFA0\_a-zA-Z0-9_]{1,16}$/.test(value);
        let res1 = /^_.*/.test(value);
        let res2 = /.*_$/.test(value);

        if (!res || res1 || res2) {
            return Promise.reject('账号名不超过16个字符，且不能以下划线开始和结尾');
        }

        return Promise.resolve();
    },
});

// uuid验证
const isUUID = () => ({
    validator(_, value) {
        if (!value || validator.isUUID(value)) {
            return Promise.resolve();
        }

        return Promise.reject('非法的数据格式');
    },
});

// int验证
const isInt = () => ({
    validator(_, value) {
        if (!value || validator.isInt(value)) {
            return Promise.resolve();
        }

        return Promise.reject('非法的数据格式');
    },
});

export {
    required,
    max,
    isIP,
    isPort,
    isCron,
    isName,
    isCNName,
    isPropertyName,
    isNoramlName,
    isJDBC,
    isAccountName,
    isUUID,
    isInt,
};
