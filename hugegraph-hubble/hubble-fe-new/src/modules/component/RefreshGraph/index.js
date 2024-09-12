/**
 * @file  RefreshGraph 刷新布局
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {SyncOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const RefreshGraph = () => {
    const {graph} = useContext(GraphContext);

    const handleRefreshGraph = useCallback(
        () => {
            const {layout} = graph.cfg;
            if (layout) {
                graph.destroyLayout();
                graph.updateLayout(layout, 'center', undefined, false);
            }
        },
        [graph]
    );

    return (
        <Tooltip title="刷新布局" placement='bottom'>
            <Button type="text" onClick={handleRefreshGraph} icon={<SyncOutlined />} />
        </Tooltip>
    );
};

export default RefreshGraph;
