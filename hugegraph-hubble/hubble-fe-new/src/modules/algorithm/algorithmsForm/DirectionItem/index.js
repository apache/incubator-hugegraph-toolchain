/**
 * @file DirectionItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Select} from 'antd';

const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const DirectionItem = props => {

    const {desc} = props;

    return (
        <Form.Item
            label='direction'
            name='direction'
            initialValue='BOTH'
            tooltip={desc}
        >
            <Select
                placeholder="顶点向外发散的方向"
                allowClear
                options={directionOptions}
            />
        </Form.Item>
    );
};

export default DirectionItem;