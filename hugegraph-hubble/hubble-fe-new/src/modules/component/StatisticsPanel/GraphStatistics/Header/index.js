/**
 * @file  图统计标题头
 * @author gouzixing
 */

import React from 'react';
import {Button, Tooltip} from 'antd';
import {QuestionCircleOutlined} from '@ant-design/icons';
import c from './index.module.scss';

const GraphStatisticsHeader = props => {

    const {name, description, highlightFunc, hideFunc, highlightFuncDisable, hideFuncDisable} = props;

    return (
        <div className={c.graphStatisticsHeader}>
            <div>
                <span className={c.name}>{name}</span>
                <Tooltip placement="right" title={description}><QuestionCircleOutlined /></Tooltip>
            </div>
            <div className={c.buttom}>
                <Button type="text" size='small' onClick={highlightFunc} disabled={highlightFuncDisable}>高亮</Button>
                <Button type="text" size='small' onClick={hideFunc} disabled={hideFuncDisable}>隐藏</Button>
            </div>
        </div>
    );
};

export default GraphStatisticsHeader;