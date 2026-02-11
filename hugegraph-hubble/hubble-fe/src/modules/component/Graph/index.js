/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * @file 画布公共组件
 * @author gouzixing@
 */

import React, {useEffect, useState, useRef, useMemo} from 'react';
import G6 from '@antv/g6';
import '@antv/graphin-icons/dist/index.css';
import _ from 'lodash';
import {GraphContext} from '../Context';
import ResizeObserver from 'resize-observer-polyfill';
import classnames from 'classnames';
import {fitView, mapLayoutNameToLayoutDetails} from '../../../utils/graph';
import {
    clearEdgesStates,
    clearItemStates,
    clearSelectedStates,
    highLightRelatedEdges,
    setItemLabelState,
} from '../../../utils/handleGraphState';
import c from './index.module.scss';
import './index.css';
import useCustomNode from '../../../customHook/useCustomNode';
import useCustomGrid from '../../../customHook/useCustomGrid';
import useCustomEdge from '../../../customHook/useCustomEdge';

const Graph = (props, ref) => {
    const {
        data,
        layout: layoutOptions,
        onGraphRender,
        isPanelEnable,
        onNodeClick,
        onEdgeClick,
        onNodedbClick,
    } = props;

    useCustomGrid();
    useCustomNode();
    useCustomEdge();

    const container = useRef(null);
    const graph = useRef(null);

    const [layout, setLayout] = useState();
    const [context, setContext] = useState({
        graph: graph.current,
    });

    const throttledContainerResize = useMemo(
        () => {
            return _.throttle((width, height) => {
                if (graph.current) {
                    graph.current.changeSize(width, height);
                    fitView(graph.current);
                }
            }, 500);
        },
        []
    );

    const graphClassName = classnames(
        c.graph,
        {[c.layoutPanelOpen]: isPanelEnable}
    );

    useEffect(
        () => {
            const resizeObserver = new ResizeObserver(entries => {
                for (let entry of entries) {
                    const {width, height} = entry.contentRect;
                    throttledContainerResize(width, height);
                }
            });
            resizeObserver.observe(container.current);
            return () => {
                resizeObserver.disconnect();
            };
        },
        [throttledContainerResize]
    );

    useEffect(
        () => {
            const layoutDetailInfo = mapLayoutNameToLayoutDetails(layoutOptions);
            setLayout(layoutDetailInfo);
        },
        [layoutOptions]
    );

    let clickount = 0;
    const debounceClick = _.debounce((evt, graphInstance) => {
        if (clickount > 1) {
            clickount = 0;
            return;
        }
        clearEdgesStates(graph.current, ['edgeActive', 'addActive']);
        const {item} = evt;
        clearItemStates(graph.current, item, ['customActive', 'addActive']);
        graphInstance.setItemState(item, 'customSelected', true);
        onNodeClick && onNodeClick(item);
        clickount = 0;
    }, 300);

    useEffect(
        () => {
            const hasGraphInstance = graph.current && !graph.current.destroyed;
            const shouldLayout = !(_.isEmpty(data.nodes) && _.isEmpty(data.edges)) && layout;
            if (!hasGraphInstance && shouldLayout) {
                const graphOptions = {
                    container: container.current,
                    enabledStack: true,
                    maxStep: 11,
                    modes: {
                        default: [
                            'drag-canvas',
                            'zoom-canvas',
                            'drag-node'],
                    },
                    animate: true,
                    defaultNode: {
                        labelCfg: {
                            position: 'bottom',
                        },
                        icon: {
                            lineWidth: 0,
                            fontSize: 36,
                        },
                    },
                };
                const graphInstance = new G6.Graph(graphOptions);
                graphInstance.on('node:mouseenter', evt => {
                    const {item} = evt;
                    graphInstance.setItemState(item, 'customActive', true);
                    highLightRelatedEdges(graphInstance, item);
                    setItemLabelState(graphInstance, item, 'bold');
                });
                graphInstance.on('node:mouseleave', evt => {
                    const {item} = evt;
                    clearItemStates(graphInstance, item, ['customActive', 'addActive']);
                    clearEdgesStates(graphInstance, ['edgeActive', 'addActive']);
                    setItemLabelState(graphInstance, item, 'normal');
                });
                graphInstance.on('node:click', evt => {
                    const {item} = evt;
                    clickount++;
                    clearSelectedStates(graphInstance);
                    debounceClick(evt, graphInstance, item);
                });
                graphInstance.on('edge:mouseenter', evt => {
                    const {item} = evt;
                    graphInstance.setItemState(item, 'edgeActive', true);
                });
                graphInstance.on('edge:mouseleave', evt => {
                    const {item} = evt;
                    clearItemStates(graphInstance, item, ['edgeActive', 'addActive']);
                });
                graphInstance.on('edge:click', evt => {
                    clearSelectedStates(graphInstance);
                    const {item} = evt;
                    graphInstance.setItemState(item, 'edgeSelected', true);
                    item.toFront();
                    onEdgeClick && onEdgeClick(item);
                });
                graphInstance.on('canvas:click', evt => {
                    clearSelectedStates(graphInstance);
                });
                graphInstance.on('node:dblclick', evt => {
                    const {item} = evt;
                    onNodedbClick && onNodedbClick(item, graphInstance);
                });
                graphInstance.on('afterrender', evt => {
                    fitView(graphInstance);
                    onGraphRender && onGraphRender(graphInstance);
                });
                graphInstance.get('canvas').set('localRefresh', false);
                graph.current = graphInstance;
                setContext({
                    graph: graphInstance,
                });
                graphInstance.data(data);
                graphInstance.updateLayout(layout);
                graphInstance.render();
            };
        },
        [clickount, data, debounceClick, layout, onEdgeClick, onGraphRender, onNodeClick, onNodedbClick]
    );

    useEffect(
        () => {
            return () => {
                graph.current?.destroy();
            };
        },
        []
    );

    return (
        <GraphContext.Provider value={context}>
            <div ref={container} className={graphClassName} id={'graph'}>
                {props.children}
            </div>
        </GraphContext.Provider>
    );
};

export default Graph;
