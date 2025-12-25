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

import {useTranslation} from 'react-i18next';

export const colors = [
    '#5c73e6',
    '#569380',
    '#8ecc93',
    '#fe9227',
    '#fe5b5d',
    '#fd6ace',
    '#4d8dda',
    '#57c7e3',
    '#ffe081',
    '#c570ff',
    '#2b65ff',
    '#0eb880',
    '#76c100',
    '#ed7600',
    '#e65055',
    '#a64ee6',
    '#108cee',
    '#00b5d9',
    '#f2ca00',
    '#e048ae',
];

export const iconsMap = {
    UserOutlined: 'user',
    UsergroupAddOutlined: 'add user',
    IdcardOutlined: 'id card',
    VerifiedOutlined: 'verified',
    UserSwitchOutlined: 'switch user',
    PropertySafetyOutlined: 'property safety',
    TransactionOutlined: 'transaction',
    InsuranceOutlined: 'insurance ',
    ShoppingOutlined: 'shopping',
    CloudSyncOutlined: 'cloud-sync',
    HomeOutlined: 'home',
    BankOutlined: 'bank',
    ShopOutlined: 'shop',
    ReadOutlined: 'read',
    MedicineBoxOutlined: 'medicinebox',
    CommentOutlined: 'comment',
    ChromeOutlined: 'chrome',
    DribbbleOutlined: 'dribbble',
    SlackOutlined: 'slack',
    MailOutlined: 'mail',
    LaptopOutlined: 'laptop',
    PhoneOutlined: 'phone',
    WhatsAppOutlined: 'whatsapp',
    AlertOutlined: 'alert',
    DashboardOutlined: 'dashboard',
    SettingOutlined: 'setting',
    StarOutlined: 'star',
    TrademarkOutlined: 'trademark',
    SoundOutlined: 'sound',
    LockOutlined: 'lock',
};

export const GREMLIN_EXECUTES_MODE = {
    QUERY: 'query',
    TASK: 'task',
};

export const GRAPH_STATUS = {
    STANDBY: 'standby',
    LOADING: 'loading',
    SUCCESS: 'success',
    FAILED: 'failed',
    UPLOAD_FAILED: 'uploadFailed',
};


export const ANALYSIS_TYPE = {
    GREMLIN: 'Gremlin',
    ALGORITHM: 'Algorithms',
    CYPHER: 'Cypher',
    ASYNC_CYPHER: 'Async_Cypher',
    ASYNC_GREMLIN: 'Async_Gremlin',
};

export const EDGE_TYPE = [
    'line',
    'runningLine',
    'quadratic',
    'runningQuadratic',
    'loop',
    'runningLoop',
];

export const PANEL_TYPE = {
    CLOSED: 0,
    LAYOUT: 1,
    SETTING: 2,
    STATISTICS: 3,
};

export const GRAPH_LOAD_STATUS = {
    CREATED: 'created',
    LOADING: 'loading',
    LOADED: 'loaded',
    ERROR: 'error',
};

export const EDGELABEL_TYPE = {
    PARENT: 'PARENT',
    SUB: 'SUB',
    NORMAL: 'NORMAL',
};

export const EDGELABEL_TYPE_NAME = {
    PARENT: '父边',
    SUB: '子边',
    NORMAL: '普通边',
};

export const Async_Task_Type = {
    '': '全部',
    gremlin: 'Gremlin任务',
    'computer-dis': '算法任务',
    remove_schema: '删除元数据',
    create_index: '创建索引',
    rebuild_index: '重建索引',
    cypher: 'Cypher任务',
    'vermeer-task:load': 'vermeer图加载任务',
    'vermeer-task:compute': 'vermeer图计算任务',
};

export const Async_Taskt_Status = {
    UNKNOWN: 'UNKNOW',
    NEW: 'new',
    SCHEDULING: 'scheduling',
    SCHEDULED: 'scheduled',
    QUEUED: 'queued',
    RUNNING: 'running',
    RESTORING: 'restoring',
    SUCCESS: 'success',
    FAILED: 'failed',
    CANCELLED: 'cancelled',
    CANCELLING: 'cancelling',
    HANGING: 'hanging',
    PENDING: 'pending',
    DELETING: 'deleting',
};

