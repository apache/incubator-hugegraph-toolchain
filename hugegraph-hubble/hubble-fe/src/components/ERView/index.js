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

import {Graph, Shape, History} from '@antv/x6';
import {Button, Divider, Dropdown, Space, Tooltip} from 'antd';
import {useCallback, useEffect, useRef, useState} from 'react';
import {
    ZoomInOutlined,
    ZoomOutOutlined,
    RedoOutlined,
    UndoOutlined,
    DeleteOutlined,
} from '@ant-design/icons';
import style from './index.module.scss';
import testData from './data/test.json';
import {erRectConfig, erRectHeadConfig, erPortPosition} from './config';
import {EditVertexLayer, EditEdgeLayer} from './EditLayer';
import {setCell} from './utils';
import {useTranslation} from 'react-i18next';

Graph.registerPortLayout('erPortPosition', erPortPosition);
Graph.registerNode('er-rect', erRectConfig, true);
Graph.registerNode('er-head-rect', erRectHeadConfig, true);

const ERView = () => {
    const container = useRef(null);
    const graph = useRef(null);
    const {t} = useTranslation();
    const [vertexVisible, setVertexVisible] = useState(false);
    const [edgeVisible, setEdgeVisible] = useState(false);

    useEffect(() => {
        graph.current = new Graph({
            container: container.current,
            grid: true,
            // selecting: true,
            connecting: {
                allowMulti: 'withPort',
                allowBlank: false,
                allowLoop: false,
                allowNode: false,
                allowEdge: false,
                snap: true,
                router: {
                    name: 'er',
                    args: {
                        offset: 25,
                        direction: 'H',
                    },
                },
                createEdge() {
                    return new Shape.Edge({
                        // tools: [
                        //     {
                        //         name: 'button-remove',
                        //         args: {
                        //             distance: -40,
                        //         },
                        //     },
                        // ],
                        attrs: {
                            line: {
                                stroke: '#A2B1C3',
                                strokeWidth: 2,
                            },
                        },
                    });
                },
            },
        });

        graph.current.use(new History({enabled: true}));

        const cells = [];
        testData.forEach(item => {
            if (item.shape === 'edge') {
                cells.push(graph.current.createEdge(item));
            }
            else {
                cells.push(graph.current.createNode(item));
            }
        });
        graph.current.resetCells(cells);
        graph.current.zoomToFit({padding: 10, maxScale: 1});

        graph.current.on('node:add', ({e, node}) => {
            e.stopPropagation();
            void node;
            // const member = createNode(
            //     'Employee',
            //     'New Employee',
            //     Math.random() < 0.5 ? male : female,
            // );
            // graph.freeze();
            // graph.addCell([member, createEdge(node, member)])
            // layout();
        });

        graph.current.on('node:delete', ({e, node}) => {
            e.stopPropagation();
            void node;
            // graph.freeze();
            // graph.removeCell(node);
            // layout();
        });

        graph.current.on('edge:mouseenter', ({cell}) => {
            cell.addTools([
                {
                    name: 'target-arrowhead',
                    args: {
                        attrs: {
                            fill: 'red',
                        },
                    },
                },
                {
                    name: 'button-remove',
                    args: {
                        distance: 0.7,
                    },
                },
                {
                    name: 'button',
                    args: {
                        distance: 0.5,
                        onClick: ({cell}) => {
                            void cell;
                        },
                        markup: [
                            {
                                tagName: 'circle',
                                selector: 'button',
                                attrs: {
                                    r: 10,
                                    stroke: '#A2B1C3',
                                    strokeWidth: 1,
                                    fill: 'white',
                                    cursor: 'pointer',
                                },
                            },
                            {
                                tagName: 'g',
                                attrs: {
                                    transform: 'translate(-8, -9)',
                                    cursor: 'pointer',
                                },
                                children: [
                                    {
                                        tagName: 'svg',
                                        attrs: {
                                            width: 16,
                                            height: 16,
                                            viewBox: '0 0 1024 1024',
                                        },
                                        children: [
                                            {
                                                tagName: 'path',
                                                attrs: {
                                                    d: 'M257.7 752c2 0 4-0.2 6-0.5L431.9 722c2-0.4 3.9-1.3 5.3-2.8l423.9-423.9c3.9-3.9 3.9-10.2 0-14.1L694.9 114.9c-1.9-1.9-4.4-2.9-7.1-2.9s-5.2 1-7.1 2.9L256.8 538.8c-1.5 1.5-2.4 3.3-2.8 5.3l-29.5 168.2c-1.9 11.1 1.5 21.9 9.4 29.8 6.6 6.4 14.9 9.9 23.8 9.9z m67.4-174.4L687.8 215l73.3 73.3-362.7 362.6-88.9 15.7 15.6-89zM880 836H144c-17.7 0-32 14.3-32 32v36c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-36c0-17.7-14.3-32-32-32z',
                                                    fill: '#A2B1C3',
                                                },
                                            },
                                        ],
                                    },
                                ],
                            },
                            // {
                            //     tagName: 'text',
                            //     textContent: <ZoomInOutlined />,
                            //     selector: 'icon',
                            //     attrs: {
                            //         fill: '#fe854f',
                            //         fontSize: 10,
                            //         textAnchor: 'middle',
                            //         pointerEvents: 'none',
                            //         y: '0.3em',
                            //     },
                            // },
                        ],
                    },
                },
            ]);
        });

        graph.current.on('edge:mouseleave', ({cell}) => {
            cell.removeTools();
        });

        return () => {
            graph.current?.dispose();
            graph.current = null;
        };
    }, []);

    const addVertex = useCallback(vertex => {
        graph.current.addNode(graph.current.createNode(setCell(t, vertex)));
    }, [t]);

    const addEdge = useCallback(edge => {
        graph.current.addNode(graph.current.createNode(setCell(t, edge, 'edge')));
    }, [t]);

    const showVertex = useCallback(() => {
        setVertexVisible(true);
    }, []);

    const hideVertex = useCallback(() => {
        setVertexVisible(false);
    }, []);

    const showEdge = useCallback(() => {
        setEdgeVisible(true);
    }, []);

    const hideEdge = useCallback(() => {
        setEdgeVisible(false);
    }, []);

    const handleZoomIn = useCallback(() => {
        graph.current?.zoom(0.5);
    }, []);

    const handleZoomOut = useCallback(() => {
        graph.current?.zoom(-0.5);
    }, []);

    const handleUndo = useCallback(() => {
        graph.current?.undo();
    }, []);

    const handleRedo = useCallback(() => {
        graph.current?.redo();
    }, []);

    const vertexMenu = {items: [
        {key: 'create', label: t('ERView.vertex.create'), onClick: showVertex},
        {type: 'divider'},
        {key: 'v1', label: t('ERView.vertex.v1name')},
        {key: 'v2', label: t('ERView.vertex.v2name')},
    ]};
    const edgeMenu = {items: [
        {key: 'create', label: t('ERView.edge.create'), onClick: showEdge},
        {type: 'divider'},
        {key: 'e1', label: t('ERView.edge.e1name')},
        {key: 'e2', label: t('ERView.edge.e2name')},
    ]};

    return (
        <div className={style.main}>
            <Space className={style.toolbar} size={8} wrap>
                <Tooltip title={t('ERView.control.zoom_in')}>
                    <Button icon={<ZoomInOutlined />} onClick={handleZoomIn}
                        aria-label={t('ERView.control.zoom_in')} />
                </Tooltip>
                <Tooltip title={t('ERView.control.zoom_out')}>
                    <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut}
                        aria-label={t('ERView.control.zoom_out')} />
                </Tooltip>
                <Divider orientation='vertical' />
                <Tooltip title={t('ERView.control.undo')}>
                    <Button icon={<UndoOutlined />} onClick={handleUndo}
                        aria-label={t('ERView.control.undo')} />
                </Tooltip>
                <Tooltip title={t('ERView.control.redo')}>
                    <Button icon={<RedoOutlined />} onClick={handleRedo}
                        aria-label={t('ERView.control.redo')} />
                </Tooltip>
                <Divider orientation='vertical' />
                <Button icon={<DeleteOutlined />} disabled aria-label={t('common.action.delete')} />
                <Button disabled>{t('ERView.control.auto_map')}</Button>
                <Divider orientation='vertical' />
                <Dropdown menu={vertexMenu} trigger={['click']}>
                    <Button>{t('ERView.vertex.name')}</Button>
                </Dropdown>
                <Dropdown menu={edgeMenu} trigger={['click']}>
                    <Button>{t('ERView.edge.name')}</Button>
                </Dropdown>
            </Space>
            <div ref={container} className={style.content} />
            <EditVertexLayer
                open={vertexVisible}
                onCancle={hideVertex}
                onChange={addVertex}
            />
            <EditEdgeLayer
                open={edgeVisible}
                onCancle={hideEdge}
                onChange={addEdge}
            />
        </div>
    );
};

export default ERView;
