/**
 * @file  FixNode 固定节点
 * @author
 */

import React, {useCallback, useContext, useState, useEffect} from 'react';
import {Button, Tooltip} from 'antd';
import {PushpinOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const FixNode = () => {
    const {graph} = useContext(GraphContext);
    const [fixState, setFixState] = useState(false);
    const [selectedNode, setSelectedNode] = useState();


    useEffect(() => {
        graph?.on('node:click', evt => {
            const {item} = evt;
            setFixState(true);
            setSelectedNode(item);
        });

        graph?.on('edge:click', evt => {
            setSelectedNode();
            setFixState(false);
        });

        graph?.on('canvas:click', evt => {
            setSelectedNode();
            setFixState(false);
        });
    },
    [graph]);

    const handleFixNode = useCallback(
        () => {
            const node = selectedNode || undefined;
            const hasLocked = node.hasLocked();
            if (hasLocked) {
                node.unlock();
                graph.clearItemStates(node, ['customFixed', 'customSelected']);
                graph.pushStack('unlock', node.getModel(), 'undo');
            }
            else {
                node.lock();
                graph?.clearItemStates(node, ['customSelected']);
                graph?.setItemState(node, 'customFixed', true);
                graph?.pushStack('lock', node.getModel(), 'undo');
            }
        },
        [graph, selectedNode]
    );

    return (
        <Tooltip title="固定" placement='bottom'>
            <Button disabled={!fixState} type="text" onClick={handleFixNode} icon={<PushpinOutlined />} />
        </Tooltip>
    );
};

export default FixNode;