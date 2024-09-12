/**
 * @file SettingConfig
 * @author
 */

import React, {useCallback} from 'react';
import {Button, Tooltip} from 'antd';
import {SettingOutlined} from '@ant-design/icons';

const SettingConfig = props => {
    const {
        buttonEnable,
        onClick,
        tooltip,
    } = props;

    const handleClickSetting = useCallback(
        () => {
            onClick();
        },
        [onClick]
    );

    return (
        <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
            <Button
                onClick={handleClickSetting}
                icon={<SettingOutlined />}
                type={'text'}
                disabled={!buttonEnable}
            >
                设置
            </Button>
        </Tooltip>
    );
};

export default SettingConfig;