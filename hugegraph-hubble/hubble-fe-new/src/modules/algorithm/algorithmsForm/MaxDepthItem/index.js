/**
 * @file MaxDepthItem
 * @author
 */

import React from 'react';
import {Form, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';

const MaxDepthItem = props => {
    const {t} = useTranslation();
    const {validator} = props;

    return (
        <Form.Item
            label='max_depth'
            name='max_depth'
            rules={[{required: true}, {validator: validator}]}
            tooltip={t('analysis.algorithm.max_depth_item.tooltip')}
        >
            <InputNumber />
        </Form.Item>
    );
};

export default MaxDepthItem;
