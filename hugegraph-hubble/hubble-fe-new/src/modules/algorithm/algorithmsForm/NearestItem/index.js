/**
 * @file NearestItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Select} from 'antd';
import {useTranslation} from 'react-i18next';

const NearestItem = props => {
    const {t} = useTranslation();
    const boolOptions = [
        {label: t('common.verify.yes'), value: true},
        {label: t('common.verify.no'), value: false},
    ];
    return (
        <Form.Item
            label='nearest'
            name='nearest'
            initialValue
            tooltip={t('analysis.algorithm.nearest_item.tooltip')}
        >
            <Select placeholder={t('analysis.algorithm.nearest_item.placeholder')} allowClear options={boolOptions} />
        </Form.Item>
    );
};

export default NearestItem;