export const Async_Taskt_Status_Name = {
    '': '全部',
    UNKNOWN: '未知',
    new: '初始化',
    scheduling: '调度中',
    scheduled: '已调度',
    queued: '排队中',
    running: '运行中',
    restoring: '恢复中',
    success: '成功',
    failed: '失败',
    cancelled: '已取消',
    cancelling: '取消中',
    hanging: '挂起',
    pending: '挂起',
    deleting: '删除中',
};

export const Filter_Task_Status = {
    '': '全部',
    scheduling: '调度中',
    queued: '排队中',
    running: '运行中',
    restoring: '恢复中',
    success: '成功',
    failed: '失败',
    cancelled: '已取消',
};

export const Async_Task_Manipulations = {
    abort: '终止',
    aborting: '终止中',
    delete: '删除',
    check_result: '查看结果',
    check_reason: '查看原因',
};

export const Status_Color = {
    new: 'geekblue',
    scheduling: 'geekblue',
    scheduled: 'geekblue',
    queued: 'geekblue',
    running: 'geekblue',
    restoring: 'geekblue',
    deleting: 'geekblue',
    hanging: 'geekblue',
    success: 'green',
    cancelling: 'green',
    cancelled: 'green',
    async_task_success: 'green',
    failed: 'red',
    async_task_failed: 'red',
    UNKNOWN: 'orange',
};

export const GRAPH_ANALYSIS_MODULE = {
    GREMLIN: 'gremlin',
    ALGORITHMS: 'algorithms',
    ASYNCTASKS: 'asyncTasks',
};

export const FAVORITE_TYPE = {
    Gremlin: 'GREMLIN',
    Algorithms: 'ALGORITHM',
    Cypher: 'CYPHER',
};

export const EXECUTION_LOGS_TYPE = {
    Gremlin: '0',
    Algorithms: '1',
    Cypher: '2',
    Async_Cypher: '4',
    Async_Gremlin: '5',
};

export const SUPPORTED_LAYOUT_TYPE = {
    FORCE: 'force',
    CIRCULAR: 'circular',
    CONCENTRIC: 'concentric',
    DAGRE: 'dagre',
    CUSTOMGRID: 'customGrid',
    RADIAL: 'radial',
};

export const GRAPH_RENDER_MODE = {
    CANVAS2D: '2D模式',
    CANVAS3D: '3D模式',
};

export const MENUBAR_TOOLTIPS_2D = {
    IMPORT: '清空画布后再导入',
    EXPORT: '空图无法导出',
    STYLE: '空图无法设置外观',
    FILTER: '空图无法筛选',
    LAYOUT: '空图无法布局',
    SETTING: '空图无法设置',
    NEW: '空图无法新建',
    STATISTICS: '空图无法统计',
    SWITCH: '空图无法切换模式',
};

export const MENUBAR_TOOLTIPS_3D = {
    IMPORT: '3D模式不可以导入',
    EXPORT: '3D模式无法导出',
    STYLE: '3D模式无法外观设置',
    FILTER: '3D模式无法筛选',
    LAYOUT: '3D模式无法布局',
    SETTING: '3D模式无法设置',
    NEW: '3D模式无法新建',
    STATISTICS: '3D模式无法统计',
};

