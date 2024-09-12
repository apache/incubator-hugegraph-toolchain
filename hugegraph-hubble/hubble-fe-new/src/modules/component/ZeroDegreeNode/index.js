/**
 * @file  孤立点检测
 * @author
 */

import React, {useCallback, useContext, useState} from 'react';
import {Button, Tooltip} from 'antd';
import {UngroupOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const ZeroDegreeNodeSearch = props => {
    const {graph} = useContext(GraphContext);

    const [isolatedNodesMode, setIsolatedNodesMode] = useState(false);

    const handleSearchZeroDegreeNodes = useCallback(
        () => {
            graph.getNodes().forEach(item => {
                const {degree} = graph.getNodeDegree(item, 'all', true);
                if (degree !== 0) {
                    if (!isolatedNodesMode) {
                        graph.hideItem(item, false);
                    }
                    else {
                        graph.showItem(item, false);
                    }
                }
            });
            graph.refresh();
            setIsolatedNodesMode(pre => !pre);
        },
        [graph, isolatedNodesMode]
    );

    return (
        <Tooltip title="孤立点" placement='bottom'>
            <Button type="text" onClick={handleSearchZeroDegreeNodes} icon={<UngroupOutlined />} />
        </Tooltip>
    );
};

export default ZeroDegreeNodeSearch;