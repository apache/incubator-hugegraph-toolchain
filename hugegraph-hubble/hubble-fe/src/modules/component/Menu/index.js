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

import {useContext, useEffect, useCallback} from 'react';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../Context';
import G6 from '@antv/g6';

const Menu = props => {
    const {t} = useTranslation();
    const {
        onClickAddNode,
        onClickAddEdge,
        onClickExpand,
        onClickSearch,
    } = props;

    const {graph} = useContext(GraphContext);

    const handleFixNode = useCallback(
        node => {
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
        [graph]
    );

    const handleAddEdge = useCallback(
        (data, isOut) => {
            onClickAddEdge(data, isOut);
        },
        [onClickAddEdge]
    );

    const onExecuteQuery = useCallback(
        async params => {
            onClickExpand(params);
        },
        [onClickExpand]
    );

    const handleClickSearch = useCallback(
        value => {
            onClickSearch(value);
        },
        [onClickSearch]
    );


    useEffect(
        () => {
            const options = {
                offsetX: 20,
                offsetY: -20,
                itemTypes: ['node', 'canvas'],
                getContent(evt) {
                    const {item} = evt;
                    if (item) {
                        const hasLocked = item.hasLocked()
                            ? t('analysis.canvas.context_menu.unfix')
                            : t('analysis.canvas.context_menu.fix');
                        return `<ul>
                            <li data-type='expand'>${t('analysis.canvas.context_menu.expand')}</li>
                            <li data-type='search'>${t('analysis.canvas.context_menu.search')}</li>
                            <li data-type='fixed'>${hasLocked}</li>
                            <li data-type='hide'>${t('analysis.canvas.context_menu.hide')}</li>
                            <li data-type='addOutEdge'>${t('analysis.canvas.context_menu.add_out_edge')}</li>
                            <li data-type='addInEdge'>${t('analysis.canvas.context_menu.add_in_edge')}</li>
                        </ul>`;
                    }
                    return `<ul>
                        <li data-type='add'>${t('analysis.canvas.context_menu.add_vertex')}</li>
                    </ul>`;
                },
                handleMenuClick(target, item) {
                    const type = target.dataset.type;
                    if (item) {
                        const model = item.getModel();
                        const vertexInfo = {
                            vertexId: model.id,
                            vertexLabel: model.itemType,
                        };
                        switch (type) {
                            case 'expand':
                                onExecuteQuery({vertex_id: model.id, vertex_label: model.itemType});
                                break;
                            case 'search':
                                handleClickSearch(vertexInfo);
                                break;
                            case 'fixed':
                                handleFixNode(item);
                                break;
                            case 'hide':
                                graph.hideItem(item);
                                break;
                            case 'addOutEdge':
                                const addOutEdge = Object.assign({}, vertexInfo);
                                handleAddEdge(addOutEdge, true);
                                break;
                            case 'addInEdge':
                                const addInEdge = Object.assign({}, vertexInfo);
                                handleAddEdge(addInEdge, false);
                                break;
                        }
                    }
                    else {
                        if (type === 'add') {
                            onClickAddNode();
                        }
                    }
                },
            };
            const menu = new G6.Menu(options);
            if (graph && !graph.destroyed) {
                graph.addPlugin(menu);
            }
            return () => {
                if (graph && !graph.destroyed) {
                    graph.removePlugin(menu);
                }
            };
        },
        [graph, handleAddEdge, onClickAddNode, handleFixNode, onExecuteQuery, handleClickSearch, t]
    );
    return null;
};

export default Menu;
