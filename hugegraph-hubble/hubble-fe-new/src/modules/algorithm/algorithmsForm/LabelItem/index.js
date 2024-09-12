/**
 * @file LabelItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Input} from 'antd';

const LabelItem = props => {
    return (
        <Form.Item
            label='label'
            name='label'
            tooltip="边的类型（默认代表所有edge label）"
        >
            <Input />
        </Form.Item>
    );
};

export default LabelItem;