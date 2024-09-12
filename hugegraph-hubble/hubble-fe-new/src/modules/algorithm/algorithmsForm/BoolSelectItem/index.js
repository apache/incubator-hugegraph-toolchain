import React from 'react';

import {Form, Select} from 'antd';

const boolOptions = [
    {label: '是', value: true},
    {label: '否', value: false},
];

const BoolSelectItem = props => {
    const {
        name,
        initialValue = false,
        isRequired = false,
        desc,
    } = props;

    return (
        <Form.Item
            label={name}
            name={name}
            initialValue={initialValue}
            tooltip={desc}
            rules={[{required: isRequired}]}
        >
            <Select options={boolOptions} allowClear={!isRequired} />
        </Form.Item>
    );
};

export default BoolSelectItem;