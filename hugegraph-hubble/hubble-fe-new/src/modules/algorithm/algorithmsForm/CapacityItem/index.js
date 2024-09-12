/**
 * @file CapacityItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, InputNumber} from 'antd';
import {integerValidator} from '../utils';

const CapacityItem = props => {
    return (
        <Form.Item
            label='capacity'
            name='capacity'
            initialValue='10000000'
            rules={[{validator: integerValidator}]}
            tooltip="遍历过程中最大的访问的顶点数目"
        >
            <InputNumber />
        </Form.Item>
    );
};
export default CapacityItem;