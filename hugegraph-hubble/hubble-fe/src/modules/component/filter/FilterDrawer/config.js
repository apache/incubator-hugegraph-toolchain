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

// powershell语法
const ruleOptions = [
    {value: 'gt', label: '>'},
    {value: 'gte', label: '>='},
    {value: 'lt', label: '<'},
    {value: 'lte', label: '<='},
    {value: 'eq', label: '='},
    {value: 'neq', label: '!='},
    {value: 'ltlt', label: '< value <'},
    {value: 'ltelt', label: '<= value <'},
    {value: 'ltlte', label: '< value <='},
    {value: 'ltelte', label: '<= value <='},
    {value: 'contains', label: 'contains'},
    {value: 'notcontains', label: 'not contains'},
];

const propertyDateOption = [
    {value: 'eq', label: '所选当天'},
    {value: 'lt', label: '之前（不包含所选日期）'},
    {value: 'gt', label: '之后（不包含所选日期）'},
    {value: 'ltelte', label: '时间段'},
];

const typeToOption = {
    'TEXT': ['eq', 'neq', 'contains', 'notcontains'],
    'DATE': ['gt', 'gte', 'lt', 'lte', 'ltlt', 'ltlte', 'ltelt', 'ltelte', 'eq', 'neq'],
    'INT': ['gt', 'gte', 'lt', 'lte', 'ltlt', 'ltlte', 'ltelt', 'ltelte', 'eq', 'neq'],
    'BOOLEAN': ['neq', 'eq'],
};

export {
    ruleOptions, propertyDateOption, typeToOption,
};
