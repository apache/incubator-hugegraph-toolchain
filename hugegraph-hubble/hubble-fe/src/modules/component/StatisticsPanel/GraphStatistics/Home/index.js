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
 */

import {useContext, useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../../../Context';
import GraphStatisticsHeader from '../Header';
import BarChartComponent from '../../BarChartComponent';
import {hubbleRenderId} from '../../../../../visualization/hubble/adapter';
import c from './index.module.scss';

const GraphStatistics = ({graphDataNums, statistics = {}}) => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const {isolated_vertices = [], isolated_edges = []} = statistics;
    const elements = useMemo(() => new Map([...(snapshot?.data.nodes || []),
        ...(snapshot?.data.edges || [])].map(element => [element.id, element])), [snapshot?.data]);
    const nodeIds = new Set(snapshot?.data.nodes.map(node => node.id));
    const currentId = [...(snapshot?.selectedIds || []), ...(snapshot?.hoveredIds || [])]
        .find(id => elements.get(id)?.data.raw.statistics && nodeIds.has(id));
    const current = elements.get(currentId)?.data.raw;
    const incidence = current?.statistics?.incidence_vertices || [];
    const selected = new Set(snapshot?.selectedIds);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    const groups = [
        {key: 'zero_degree_nodes', ids: isolated_vertices, kind: 'node', total: graphDataNums?.nodesNum},
        {key: 'zero_degree_edges', ids: isolated_edges, kind: 'edge', total: graphDataNums?.edgesNum},
        {key: 'incidence_nodes', ids: incidence, kind: 'node', total: graphDataNums?.nodesNum,
            name: current?.id == null ? t('analysis.canvas.statistics_panel.select_node') : String(current.id)},
    ];
    return (
        <div className={c.graphStatistics}>
            {groups.map(group => {
                const ids = group.ids.map(id => hubbleRenderId(group.kind, id)).filter(id => elements.has(id));
                const hidden = ids.length > 0 && ids.every(id => elements.get(id)?.style?.visibility === 'hidden');
                const active = ids.length > 0 && ids.every(id => selected.has(id));
                return (
                    <div key={group.key}>
                        <GraphStatisticsHeader
                            name={t(`analysis.canvas.statistics_panel.${group.key}`)}
                            description={t(`analysis.canvas.statistics_panel.${group.key}_desc`)}
                            highlightFunc={() => run(() => controller.select(active ? [] : ids))}
                            hideFunc={() => run(() => controller.hide(ids, !hidden))}
                            highlightFuncDisable={!ready || hidden || ids.length === 0}
                            hideFuncDisable={!ready || ids.length === 0}
                        />
                        <BarChartComponent data={[{...(group.name ? {name: group.name} : {}),
                            count: group.ids.length}]} totalData={group.total} />
                    </div>
                );
            })}
        </div>
    );
};

export default GraphStatistics;
