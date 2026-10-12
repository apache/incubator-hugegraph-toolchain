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
 * @file  标签统计
 */

import React, {useMemo} from 'react';
import {Tooltip, Collapse} from 'antd';
import {QuestionCircleOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import c from './index.module.scss';
import BarChartComponent from '../BarChartComponent';
import _ from 'lodash';

const {Panel} = Collapse;

const LabelStatistics = props => {

    const {t} = useTranslation();
    const {graphDataNums, statistics} = props;

    const {edge_label = [], vertex_label = [], highest_degree_vertices = []} = statistics || {};

    const convertedVertexTypeData = useMemo(
        () => {
            return [...vertex_label.map(item => ({name: item.label, count: item.count}))];
        },
        [vertex_label]
    );

    const convertedEdgeTypeData = useMemo(
        () => {
            return [...edge_label.map(item => ({name: item.label, count: item.count}))];
        },
        [edge_label]
    );

    const convertedHighestDegreeVerticesData = useMemo(
        () => {
            const convertedData = [
                ...highest_degree_vertices.map(item => (
                    {name: item.id, count: item.degree}
                )),
            ];
            return _.sortBy(convertedData, ['count']);
        }, [highest_degree_vertices]
    );

    const renderHeader = info => {
        const {name, description} = info;
        return (
            <>
                <span style={{marginRight: '10px'}}>{name}</span>
                <Tooltip title={description}>
                    <QuestionCircleOutlined />
                </Tooltip>
            </>
        );
    };

    return (
        <div className={c.labelStatistics}>
            <Collapse defaultActiveKey={['type', 'highestDegree']} ghost>
                <Panel
                    header={renderHeader({
                        name: t('analysis.canvas.statistics_panel.type_count'),
                        description: t('analysis.canvas.statistics_panel.type_count_desc'),
                    })}
                    key="type"
                >
                    <BarChartComponent data={convertedVertexTypeData} totalData={graphDataNums?.nodesNum} />
                    <BarChartComponent data={convertedEdgeTypeData} totalData={graphDataNums?.edgesNum} />
                </Panel>
                <Panel
                    header={renderHeader({
                        name: t('analysis.canvas.statistics_panel.degree_top10'),
                        description: t('analysis.canvas.statistics_panel.degree_top10_desc'),
                    })}
                    key="highestDegree"
                >
                    <BarChartComponent data={convertedHighestDegreeVerticesData} totalData={graphDataNums?.edgesNum} />
                </Panel>
            </Collapse>
        </div>
    );
};

export default LabelStatistics;
