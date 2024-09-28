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

import {Graph, Cell, Shape, Color} from '@antv/x6';
import {ReactShape} from '@antv/x6-react-shape';
import {Menu, Toolbar, Dropdown} from '@antv/x6-react-components';
import {memo, useCallback, useEffect, useRef, useState} from 'react';
import {
    ZoomInOutlined,
    ZoomOutOutlined,
    RedoOutlined,
    UndoOutlined,
    DeleteOutlined,
} from '@ant-design/icons';
import style from './index.module.scss';
import testData from './data/test.json';
import vertexData from './data/vertex.json';
import edgeData from './data/edge.json';
import propertyData from './data/property.json';
import {erRectConfig, erRectHeadConfig, erPortPosition} from './config';
import '@antv/x6-react-components/es/menu/style/index.css';
import '@antv/x6-react-components/es/toolbar/style/index.css';
import {EditVertexLayer, EditEdgeLayer} from './EditLayer';
import {setCell} from './utils';
import {useTranslation} from 'react-i18next';

Graph.registerPortLayout('erPortPosition', erPortPosition);
Graph.registerNode('er-rect', erRectConfig, true);
Graph.registerNode('er-head-rect', erRectHeadConfig, true);

const ERShapce = memo(
    ({node, text}) => {
        const color = Color.randomHex();

        return (
            <div
                style={{
                    color: Color.invert(color, true),
                    width: '100%',
                    height: '100%',
                    textAlign: 'center',
                    lineHeight: '40px',
                    background: color,
                }}
            >
                {text}
            </div>
        );
    },
    (prev, next) => {
        return Boolean(next.node?.hasChanged('data'));
    }
);

const ERView = () => {
    const container = useRef(null);
    const graph = useRef(null);
    const {t} = useTranslation();
    const [vertexVisible, setVertexVisible] = useState(false);
    const [edgeVisible, setEdgeVisible] = useState(false);
    const [data, setData] = useState([]);

    useEffect(() => {
        graph.current = new Graph({
            container: container.current,
            grid: true,
            history: true,
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
            console.log('add', node);
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
            console.log('delete', node);
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
                            console.log(cell);
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
                                                    // eslint-disable-next-line max-len
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
    }, []);

    const [vertexList, setVertexList] = useState([]);
    const [edgeList, setEdgeList] = useState([]);

    const addVertex = useCallback(vertex => {
        graph.current.addNode(graph.current.createNode(setCell(t, vertex)));
    }, [t]);

    const addEdge = useCallback(edge => {
        graph.current.addNode(graph.current.createNode(setCell(t, edge, 'edge')));
    }, [t]);

    const addValueMap = () => {

    };

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
        graph.current?.history.undo();
    }, []);

    const handleRedo = useCallback(() => {
        graph.current?.history.redo();
    }, []);

    const Item = Toolbar.Item;
    const Group = Toolbar.Group;
    const vertexMenu = (
        <Menu>
            <Menu.Item key="1" onClick={showVertex}>{t('ERView.vertex.create')}</Menu.Item>
            <Menu.Divider />
            <Menu.Item key="2">{t('ERView.vertex.v1name')}</Menu.Item>
            <Menu.Item key="3">{t('ERView.vertex.v2name')}</Menu.Item>
        </Menu>
    );

    const edgeMenu = (
        <Menu>
            <Menu.Item key="1" onClick={showEdge}>{t('ERView.edge.create')}</Menu.Item>
            <Menu.Divider />
            <Menu.Item key="2">{t('ERView.edge.e1name')}</Menu.Item>
            <Menu.Item key="3">{t('ERView.edge.e2name')}</Menu.Item>
        </Menu>
    );

    return (
        <div className={style.main}>
            <Toolbar extra={<span onClick={() => console.log(graph.current.toJSON())}>Save</span>}>
                <Group>
                    <Item
                        name="zoomIn"
                        tooltip={t('ERView.control.zoom_in')}
                        icon={<ZoomInOutlined />}
                        onClick={handleZoomIn}
                    />
                    <Item
                        name="zoomOut"
                        tooltip={t('ERView.control.zoom_out')}
                        icon={<ZoomOutOutlined />}
                        onClick={handleZoomOut}
                    />
                </Group>
                <Group>
                    <Item
                        name="undo"
                        tooltip={t('ERView.control.undo')}
                        icon={<UndoOutlined />}
                        onClick={handleUndo}
                    />
                    <Item
                        name="redo"
                        tooltip={t('ERView.control.redo')}
                        icon={<RedoOutlined />}
                        onClick={handleRedo}
                    />
                </Group>
                <Group>
                    <Item name="delete" icon={<DeleteOutlined />} disabled tooltip="Delete (Delete)" />
                </Group>
                <Group>
                    <Item text={t('ERView.control.auto_map')} />
                </Group>
                <Group>
                    <Item text={t('ERView.vertex.name')} dropdown={vertexMenu} />
                    <Item text={t('ERView.edge.name')} dropdown={edgeMenu} />
                </Group>
            </Toolbar>
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
