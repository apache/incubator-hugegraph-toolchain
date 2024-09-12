/**
 * @file  FitCenter 自适应
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {OneToOneOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';
import {fitView} from '../../../utils/graph';

const FitCenter = () => {
    const {graph} = useContext(GraphContext);

    const handleFitCenter = useCallback(
        () => {
            if (graph) {
                graph.fitCenter();
                fitView(graph);
            }
        },
        [graph]
    );

    return (
        <Tooltip title="自适应" placement='bottom'>
            <Button type="text" onClick={handleFitCenter} icon={<OneToOneOutlined />} />
        </Tooltip>
    );
};

export default FitCenter;