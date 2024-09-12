/**
 * @file  FullScreen 全屏
 * @author
 */

import React, {useCallback, useContext, useEffect} from 'react';
import {Button, Tooltip} from 'antd';
import {CompressOutlined} from '@ant-design/icons';
import screenfull from 'screenfull';
import {GraphContext} from '../Context';

const FullScreen = props => {
    const {onChange} = props;
    const {graph} = useContext(GraphContext);
    const handleFullScreenState = useCallback(
        () => {
            onChange();
        },
        [onChange]
    );

    useEffect(
        () => {
            document.addEventListener('fullscreenchange', handleFullScreenState, false);
            return () => {
                document.removeEventListener('fullscreenchange', handleFullScreenState, false);
            };
        },
        [handleFullScreenState]
    );

    const handleFullScreen = useCallback(
        () => {
            const container = graph?.getContainer();
            if (screenfull.isEnabled) {
                if (screenfull.isFullscreen) {
                    screenfull.exit();
                }
                else {
                    screenfull.request(container);
                }
            }
        },
        [graph]
    );

    return (
        <Tooltip title="全屏" placement='bottom'>
            <Button type="text" onClick={handleFullScreen} icon={<CompressOutlined />} />
        </Tooltip>
    );
};

export default FullScreen;