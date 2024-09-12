/**
 * @file 外观设置
 * @author
 */

import React, {useCallback, useState} from 'react';
import {Button, Tooltip} from 'antd';
import ConfigModal from '../ConfigModal';
import {ExperimentOutlined} from '@ant-design/icons';

const StyleConfig = props => {
    const {
        styleConfig,
        onChange,
        buttonEnable,
        refreshExcuteCount,
        tooltip,
    } = props;

    const [isModalVisible, setModalVisible] = useState(false);

    const handleClickStyleConfig = useCallback(
        () => {
            setModalVisible(true);
        },
        []
    );

    const handleStyleModalOk = useCallback(
        styleConfig => {
            onChange(styleConfig);
            setModalVisible(false);
            refreshExcuteCount && refreshExcuteCount();
        },
        [onChange, refreshExcuteCount]
    );

    const handleStyleModalCancel = useCallback(
        () => {
            setModalVisible(false);
        },
        []
    );

    return (
        <>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    onClick={handleClickStyleConfig}
                    icon={<ExperimentOutlined />}
                    type={'text'}
                    disabled={!buttonEnable}
                >
                    外观设置
                </Button>
            </Tooltip>
            <ConfigModal
                style={styleConfig}
                visible={isModalVisible}
                onOk={handleStyleModalOk}
                onCancel={handleStyleModalCancel}
            />
        </>
    );
};

export default StyleConfig;
