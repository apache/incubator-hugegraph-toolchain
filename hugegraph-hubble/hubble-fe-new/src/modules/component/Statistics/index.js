/**
 * @file Menubar统计按钮
 * @author gouzixing
 */

import React, {useCallback} from 'react';
import {Button, Tooltip} from 'antd';
import {AreaChartOutlined} from '@ant-design/icons';

const Statistics = props => {

    const {buttonEnable, onClick, tooltip} = props;

    const handleClickStatistics = useCallback(() => {
        onClick();
    }, [onClick]);

    return (
        <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
            <Button
                onClick={handleClickStatistics}
                icon={<AreaChartOutlined />}
                type={'text'}
                disabled={!buttonEnable}
            >
                统计
            </Button>
        </Tooltip>
    );
};

export default Statistics;