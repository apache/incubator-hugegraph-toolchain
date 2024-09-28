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

import SelectedSolidArrowIcon from '../../../assets/ic_arrow_selected.svg';
import NoSelectedSolidArrowIcon from '../../../assets/ic_arrow.svg';
import SelectedSolidStraightIcon from '../../../assets/ic_straight_selected.svg';
import NoSelectedSolidStraightIcon from '../../../assets/ic_straight.svg';

const colorSchemas = [
    '#5c73e6',
    '#569380',
    '#8ecc93',
    '#fe9227',
    '#fe5b5d',
    '#fd6ace',
    '#4d8dda',
    '#57c7e3',
    '#ffe081',
    '#c570ff',
    '#2b65ff',
    '#0eb880',
    '#76c100',
    '#ed7600',
    '#e65055',
    '#a64ee6',
    '#108cee',
    '#00b5d9',
    '#f2ca00',
    '#e048ae',
];

const vertexSizeSchemas = [
    {label: '超小', value: 'TINY'},
    {label: '小', value: 'SMALL'},
    {label: '中', value: 'NORMAL'},
    {label: '大', value: 'BIG'},
    {label: '超大', value: 'HUGE'},
];

const edgeSizeSchemas = [
    {label: '粗', value: 'THICK'},
    {label: '中', value: 'NORMAL'},
    {label: '细', value: 'FINE'},
];

const idOptions = [
    {label: '主键ID', value: 'PRIMARY_KEY'},
    {label: '自动生成', value: 'AUTOMATIC'},
    {label: '自定义字符串', value: 'CUSTOMIZE_STRING'},
    {label: '自定义数字', value: 'CUSTOMIZE_NUMBER'},
    {label: '自定义UUID', value: 'CUSTOMIZE_UUID'},
];

const dataTypeOptions = [
    'string',
    'boolean',
    'byte',
    'int',
    'long',
    'float',
    'double',
    'date',
    'uuid',
    'blob',
].map(item => ({label: item, value: item === 'string' ? 'TEXT' : item.toUpperCase()}));

const cardinalityOptions = ['single', 'list', 'set'].map(item => ({label: item, value: item.toUpperCase()}));

const edgeShapeSchemas = [
    {
        blackicon: NoSelectedSolidArrowIcon,
        blueicon: SelectedSolidArrowIcon,
        flag: true,
        shape: 'solid',
    },
    {
        blackicon: NoSelectedSolidStraightIcon,
        blueicon: SelectedSolidStraightIcon,
        flag: false,
        shape: 'solid',
    },
];

const attrOptions = [
    {label: '非空', value: false},
    {label: '允许空', value: true},
];

const indexTypeOptions = [
    {label: '二级索引', value: 'SECONDARY'},
    {label: '范围索引', value: 'RANGE'},
    {label: '全文索引', value: 'SEARCH'},
    {label: 'SHARD', value: 'SHARD'},
    {label: '唯一索引', value: 'UNIQUE'},
];

export {
    colorSchemas,
    indexTypeOptions,
    edgeSizeSchemas,
    idOptions,
    vertexSizeSchemas,
    dataTypeOptions,
    cardinalityOptions,
    edgeShapeSchemas,
    attrOptions,
};
