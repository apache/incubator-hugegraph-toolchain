/**
 * @file NearestItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Select} from 'antd';

const boolOptions = [
    {label: '是', value: true},
    {label: '否', value: false},
];

const NearestItem = props => {
    return (
        <Form.Item
            label='nearest'
            name='nearest'
            initialValue
            tooltip={`nearest为true时，代表起始顶点到达结果顶点的最短路径长度为depth，不存在更短的路径；near
            est为false时，代表起始顶点到结果顶点有一条长度为depth的路径（未必最短且可以有环）`}
        >
            <Select placeholder="最短路径长度" allowClear options={boolOptions} />
        </Form.Item>
    );
};

export default NearestItem;