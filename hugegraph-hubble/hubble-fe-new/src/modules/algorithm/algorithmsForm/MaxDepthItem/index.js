/**
 * @file MaxDepthItem
 * @author
 */

import React from 'react';
import {Form, InputNumber} from 'antd';

const MaxDepthItem = props => {

    const {validator} = props;

    return (
        <Form.Item
            label='max_depth'
            name='max_depth'
            rules={[{required: true}, {validator: validator}]}
            tooltip="步数"
        >
            <InputNumber />
        </Form.Item>
    );
};

export default MaxDepthItem;