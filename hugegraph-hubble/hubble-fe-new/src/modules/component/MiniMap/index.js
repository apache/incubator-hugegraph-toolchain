/**
 * @file  miniMap
 * @author
 */

import {useContext, useEffect} from 'react';
import G6 from '@antv/g6';
import {GraphContext} from '../Context';
const MiniMap = () => {
    const {graph} = useContext(GraphContext);

    useEffect(
        () => {
            const minimap = new G6.Minimap();
            if (graph && !graph.destroyed) {
                graph.addPlugin(minimap);
            }
            return () => {
                if (graph && !graph.destroyed) {
                    graph.removePlugin(minimap);
                }
            };
        },
        [graph]
    );
    return null;
};
export default MiniMap;