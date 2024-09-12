/**
 * @file LayoutConfig
 * @author gouzixing@
 */

import React, {useCallback} from 'react';
import {Button, Tooltip} from 'antd';
import {DeploymentUnitOutlined} from '@ant-design/icons';

const LayoutConfig = props => {
    const {
        buttonEnable,
        onClick,
        tooltip,
    } = props;

    const handleClickLayout = useCallback(
        () => {
            onClick();
        },
        [onClick]
    );

    return (
        <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
            <Button
                onClick={handleClickLayout}
                icon={<DeploymentUnitOutlined />}
                type={'text'}
                disabled={!buttonEnable}
            >
                布局方式
            </Button>
        </Tooltip>
    );
};

export default LayoutConfig;