// TODO TRANSLATE NAME
export const ALGORITHM_NAME = {
    PAGE_RANK: 'PageRank',
    WEAKLY_CONNECTED_COMPONENT: 'Weakly Connected Component',
    DEGREE_CENTRALIT: 'Degree Centrality',
    CLOSENESS_CENTRALITY: 'Closeness Centrality',
    TRIANGLE_COUNT: 'Triangle Count',
    K_NEIGHBOR: 'K-neighbor（GET，基础版）',
    K_OUT: 'K-out API（GET，基础版）',
    SAME_NEIGHBORS: 'Same Neighbors',
    RINGS: 'Rings',
    SHORTEST_PATH: 'Shortest Path',
    ALLPATHS: '查找所有路径（POST，高级版）',
    JACCARD_SIMILARITY: 'Jaccard Similarity（GET）',
    CROSSPOINTS: 'Crosspoints',
    RINGS_DETECTION: 'Rings Detection',
    FILTERED_RINGS_DETECTION: 'Filtered Rings Detection',
    LINKS: 'Links',
    CLUSTER_COEFFICIENT: 'Cluster Coefficient',
    BETWEENNESS_CENTRALITY: 'Betweenness Centrality',
    LABEL_PROPAGATION_ALGORITHM: 'Label Propagation Algorithm',
    LOUVAIN: 'Louvain',
    FILTER_SUBGRAPH_MATCHING: 'Filter SubGraph Matching',
    K_CORE: 'K-Core',
    PERSONAL_PAGE_RANK: 'PersonalPageRank',
    KOUT_POST: 'K-out API(POST, 高级版)',
    KNEIGHBOR_POST: 'K-neighbor API（POST，高级版）',
    JACCARD_SIMILARITY_POST: 'Jaccard Similarity（POST）',
    RANK_API: 'rank API',
    NEIGHBOR_RANK_API: 'Neighbor Rank API',
    FINDSHORTESTPATH: '查找最短路径',
    FINDSHORTESTPATHWITHWEIGHT: '查找带权重的最短路径',
    SINGLESOURCESHORTESTPATH: '(从一个顶点出发)查找最短路径',
    MULTINODESSHORTESTPATH: '(指定顶点集)查找最短路径',
    CUSTOMIZEDPATHS: '自定义路径查询',
    TEMPLATEPATHS: '模版路径查询',
    CUSTOMIZED_CROSSPOINTS: 'Customized Crosspoints',
    RAYS: 'Rays',
    PATHS: '查找所有路径（GET，基础版）',
    FUSIFORM_SIMILARITY: 'Fusiform Similarity',
    ADAMIC_ADAR: 'Adamic Adar',
    RESOURCE_ALLOCATION: 'Resource Allocation',
    SAME_NEIGHBORS_BATCH: 'Same Neighbors Batch',
    EGONET: 'Egonet',
    SSSP: 'SSSP（单元最短路径）',
};

export const Algorithm_Layout = {
    [ALGORITHM_NAME.K_OUT]: 'radial',
    [ALGORITHM_NAME.K_NEIGHBOR]: 'radial',
    [ALGORITHM_NAME.SAME_NEIGHBORS]: 'relationship',
    [ALGORITHM_NAME.RINGS]: 'force',
    [ALGORITHM_NAME.SHORTEST_PATH]: 'relationship',
    [ALGORITHM_NAME.ALLPATHS]: 'relationship',
    [ALGORITHM_NAME.KOUT_POST]: 'force',
    [ALGORITHM_NAME.KNEIGHBOR_POST]: 'force',
    [ALGORITHM_NAME.FINDSHORTESTPATH]: 'force',
    [ALGORITHM_NAME.FINDSHORTESTPATHWITHWEIGHT]: 'force',
    [ALGORITHM_NAME.SINGLESOURCESHORTESTPATH]: 'force',
    [ALGORITHM_NAME.MULTINODESSHORTESTPATH]: 'relationship',
    [ALGORITHM_NAME.CUSTOMIZEDPATHS]: 'forceAtlas',
    [ALGORITHM_NAME.TEMPLATEPATHS]: 'forceAtlas',
    [ALGORITHM_NAME.CROSSPOINTS]: 'grid',
    [ALGORITHM_NAME.CUSTOMIZED_CROSSPOINTS]: 'relationship',
    [ALGORITHM_NAME.RAYS]: 'forceAtlas',
    [ALGORITHM_NAME.FUSIFORM_SIMILARITY]: 'force',
    [ALGORITHM_NAME.SAME_NEIGHBORS_BATCH]: 'relationship',
    [ALGORITHM_NAME.EGONET]: 'force',
    [ALGORITHM_NAME.PATHS]: 'force',
};

