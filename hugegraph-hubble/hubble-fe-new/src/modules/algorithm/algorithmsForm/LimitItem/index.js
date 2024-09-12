/**
 * @file LimitItem
 * @author
 */

import React from 'react';
import {Form, InputNumber} from 'antd';
import {limitValidator} from '../utils';

const LimitItem = props => {
    const {initialValue, desc} = props;

    return (
        <Form.Item
            label='limit'
            name='limit'
            initialValue={initialValue}
            rules={[{validator: limitValidator}]}
            tooltip={desc}
        >
            <InputNumber />
        </Form.Item>
    );
};

export default LimitItem;