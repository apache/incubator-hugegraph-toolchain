/**
 * @file CapacityItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, InputNumber} from 'antd';
import {integerValidator} from '../utils';
import {useTranslation} from 'react-i18next';

const CapacityItem = props => {
    const {t} = useTranslation();
    return (
        <Form.Item
            label='capacity'
            name='capacity'
            initialValue='10000000'
            rules={[{validator: integerValidator}]}
            tooltip={t('analysis.algorithm.capacity_item.tooltip')}
        >
            <InputNumber />
        </Form.Item>
    );
};
export default CapacityItem;
