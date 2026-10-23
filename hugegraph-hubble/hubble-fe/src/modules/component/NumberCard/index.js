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
 * @file  画布数据统计
 */

import c from './index.module.scss';
import classnames from 'classnames';
import {useTranslation} from 'react-i18next';
import {isKnownCount, formatRecordedCount} from '../../../utils/graphStatistics';

const isLoadingCount = value => {
    if (value === null || value === undefined
        || (typeof value === 'string' && value.trim() === '')) {
        return false;
    }
    const count = Number(value);
    return Number.isFinite(count) && count < 0;
};

const getCountDisplay = (value, t) => {
    if (isLoadingCount(value)) {
        return t('analysis.canvas.number_card.loading');
    }
    return isKnownCount(value) ? value : t('analysis.canvas.number_card.unavailable');
};

const NumberCard = props => {
    const {t} = useTranslation();
    const {pathNum, data, hasPadding} = props;
    const {currentGraphNodesNum, currentGraphEdgesNum, allGraphNodesNum, allGraphEdgesNum,
        approximate, scan_limit: scanLimit} = data;
    const totalDisplay = value => isKnownCount(value)
        ? formatRecordedCount(value, data) : getCountDisplay(value, t);
    const statisticsHint = scanLimit > 0
        ? t('analysis.canvas.number_card.bounded_hint', {limit: scanLimit})
        : approximate ? t('analysis.canvas.number_card.approximate_hint') : '';

    const currentGraphNodes = getCountDisplay(currentGraphNodesNum, t);
    const currentGraphEdges = getCountDisplay(currentGraphEdgesNum, t);
    const allGraphNodes = totalDisplay(allGraphNodesNum);
    const allGraphEdges = totalDisplay(allGraphEdgesNum);
    const hasNodeTotal = isKnownCount(allGraphNodesNum) || isLoadingCount(allGraphNodesNum);
    const hasEdgeTotal = isKnownCount(allGraphEdgesNum) || isLoadingCount(allGraphEdgesNum);
    const nodeSummary = t(`analysis.canvas.number_card.${hasNodeTotal ? 'node_summary' : 'node_summary_unknown'}`, {
        current: currentGraphNodes,
        total: allGraphNodes,
    });
    const edgeSummary = t(`analysis.canvas.number_card.${hasEdgeTotal ? 'edge_summary' : 'edge_summary_unknown'}`, {
        current: currentGraphEdges,
        total: allGraphEdges,
    });

    const graphClassName = classnames(
        c.numberCard,
        {[c.numberCardWithLayoutPanel]: hasPadding}
    );

    return (
        <div className={graphClassName}>
            {pathNum && (
                <div className={c.numberCardItem}>
                    <div className={c.numberCardTitle}>
                        {t('analysis.canvas.number_card.paths')}
                    </div>
                    <div className={c.numberCardInfo}>
                        <span className={c.numberCur}>{pathNum}</span>
                    </div>
                </div>
            )}
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>
                    {t('analysis.canvas.number_card.nodes')}
                </div>
                <div
                    className={c.numberCardInfo}
                    role='group'
                    aria-label={nodeSummary}
                    title={statisticsHint ? `${nodeSummary}\n${statisticsHint}` : nodeSummary}
                >
                    <span className={c.numberCur} aria-hidden='true'>
                        {currentGraphNodes}
                    </span>
                    {hasNodeTotal ? <>
                        <span aria-hidden='true'>/</span>
                        <span className={c.numberAll} aria-hidden='true'>{allGraphNodes}</span>
                    </> : <span className={c.currentOnly} aria-hidden='true'>
                        {t('analysis.canvas.number_card.current_result')}
                    </span>}
                </div>
            </div>
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>
                    {t('analysis.canvas.number_card.edges')}
                </div>
                <div
                    className={c.numberCardInfo}
                    role='group'
                    aria-label={edgeSummary}
                    title={statisticsHint ? `${edgeSummary}\n${statisticsHint}` : edgeSummary}
                >
                    <span className={c.numberCur} aria-hidden='true'>
                        {currentGraphEdges}
                    </span>
                    {hasEdgeTotal ? <>
                        <span aria-hidden='true'>/</span>
                        <span className={c.numberAll} aria-hidden='true'>{allGraphEdges}</span>
                    </> : <span className={c.currentOnly} aria-hidden='true'>
                        {t('analysis.canvas.number_card.current_result')}
                    </span>}
                </div>
            </div>
        </div>
    );
};

export default NumberCard;
