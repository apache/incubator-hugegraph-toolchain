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
 * @file  图统计
 * @author gouzixing
 */

import React, {useCallback, useContext, useEffect, useMemo, useState} from 'react';
import {GraphContext} from '../../../Context';
import GraphStatisticsHeader from '../Header';
import BarChartComponent from '../../BarChartComponent';
import {clearSelectedStates} from '../../../../../utils/handleGraphState';
import c from './index.module.scss';
import _ from 'lodash';

const description = {
    zeroDegreeNodes: '孤立点是指和其余点没有关联，在画布中独立存在的点。',
    zeroDegreeEdges: '只有一度关联的点的边。',
    incidenceNodes: '只有选中图上的一个点，才能显示相应的关联点的数量。',
};

const GraphStatistics = props => {

    const {graphDataNums, statistics} =  props;
    const {isolated_vertices = [], isolated_edges = []} = statistics;

    const {graph} = useContext(GraphContext);

    const [isHighlightZeroDegreeNodes, setHighlightZeroDegreeNodes] = useState(false);
    const [isHideZeroDegreeNodes, setHideZeroDegreeNodes] = useState(false);
    const [isHighlightZeroDegreeEdeges, setHighlightZeroDegreeEdges] = useState(false);
    const [isHideZeroDegreeEdeges, setHideZeroDegreeEdges] = useState(false);
    const [isHighlightIncidenceNodes, setHighlightIncidenceNodes] = useState(false);
    const [isHideIncidenceNodes, setHideIncidenceNodes] = useState(false);
    const [selectedNode, setSelectedNode] = useState();

    const incidenceNodes = selectedNode?.statistics.incidence_vertices;
    const isIsolatedVerticesEmpty = _.isEmpty(isolated_vertices);
    const isIsolatedEdgesEmpty = _.isEmpty(isolated_edges);
    const isIncidenceNodesEmpty = _.isEmpty(incidenceNodes);

    useEffect(
        () => {
            graph.on('node:mouseenter', evt => {
                const {id, statistics} = evt.item.getModel();
                setSelectedNode({id, statistics});
            });
        },
        [graph]
    );

    const highlightZeroDegreeNodes = useCallback(
        () => {
            // 清空画布有的其他高亮状态
            clearSelectedStates(graph);
            isolated_vertices.forEach(
                item => {
                    const node = graph.findById(item);
                    node && graph.setItemState(node, 'customSelected', !isHighlightZeroDegreeNodes);
                }
            );
            setHighlightZeroDegreeNodes(pre => !pre);
        },
        [graph, isHighlightZeroDegreeNodes, isolated_vertices]
    );

    const hideZeroDegreeNodes = useCallback(
        () => {
            isolated_vertices.forEach(
                item => {
                    const node = graph.findById(item);
                    if (node) {
                        isHideZeroDegreeNodes ? graph.showItem(node, false) : graph.hideItem(node, false);
                    }
                }
            );
            setHideZeroDegreeNodes(pre => !pre);
        },
        [graph, isHideZeroDegreeNodes, isolated_vertices]
    );

    const highlightZeroDegreeEdges = useCallback(
        () => {
            // 清空画布有的其他高亮状态
            clearSelectedStates(graph);
            isolated_edges.forEach(
                item => {
                    const edge = graph.findById(item);
                    edge && graph.setItemState(edge, 'edgeActive', !isHighlightZeroDegreeEdeges);
                }
            );
            setHighlightZeroDegreeEdges(pre => !pre);
        },
        [graph, isHighlightZeroDegreeEdeges, isolated_edges]
    );

    const hideZeroDegreeEdges = useCallback(
        () => {
            isolated_edges.forEach(
                item => {
                    const edge = graph.findById(item);
                    if (edge) {
                        const sourceItem = edge.getSource();
                        const targetItem = edge.getTarget();
                        if (!isHideZeroDegreeEdeges) {
                            graph.hideItem(sourceItem, false);
                            graph.hideItem(targetItem, false);
                        }
                        else {
                            graph.showItem(sourceItem, false);
                            graph.showItem(targetItem, false);
                        }
                    }
                }
            );
            setHideZeroDegreeEdges(pre => !pre);
        },
        [graph, isHideZeroDegreeEdeges, isolated_edges]
    );

    const highlightIncidenceNodes = useCallback(
        () => {
            clearSelectedStates(graph);
            incidenceNodes.forEach(
                item => {
                    const node = graph.findById(item);
                    node && graph.setItemState(node, 'customSelected', !isHighlightIncidenceNodes);
                }
            );
            setHighlightIncidenceNodes(pre => !pre);
        },
        [graph, incidenceNodes, isHighlightIncidenceNodes]
    );

    const hideIncidenceNodes = useCallback(
        () => {
            incidenceNodes.forEach(
                item => {
                    const node = graph.findById(item);
                    if (node) {
                        isHideIncidenceNodes ? graph.showItem(item, false) : graph.hideItem(item, false);
                    }
                }
            );
            setHideIncidenceNodes(pre => !pre);
        },
        [graph, incidenceNodes, isHideIncidenceNodes]
    );

    const zeroDegreeNodesData = useMemo(
        () => ([{count: isolated_vertices.length}]),
        [isolated_vertices]
    );

    const zeroDegreeEdgesData = useMemo(
        () => ([{count: isolated_edges.length}]),
        [isolated_edges]
    );

    const incidenceData =  useMemo(
        () => {
            const {id, statistics} =  selectedNode || {};
            const {incidence_vertices = []} = statistics || {};
            return [
                {
                    name: id || '请选择节点',
                    count: incidence_vertices.length,
                },
            ];
        },
        [selectedNode]
    );

    return (
        <div className={c.graphStatistics}>
            <GraphStatisticsHeader
                name='孤立点'
                description={description.zeroDegreeNodes}
                highlightFunc={highlightZeroDegreeNodes}
                hideFunc={hideZeroDegreeNodes}
                highlightFuncDisable={isHideZeroDegreeNodes || isIsolatedVerticesEmpty}
                hideFuncDisable={isIsolatedVerticesEmpty}
            />
            <BarChartComponent data={zeroDegreeNodesData} totalData={graphDataNums?.nodesNum} />
            <GraphStatisticsHeader
                name='孤立边'
                description={description.zeroDegreeEdges}
                highlightFunc={highlightZeroDegreeEdges}
                hideFunc={hideZeroDegreeEdges}
                highlightFuncDisable={isHideZeroDegreeEdeges || isIsolatedEdgesEmpty}
                hideFuncDisable={isIsolatedEdgesEmpty}
            />
            <BarChartComponent data={zeroDegreeEdgesData} totalData={graphDataNums?.edgesNum} />
            <GraphStatisticsHeader
                name='关联点'
                description={description.incidenceNodes}
                highlightFunc={highlightIncidenceNodes}
                hideFunc={hideIncidenceNodes}
                highlightFuncDisable={!selectedNode || isHideIncidenceNodes || isIncidenceNodesEmpty}
                hideFuncDisable={!selectedNode || isIncidenceNodesEmpty}
            />
            <BarChartComponent data={incidenceData} totalData={graphDataNums?.nodesNum} />
        </div>
    );
};

export default GraphStatistics;
