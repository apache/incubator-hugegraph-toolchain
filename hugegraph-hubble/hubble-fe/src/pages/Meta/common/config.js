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
import i18n from '../../../i18n';

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
    {label: i18n.t('schema.options.size_tiny'), value: 'TINY'},
    {label: i18n.t('schema.options.size_small'), value: 'SMALL'},
    {label: i18n.t('schema.options.size_normal'), value: 'NORMAL'},
    {label: i18n.t('schema.options.size_big'), value: 'BIG'},
    {label: i18n.t('schema.options.size_huge'), value: 'HUGE'},
];

const edgeSizeSchemas = [
    {label: i18n.t('schema.options.line_thick'), value: 'THICK'},
    {label: i18n.t('schema.options.size_normal'), value: 'NORMAL'},
    {label: i18n.t('schema.options.line_fine'), value: 'FINE'},
];

const idOptions = [
    {label: i18n.t('schema.options.id_primary'), value: 'PRIMARY_KEY'},
    {label: i18n.t('schema.options.id_auto'), value: 'AUTOMATIC'},
    {label: i18n.t('schema.options.id_string'), value: 'CUSTOMIZE_STRING'},
    {label: i18n.t('schema.options.id_number'), value: 'CUSTOMIZE_NUMBER'},
    {label: i18n.t('schema.options.id_uuid'), value: 'CUSTOMIZE_UUID'},
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
    {label: i18n.t('schema.options.required'), value: false},
    {label: i18n.t('schema.options.nullable'), value: true},
];

const indexTypeOptions = [
    {label: i18n.t('schema.options.index_secondary'), value: 'SECONDARY'},
    {label: i18n.t('schema.options.index_range'), value: 'RANGE'},
    {label: i18n.t('schema.options.index_search'), value: 'SEARCH'},
    {label: 'SHARD', value: 'SHARD'},
    {label: i18n.t('schema.options.index_unique'), value: 'UNIQUE'},
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
