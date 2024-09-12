/**
 * @file  ClearGraph 清空画布
 * @author
 */

import React, {useCallback, useContext, useState} from 'react';
import {Button, Tooltip, Modal} from 'antd';
import {CopyrightOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const ClearGraph = props => {
    const {enable, onChange} = props;
    const {graph} = useContext(GraphContext);
    const [clearModalVisible, setClearModalVisible] = useState(false);

    const handleClear = useCallback(
        () => {
            setClearModalVisible(true);
        },
        []
    );

    const handleClearModalOk = useCallback(
        () => {
            graph?.clear();
            setClearModalVisible(false);
            onChange();
        },
        [graph, onChange]
    );

    const handleClearModalCancel = useCallback(
        () => {
            setClearModalVisible(false);
        },
        []
    );

    return (
        <>
            <Tooltip title="清空画布" placement='bottom'>
                <Button disabled={!enable} type="text" onClick={handleClear} icon={<CopyrightOutlined />} />
            </Tooltip>
            <Modal
                width={600}
                title="是否清除当前画布"
                open={clearModalVisible}
                onOk={handleClearModalOk}
                onCancel={handleClearModalCancel}
                okText="确认"
                cancelText="取消"
            >
                <div>清除当前画布，此次清除操作后不可以恢复</div>
            </Modal>
        </>
    );
};

export default ClearGraph;
