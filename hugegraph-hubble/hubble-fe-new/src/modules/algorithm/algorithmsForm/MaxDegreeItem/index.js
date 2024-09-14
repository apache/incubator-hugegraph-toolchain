/**
 * @file MaxDegreeItem
 * @author
 */

import React from 'react';
import {Form, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';

const MaxDegreeItem = props => {
    const {t} = useTranslation();
    const {isRequired, initialValue, validator} = props;

    return (
        <Form.Item
            label='max_degree'
            name='max_degree'
            initialValue={initialValue}
            rules={[{required: isRequired}, {validator: validator}]}
            tooltip={t('analysis.algorithm.max_degree_item.tooltip')}
        >
            <InputNumber />
        </Form.Item>
    );
};

export default MaxDegreeItem;
