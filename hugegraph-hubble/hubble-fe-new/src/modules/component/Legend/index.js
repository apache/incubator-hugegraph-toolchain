/**
 * @file  Legend
 * @author
 */

import {useContext, useEffect, useRef, useCallback} from 'react';

import {GraphContext} from '../Context';
import CustomLegend from './customLegend';

const Legend = props => {
    const context = useContext(GraphContext);
    const legendRef = useRef(null);
    const {graph} = context;
    const {data} = props;

    const filterFunctions = useCallback(
        legendData => {
            let filter = {};
            legendData?.nodes.forEach(node => {
                const key = node.id;
                filter[key] = d => d.legendType === node.id;
            });
            legendData?.edges.forEach(edge => {
                const key = edge.id;
                filter[key] = d => d.legendType === edge.id;
            });
            return filter;
        },
        []
    );

    useEffect(() => {
        const filter = filterFunctions(data);
        const legendOptions = {
            data: data,
            layout: 'vertical',
            title: '图例',
            width: 1000,
            align: 'left',
            containerStyle: {
                fill: '#fff',
                lineWidth: '0',
            },
            titleConfig: {
                position: 'left',
                offsetX: 0,
                offsetY: 0,
                style: {
                    textAlign: 'end',
                    fontSize: 12,
                    fontWeight: 'bold',
                },
            },
            position: 'top-left',
            offsetY: 0,
            filter: {
                enable: true,
                multiple: true,
                trigger: 'click',
                graphActiveState: 'activeByLegend',
                graphInactiveState: 'inactiveByLegend',
                filterFunctions: filter,
            },
        };
        const legendInstance = new CustomLegend(legendOptions);
        legendRef.current = legendInstance;
        graph?.addPlugin(legendInstance);
        return () => {
            if (graph && !graph.destroyed) {
                graph.removePlugin(legendInstance);
            }
        };
    },
    [data, filterFunctions, graph]);

};

export default Legend;