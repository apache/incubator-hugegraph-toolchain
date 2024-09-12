/**
 * @file  ZoomGraph 放大缩小
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {ZoomOutOutlined, ZoomInOutlined} from '@ant-design/icons';
import {ToolBarContext} from '../Context';

const ZoomGraph = () => {
    const toolBarInstance = useContext(ToolBarContext);

    const handleZoomIn = useCallback(
        () => {
            toolBarInstance.zoomIn();
        },
        [toolBarInstance]
    );

    const handleZoomOut = useCallback(
        () => {
            toolBarInstance.zoomOut();
        },
        [toolBarInstance]
    );

    return (
        <>
            <Tooltip title="缩小" placement='bottom'>
                <Button type="text" onClick={handleZoomIn} icon={<ZoomOutOutlined />} />
            </Tooltip>
            <Tooltip title="放大" placement='bottom'>
                <Button type="text" onClick={handleZoomOut} icon={<ZoomInOutlined />} />
            </Tooltip>
        </>
    );
};

export default ZoomGraph;