export const Algorithm_Url = {
    [ALGORITHM_NAME.K_OUT]: 'kout',
    [ALGORITHM_NAME.K_NEIGHBOR]: 'kneighbor',
    [ALGORITHM_NAME.SAME_NEIGHBORS]: 'sameNeighbors',
    [ALGORITHM_NAME.SHORTEST_PATH]: 'shortestPath',
    [ALGORITHM_NAME.RINGS]: 'rings',
    [ALGORITHM_NAME.ALLPATHS]: 'advancedPaths',
    [ALGORITHM_NAME.JACCARD_SIMILARITY]: 'jaccardSimilarity',
    [ALGORITHM_NAME.KOUT_POST]: 'kout_post',
    [ALGORITHM_NAME.KNEIGHBOR_POST]: 'kneighbor_post',
    [ALGORITHM_NAME.RANK_API]: 'personalrank',
    [ALGORITHM_NAME.NEIGHBOR_RANK_API]: 'neighborrank',
    [ALGORITHM_NAME.JACCARD_SIMILARITY_POST]: 'jaccardSimilarity_post',
    [ALGORITHM_NAME.FINDSHORTESTPATH]: 'allshortestpaths',
    [ALGORITHM_NAME.FINDSHORTESTPATHWITHWEIGHT]: 'weightedshortestpath',
    [ALGORITHM_NAME.SINGLESOURCESHORTESTPATH]: 'singlesourceshortestpath',
    [ALGORITHM_NAME.MULTINODESSHORTESTPATH]: 'multinodeshortestpath',
    [ALGORITHM_NAME.CUSTOMIZEDPATHS]: 'customizedpaths',
    [ALGORITHM_NAME.TEMPLATEPATHS]: 'templatepaths',
    [ALGORITHM_NAME.CROSSPOINTS]: 'crosspoints',
    [ALGORITHM_NAME.CUSTOMIZED_CROSSPOINTS]: 'customizedcrosspoints',
    [ALGORITHM_NAME.RAYS]: 'rays',
    [ALGORITHM_NAME.RANK_API]: 'personalrank',
    [ALGORITHM_NAME.FUSIFORM_SIMILARITY]: 'fusiformsimilarity',
    [ALGORITHM_NAME.ADAMIC_ADAR]: 'adamicadar',
    [ALGORITHM_NAME.RESOURCE_ALLOCATION]: 'resourceallocation',
    [ALGORITHM_NAME.SAME_NEIGHBORS_BATCH]: 'sameneighborsbatch',
    [ALGORITHM_NAME.EGONET]: 'egonet',
    [ALGORITHM_NAME.PATHS]: 'paths',
};
//



export const TEXT_PATH = {
    COMMON_VERIFY: 'common.verify',
    COMMON_STATUS: 'common.status',
    ALGORITHM: 'analysis.algorithm',
    OLAP: 'analysis.algorithm.olap',
    ALGORITHM_COMMON: 'analysis.algorithm.common',
};

