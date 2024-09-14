/**
 * @file DirectionItem
 * @author gouzixing@
 */

import React from 'react';
import {Form, Select} from 'antd';
import {useTranslation} from 'react-i18next';

const DirectionItem = props => {
    const {t} = useTranslation();
    const {desc} = props;
    const directionOptions = [
        {label: t('ERView.edge.out'), value: 'OUT'},
        {label: t('ERView.edge.in'), value: 'IN'},
        {label: t('ERView.edge.both'), value: 'BOTH'},
    ];

    return (
        <Form.Item
            label='direction'
            name='direction'
            initialValue='BOTH'
            tooltip={desc}
        >
            <Select
                placeholder={t('analysis.algorithm.direction_item.tooltip')}
                allowClear
                options={directionOptions}
            />
        </Form.Item>
    );
};

export default DirectionItem;
