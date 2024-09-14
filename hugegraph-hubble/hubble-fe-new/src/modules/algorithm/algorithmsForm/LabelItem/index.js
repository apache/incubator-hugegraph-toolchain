/**
 * @file LabelItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Input} from 'antd';
import {useTranslation} from 'react-i18next';

const LabelItem = props => {
    const {t} = useTranslation();
    return (
        <Form.Item
            label='label'
            name='label'
            tooltip={t('analysis.algorithm.label_item.tooltip')}
        >
            <Input />
        </Form.Item>
    );
};

export default LabelItem;