export const useTranslatedConstants = () => {
    const {t} = useTranslation();
    const boolOptions = [
        {label: t('common.verify.yes'), value: 1},
        {label: t('common.verify.no'), value: 0},
    ];
    const directionOptions = [
        {label: t('ERView.edge.out'), value: 'out'},
        {label: t('ERView.edge.in'), value: 'in'},
        {label: t('ERView.edge.both'), value: 'both'},
    ];
    // TODO Tranfer directionOptions to here
    const ALGO_NAME_BASE_PATH = 'analysis.algorithm.olap.item';

    const ALGORITHM_NAME = {
        PAGE_RANK: t(`${ALGO_NAME_BASE_PATH}.PAGE_RANK`),
        WEAKLY_CONNECTED_COMPONENT: t(`${ALGO_NAME_BASE_PATH}.WEAKLY_CONNECTED_COMPONENT`),
        DEGREE_CENTRALIT: t(`${ALGO_NAME_BASE_PATH}.DEGREE_CENTRALIT`),
        CLOSENESS_CENTRALITY: t(`${ALGO_NAME_BASE_PATH}.CLOSENESS_CENTRALITY`),
        TRIANGLE_COUNT: t(`${ALGO_NAME_BASE_PATH}.TRIANGLE_COUNT`),
        K_NEIGHBOR: t(`${ALGO_NAME_BASE_PATH}.K_NEIGHBOR`),
        K_OUT: t(`${ALGO_NAME_BASE_PATH}.K_OUT`),
        SAME_NEIGHBORS: t(`${ALGO_NAME_BASE_PATH}.SAME_NEIGHBORS`),
        RINGS: t(`${ALGO_NAME_BASE_PATH}.RINGS`),
        SHORTEST_PATH: t(`${ALGO_NAME_BASE_PATH}.SHORTEST_PATH`),
        ALLPATHS: t(`${ALGO_NAME_BASE_PATH}.ALLPATHS`),
        JACCARD_SIMILARITY: t(`${ALGO_NAME_BASE_PATH}.JACCARD_SIMILARITY`),
        CROSSPOINTS: t(`${ALGO_NAME_BASE_PATH}.CROSSPOINTS`),
        RINGS_DETECTION: t(`${ALGO_NAME_BASE_PATH}.RINGS_DETECTION`),
        FILTERED_RINGS_DETECTION: t(`${ALGO_NAME_BASE_PATH}.FILTERED_RINGS_DETECTION`),
        LINKS: t(`${ALGO_NAME_BASE_PATH}.LINKS`),
        CLUSTER_COEFFICIENT: t(`${ALGO_NAME_BASE_PATH}.CLUSTER_COEFFICIENT`),
        BETWEENNESS_CENTRALITY: t(`${ALGO_NAME_BASE_PATH}.BETWEENNESS_CENTRALITY`),
        LABEL_PROPAGATION_ALGORITHM: t(`${ALGO_NAME_BASE_PATH}.LABEL_PROPAGATION_ALGORITHM`),
        LOUVAIN: t(`${ALGO_NAME_BASE_PATH}.LOUVAIN`),
        FILTER_SUBGRAPH_MATCHING: t(`${ALGO_NAME_BASE_PATH}.FILTER_SUBGRAPH_MATCHING`),
        K_CORE: t(`${ALGO_NAME_BASE_PATH}.K_CORE`),
        PERSONAL_PAGE_RANK: t(`${ALGO_NAME_BASE_PATH}.PERSONAL_PAGE_RANK`),
        KOUT_POST: t(`${ALGO_NAME_BASE_PATH}.KOUT_POST`),
        KNEIGHBOR_POST: t(`${ALGO_NAME_BASE_PATH}.KNEIGHBOR_POST`),
        JACCARD_SIMILARITY_POST: t(`${ALGO_NAME_BASE_PATH}.JACCARD_SIMILARITY_POST`),
        RANK_API: t(`${ALGO_NAME_BASE_PATH}.RANK_API`),
        NEIGHBOR_RANK_API: t(`${ALGO_NAME_BASE_PATH}.NEIGHBOR_RANK_API`),
        FINDSHORTESTPATH: t(`${ALGO_NAME_BASE_PATH}.FINDSHORTESTPATH`),
        FINDSHORTESTPATHWITHWEIGHT: t(`${ALGO_NAME_BASE_PATH}.FINDSHORTESTPATHWITHWEIGHT`),
        SINGLESOURCESHORTESTPATH: t(`${ALGO_NAME_BASE_PATH}.SINGLESOURCESHORTESTPATH`),
        MULTINODESSHORTESTPATH: t(`${ALGO_NAME_BASE_PATH}.MULTINODESSHORTESTPATH`),
        CUSTOMIZEDPATHS: t(`${ALGO_NAME_BASE_PATH}.CUSTOMIZEDPATHS`),
        TEMPLATEPATHS: t(`${ALGO_NAME_BASE_PATH}.TEMPLATEPATHS`),
        CUSTOMIZED_CROSSPOINTS: t(`${ALGO_NAME_BASE_PATH}.CUSTOMIZED_CROSSPOINTS`),
        RAYS: t(`${ALGO_NAME_BASE_PATH}.RAYS`),
        PATHS: t(`${ALGO_NAME_BASE_PATH}.PATHS`),
        FUSIFORM_SIMILARITY: t(`${ALGO_NAME_BASE_PATH}.FUSIFORM_SIMILARITY`),
        ADAMIC_ADAR: t(`${ALGO_NAME_BASE_PATH}.ADAMIC_ADAR`),
        RESOURCE_ALLOCATION: t(`${ALGO_NAME_BASE_PATH}.RESOURCE_ALLOCATION`),
        SAME_NEIGHBORS_BATCH: t(`${ALGO_NAME_BASE_PATH}.SAME_NEIGHBORS_BATCH`),
        EGONET: t(`${ALGO_NAME_BASE_PATH}.EGONET`),
        SSSP: t(`${ALGO_NAME_BASE_PATH}.SSSP`),
    };

    const Algorithm_Layout = {
        [ALGORITHM_NAME.K_OUT]: 'radial',
        [ALGORITHM_NAME.K_NEIGHBOR]: 'radial',
        [ALGORITHM_NAME.SAME_NEIGHBORS]: 'relationship',
        [ALGORITHM_NAME.RINGS]: 'force',
        [ALGORITHM_NAME.SHORTEST_PATH]: 'relationship',
        [ALGORITHM_NAME.ALLPATHS]: 'relationship',
        [ALGORITHM_NAME.KOUT_POST]: 'force',
        [ALGORITHM_NAME.KNEIGHBOR_POST]: 'force',
        [ALGORITHM_NAME.FINDSHORTESTPATH]: 'force',
        [ALGORITHM_NAME.FINDSHORTESTPATHWITHWEIGHT]: 'force',
        [ALGORITHM_NAME.SINGLESOURCESHORTESTPATH]: 'force',
        [ALGORITHM_NAME.MULTINODESSHORTESTPATH]: 'relationship',
        [ALGORITHM_NAME.CUSTOMIZEDPATHS]: 'forceAtlas',
        [ALGORITHM_NAME.TEMPLATEPATHS]: 'forceAtlas',
        [ALGORITHM_NAME.CROSSPOINTS]: 'grid',
        [ALGORITHM_NAME.CUSTOMIZED_CROSSPOINTS]: 'relationship',
        [ALGORITHM_NAME.RAYS]: 'forceAtlas',
        [ALGORITHM_NAME.FUSIFORM_SIMILARITY]: 'force',
        [ALGORITHM_NAME.SAME_NEIGHBORS_BATCH]: 'relationship',
        [ALGORITHM_NAME.EGONET]: 'force',
        [ALGORITHM_NAME.PATHS]: 'force',
    };

    const Algorithm_Url = {
        [ALGORITHM_NAME.K_OUT]: 'kout',
        [ALGORITHM_NAME.K_NEIGHBOR]: 'kneighbor',
        [ALGORITHM_NAME.SAME_NEIGHBORS]: 'sameNeighbors',
        [ALGORITHM_NAME.SHORTEST_PATH]: 'shortestPath',
        [ALGORITHM_NAME.RINGS]: 'rings',
        [ALGORITHM_NAME.ALLPATHS]: 'advancedPaths',
        [ALGORITHM_NAME.JACCARD_SIMILARITY]: 'jaccardSimilarity',
        [ALGORITHM_NAME.KOUT_POST]: 'kout_post',
        [ALGORITHM_NAME.KNEIGHBOR_POST]: 'kneighbor_post',
        [ALGORITHM_NAME.RANK_API]: 'personalrank',
        [ALGORITHM_NAME.NEIGHBOR_RANK_API]: 'neighborrank',
        [ALGORITHM_NAME.JACCARD_SIMILARITY_POST]: 'jaccardSimilarity_post',
        [ALGORITHM_NAME.FINDSHORTESTPATH]: 'allshortestpaths',
        [ALGORITHM_NAME.FINDSHORTESTPATHWITHWEIGHT]: 'weightedshortestpath',
        [ALGORITHM_NAME.SINGLESOURCESHORTESTPATH]: 'singlesourceshortestpath',
        [ALGORITHM_NAME.MULTINODESSHORTESTPATH]: 'multinodeshortestpath',
        [ALGORITHM_NAME.CUSTOMIZEDPATHS]: 'customizedpaths',
        [ALGORITHM_NAME.TEMPLATEPATHS]: 'templatepaths',
        [ALGORITHM_NAME.CROSSPOINTS]: 'crosspoints',
        [ALGORITHM_NAME.CUSTOMIZED_CROSSPOINTS]: 'customizedcrosspoints',
        [ALGORITHM_NAME.RAYS]: 'rays',
        [ALGORITHM_NAME.RANK_API]: 'personalrank',
        [ALGORITHM_NAME.FUSIFORM_SIMILARITY]: 'fusiformsimilarity',
        [ALGORITHM_NAME.ADAMIC_ADAR]: 'adamicadar',
        [ALGORITHM_NAME.RESOURCE_ALLOCATION]: 'resourceallocation',
        [ALGORITHM_NAME.SAME_NEIGHBORS_BATCH]: 'sameneighborsbatch',
        [ALGORITHM_NAME.EGONET]: 'egonet',
        [ALGORITHM_NAME.PATHS]: 'paths',
    };
    const ALGORITHM_MODE = {
        OLTP: t(TEXT_PATH.ALGORITHM + '.mode.OLTP'),
        OLAP: t(TEXT_PATH.ALGORITHM + '.mode.OLAP'),
    };
    return {
        boolOptions,
        directionOptions,
        ALGORITHM_NAME,
        Algorithm_Layout,
        Algorithm_Url,
        ALGORITHM_MODE,
    };
};
