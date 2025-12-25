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

import {Select} from 'antd';
import Style from './index.module.scss';

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

const Colorbox = ({color, border}) => {
    return <div className={Style.colorbox} style={{background: color, borderColor: border ? '#eee' : '#fff'}} />;
};

const SelectColorbox = ({value = '', onChange}) => {

    const triggerChange = changedValue => {
        onChange?.(changedValue);
    };

    return (
        <Select
            style={{width: 67}}
            dropdownMatchSelectWidth={false}
            popupClassName={Style.colorgroup}
            onChange={triggerChange}
            value={value}
        >
            {colorSchemas.map(item => {
                return (
                    <Select.Option key={item} value={item}>
                        <Colorbox color={item} />
                    </Select.Option>
                );
            })}
        </Select>
    );
};

export {Colorbox, SelectColorbox};
