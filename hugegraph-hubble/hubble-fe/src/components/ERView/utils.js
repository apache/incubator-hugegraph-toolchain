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

import {useTranslation} from 'react-i18next';

const setPropertyRow = item => {
    return {...item, attr: {...item.attr, rect: {fill: '#FAFAFA'}}};
};

const setValueRow = item => {
    return {...item, attr: {...item.attr, rect: {fill: '#ACACAC'}}};
};

const setCell = (t, cell, type) => {
    const idList = [];
    if (type === 'edge') {
        idList.push(
            {
                id: `${cell.name}-source`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: t('ERView.edge.start'),
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );

        idList.push(
            {
                id: `${cell.name}-target`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: t('ERView.edge.end'),
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );
    }
    else {
        idList.push(
            {
                id: `${cell.name}-ID`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: 'ID',
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );
    }

    const propertyList = cell.properties.map(item => ({
        id: `${cell.name}-${item.name}`,
        group: 'list',
        attrs: {
            portNameLabel: {
                text: 'Property',
            },
            portTypeLabel: {
                text: item.name,
            },
        },
    }));

    return {
        id: cell.name,
        shape: 'er-rect',
        label: cell.name,
        width: 150,
        height: 24,
        position: {
            x: 4,
            y: 150,
        },
        ports: [
            ...idList,
            ...propertyList,
        ],
    };
};

export {setPropertyRow, setValueRow, setCell};
