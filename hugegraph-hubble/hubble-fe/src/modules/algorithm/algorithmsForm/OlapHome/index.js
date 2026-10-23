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
 * @file Olap图算法表单列表
 */

import {AlgorithmPanelGroup} from '../AlgorithmPanel';
import {useContext, useEffect, useState} from 'react';
import {Button} from 'antd';
import * as api from '../../../../api';
import GraphAnalysisContext from '../../../Context';
import _ from 'lodash';
import {
    GRAPH_LOAD_STATUS,
    isAlgorithmNameMatched,
    useTranslatedConstants,
} from '../../../../utils/constants';
import OlapItem from '../Olap/OlapItem';
import c from './index.module.scss';
import {useTranslation} from 'react-i18next';
import {COMPUTER_DOC} from '../algorithmDocs';




const OlapFormHome = props => {
    const {
        onOlapFormSubmit,
        search,
        currentAlgorithm,
        updateCurrentAlgorithm,
        canRunLouvain,
        graphSpace,
        graph,
    } =  props;
    const {ALGORITHM_NAME} = useTranslatedConstants();
    const {t} = useTranslation();
    const {
        PAGE_RANK,
        WEAKLY_CONNECTED_COMPONENT,
        DEGREE_CENTRALIT,
        CLOSENESS_CENTRALITY,
        TRIANGLE_COUNT,
        RINGS_DETECTION,
        FILTERED_RINGS_DETECTION,
        LINKS,
        CLUSTER_COEFFICIENT,
        BETWEENNESS_CENTRALITY,
        LABEL_PROPAGATION_ALGORITHM,
        LOUVAIN,
        FILTER_SUBGRAPH_MATCHING,
        K_CORE,
        PERSONAL_PAGE_RANK,
        SSSP,
    } = ALGORITHM_NAME;

    const olapComputeList = [
        PAGE_RANK,
        WEAKLY_CONNECTED_COMPONENT,
        DEGREE_CENTRALIT,
        CLOSENESS_CENTRALITY,
        TRIANGLE_COUNT,
        RINGS_DETECTION,
        FILTERED_RINGS_DETECTION,
        LINKS,
        CLUSTER_COEFFICIENT,
        BETWEENNESS_CENTRALITY,
        LABEL_PROPAGATION_ALGORITHM,
        LOUVAIN,
        FILTER_SUBGRAPH_MATCHING,
        K_CORE,
        PERSONAL_PAGE_RANK,
    ];

    const olapVermeerList = [
        PAGE_RANK,
        WEAKLY_CONNECTED_COMPONENT,
        LABEL_PROPAGATION_ALGORITHM,
        DEGREE_CENTRALIT,
        CLOSENESS_CENTRALITY,
        BETWEENNESS_CENTRALITY,
        TRIANGLE_COUNT,
        K_CORE,
        SSSP,
    ];


    const {isVermeer, graphStatus} = useContext(GraphAnalysisContext);
    const scope = JSON.stringify([graphSpace, graph]);
    const [legacyScope, setLegacyScope] = useState(null);
    const legacyExpanded = !isVermeer && legacyScope === scope;
    const [computerCapability, setComputerCapability] = useState(null);
    const computerAvailable = computerCapability?.scope === scope
        ? computerCapability.available : null;

    useEffect(() => {
        setLegacyScope(null);
    }, [scope, isVermeer]);

    useEffect(() => {
        let cancelled = false;
        if (!legacyExpanded || !graphSpace || !graph) {
            setComputerCapability(null);
            return undefined;
        }
        setComputerCapability(null);
        api.analysis.getOlapCapability(graphSpace, graph, {
            suppressBusinessErrorToast: true,
        }).then(response => {
            if (!cancelled) {
                setComputerCapability({
                    scope,
                    available: response?.status === 200 && response?.data?.available === true,
                });
            }
        }).catch(() => {
            if (!cancelled) {
                setComputerCapability({scope, available: false});
            }
        });
        return () => {
            cancelled = true;
        };
    }, [graph, graphSpace, legacyExpanded, scope]);

    const getSearchedList = (arr, value) => {
        return arr.filter(item => isAlgorithmNameMatched(item, value, t));
    };

    const olapList = isVermeer ? olapVermeerList : olapComputeList;
    const olapGroups = [
        {
            key: 'importance',
            items: [PAGE_RANK, PERSONAL_PAGE_RANK, DEGREE_CENTRALIT,
                CLOSENESS_CENTRALITY, BETWEENNESS_CENTRALITY],
        },
        {
            key: 'communities',
            items: [WEAKLY_CONNECTED_COMPONENT, LABEL_PROPAGATION_ALGORITHM,
                LOUVAIN, K_CORE],
        },
        {
            key: 'structure',
            items: [TRIANGLE_COUNT, RINGS_DETECTION, FILTERED_RINGS_DETECTION,
                LINKS, CLUSTER_COEFFICIENT, FILTER_SUBGRAPH_MATCHING, SSSP],
        },
    ];

    // 筛选显示已搜索到的算法
    const visibleGroups = olapGroups.map(group => ({
        ...group,
        items: getSearchedList(
            group.items.filter(item => olapList.includes(item)), search
        ),
    })).filter(group => !_.isEmpty(group.items));
    const isEmptyBasicOlap = _.isEmpty(visibleGroups);
    const vermeerStatus = !isVermeer ? 'vermeer_not_connected'
        : graphStatus === GRAPH_LOAD_STATUS.LOADED ? 'vermeer_ready' : 'vermeer_load_first';
    const computerStatus = computerAvailable === false ? 'computer_legacy_unavailable'
        : computerAvailable === true ? 'computer_legacy_ready' : 'computer_checking';

    return (
        <div>
            <div className={c.algorithmCatagery}>
                {t('analysis.algorithm.vermeer_olap')}
            </div>
            <p className={c.engineHint}>
                {t(`analysis.algorithm.${vermeerStatus}`)}
            </p>
            {isVermeer && isEmptyBasicOlap && search && (
                <p className={c.engineHint}>{t('analysis.algorithm.vermeer_no_match')}</p>
            )}
            {!isVermeer && (
                <>
                    <Button
                        type='link'
                        size='small'
                        className={c.legacyToggle}
                        aria-expanded={legacyExpanded}
                        onClick={() => setLegacyScope(legacyExpanded ? null : scope)}
                    >
                        {t('analysis.algorithm.computer_legacy_entry')}
                    </Button>
                    {legacyExpanded && (
                        <p className={c.engineHint}>
                            {t(`analysis.algorithm.${computerStatus}`)}{' '}
                            <a href={COMPUTER_DOC} target='_blank' rel='noreferrer'>
                                {t('analysis.algorithm.computer_legacy_docs')}
                            </a>
                        </p>
                    )}
                </>
            )}
            {(isVermeer || (legacyExpanded && computerAvailable === true)) && visibleGroups.map(group => (
                <div key={group.key}>
                    <div className={c.algorithmGoal}>
                        {t(`analysis.algorithm.group.${group.key}`)}
                    </div>
                    <AlgorithmPanelGroup className={c.sideBarCollapse}>
                        {group.items.map(item => (
                            <OlapItem
                                key={item}
                                handleFormSubmit={onOlapFormSubmit}
                                algorithmName={item}
                                searchValue={search}
                                currentAlgorithm={currentAlgorithm}
                                updateCurrentAlgorithm={updateCurrentAlgorithm}
                                canRunLouvain={canRunLouvain}
                            />
                        ))}
                    </AlgorithmPanelGroup>
                </div>
            ))}
        </div>
    );
};

export default OlapFormHome;
