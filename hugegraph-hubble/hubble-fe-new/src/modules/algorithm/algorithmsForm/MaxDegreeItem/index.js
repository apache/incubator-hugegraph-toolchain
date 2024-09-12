/**
 * @file MaxDegreeItem
 * @author
 */

import React from 'react';
import {Form, InputNumber} from 'antd';

const MaxDegreeItem = props => {

    const {isRequired, initialValue, validator} = props;

    return (
        <Form.Item
            label='max_degree'
            name='max_degree'
            initialValue={initialValue}
            rules={[{required: isRequired}, {validator: validator}]}
            tooltip={'查询过程中，单个顶点遍历的最大邻接边数目'}
        >
            <InputNumber />
        </Form.Item>
    );
};

export default MaxDegreeItem;