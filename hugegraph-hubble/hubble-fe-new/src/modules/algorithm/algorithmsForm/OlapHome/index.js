/**
 * @file Olap图算法表单列表
 * @author
 */

import React, {useContext} from 'react';
import {Collapse, Tooltip} from 'antd';
import GraphAnalysisContext from '../../../Context';
import _ from 'lodash';
import {ALGORITHM_NAME, ALGORITHM_MODE, GRAPH_LOAD_STATUS} from '../../../../utils/constants';
import OlapItem from '../Olap/OlapItem';
import c from './index.module.scss';

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

const {OLAP} = ALGORITHM_MODE;

const OlapFormHome = props => {
    const {
        onOlapFormSubmit,
        search,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } =  props;

    const {isVermeer, graphStatus} = useContext(GraphAnalysisContext);

    const getSearchedList = (arr, value) => {
        return arr.filter(item => item.includes(value));
    };

    const olapList = isVermeer ? olapVermeerList : olapComputeList;

    // 筛选显示已搜索到的算法
    const basicOlapList = getSearchedList(olapList, search);
    const isEmptyBasicOlap = _.isEmpty(basicOlapList);
    const shouldDisableForm = isVermeer && graphStatus !== GRAPH_LOAD_STATUS.LOADED;

    return (
        <div>
            {!isEmptyBasicOlap && (
                <Tooltip title={shouldDisableForm ? '仅当选择的图数据加载完成后，才可以使用OLAP算法。' : ''}>
                    <div className={c.algorithmCatagery}>{OLAP}</div>
                </Tooltip>
            )}
            <Collapse ghost accordion className={c.sideBarCollapse}>
                {
                    basicOlapList.map(item =>
                        (
                            <OlapItem
                                key={item}
                                handleFormSubmit={onOlapFormSubmit}
                                algorithmName={item}
                                searchValue={search}
                                currentAlgorithm={currentAlgorithm}
                                updateCurrentAlgorithm={updateCurrentAlgorithm}
                            />
                        )
                    )
                }
            </Collapse>
        </div>
    );
};

export default OlapFormHome;
