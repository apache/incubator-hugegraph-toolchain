import React from 'react';

import {Form, Select} from 'antd';
import {useTranslation} from 'react-i18next';

const BoolSelectItem = props => {
    const {t} = useTranslation();
    const boolOptions = [
        {label: t('common.verify.yes'), value: true},
        {label: t('common.verify.no'), value: false},
    ];
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
