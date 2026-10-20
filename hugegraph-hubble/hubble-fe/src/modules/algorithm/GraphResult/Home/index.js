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
 * @file 图分析画布 Home
 */
import {useCallback, useEffect, useLayoutEffect, useState, useContext, useMemo, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {Empty, Space} from 'antd';
import {Link} from 'react-router-dom';
import GraphAnalysisContext from '../../../Context';
import Graph from '../../../component/Graph';
import Legend from '../../../component/Legend';
import MiniMap from '../../../component/MiniMap';
import GraphMenuBar from '../GraphMenuBar';
import GraphToolBar from '../GraphToolBar';
import Menu from '../../../component/Menu';
import NumberCard from '../../../component/NumberCard';
import EditElement from '../../../component/EditElement';
import Search from '../../../component/Search';
import SettingConfigPanel from '../../../component/SettingConfigPanel';
import LayoutConfigPanel from '../../../component/layoutConfigPanel/Home';
import PanelControlButton from '../../../component/ClosePanelButton';
import DynamicAddNode from '../../../component/DynamicAddNode';
import DynamicAddEdge from '../../../component/DynamicAddEdge';
import NeighborRankApiView from '../NeighborRankView';
import StatisticPanel from '../../../component/StatisticsPanel/Home';
import RankApiView from '../RankApiView';
import JaccView from '../JaccView';
import GraphStatusView from '../../../component/GraphStatusView';
import TaskNavigateView from '../../../component/TaskNavigateView';
import Canvas3D from '../../../component/Canvas3D';
import {
    GRAPH_STATUS, PANEL_TYPE, GRAPH_RENDER_MODE, ALGORITHM_NAME,
    getAlgorithmDisplayName, getCanonicalAlgorithmName,
} from '../../../../utils/constants';
import c from './index.module.scss';
import _ from 'lodash';
import {USER_CHANGE_EVENT} from '../../../../utils/user';

import {useAnalysisGraph} from '../../../component/Graph/useAnalysisGraph';
import {toCanvas3DData, toElementDetails} from '../../../../utils/analysisGraphData';
const EMPTY_DATA = {vertices: [], edges: []};

const GraphResult = props => {
    const {t} = useTranslation();
    const {
        data = EMPTY_DATA,
        metaData,
        options,
        asyncTaskId,
        queryStatus,
        queryMessage,
        isQueryMode,
        algorithm: algorithmName,
        resetGraphStatus,
        panelType,
        updatePanelType,
        graphNums,
        onRefreshGraphNums,
        propertyKeysRecords,
        graphRenderMode,
        onGraphRenderModeChange,
    } = props;
    const canonicalAlgorithmName = useMemo(
        () => getCanonicalAlgorithmName(algorithmName, t),
        [algorithmName, t]
    );
    const displayAlgorithmName = getAlgorithmDisplayName(algorithmName, t);

    const {STANDBY, LOADING, SUCCESS, FAILED, UPLOAD_FAILED} = GRAPH_STATUS;
    const {JACCARD_SIMILARITY, JACCARD_SIMILARITY_POST, RANK_API,
        NEIGHBOR_RANK_API, ADAMIC_ADAR, RESOURCE_ALLOCATION} = ALGORITHM_NAME;
    const noneGraphAlgorithm = [JACCARD_SIMILARITY, JACCARD_SIMILARITY_POST, RANK_API,
        NEIGHBOR_RANK_API, ADAMIC_ADAR, RESOURCE_ALLOCATION];
    const {CLOSED, LAYOUT, SETTING, STATISTICS} = PANEL_TYPE;
    const {CANVAS2D} = GRAPH_RENDER_MODE;


    const graphSpaceInfo = useContext(GraphAnalysisContext);
    const {controller, graphData, currentRawData, clearGraph, styleConfigData, initialData, datasetKey, scopeKey, labelFields, layout, layoutSettings, defaultLayout, onReady: onGraphReady,
        changeStyles: handleGraphStyleChange, changeLayout: handleLayoutChange,
        changeSettings: handleSettingChange, exportPng: handleExportPng,
        editElement: onEditElementChange, expand: getExpandInfo, filter: handleFilterChange, addElements}
        = useAnalysisGraph({data, metaData, graphSpaceInfo, queryStatus, options, algorithmName: canonicalAlgorithmName});
    const data3D = useMemo(() => graphRenderMode === CANVAS2D ? null : toCanvas3DData(graphData, metaData),
        [CANVAS2D, graphData, graphRenderMode, metaData]);
    const handleClickGraphElement = useCallback(event => {
        setEditElementInfo(toElementDetails(event, metaData));
        setShowEditElement(true);
    }, [metaData]);

    const {edgeMeta, vertexMeta} = metaData || {};
    const hasGraphData = Number(graphNums?.vertexCount) > 0
        || Number(graphNums?.edgeCount) > 0;
    const hasKnownEmptyCounts = graphNums?.vertexCount !== null && graphNums?.edgeCount !== null
        && Number(graphNums?.vertexCount) === 0
        && Number(graphNums?.edgeCount) === 0;
    const hasKnownEmptySchema = Array.isArray(vertexMeta) && Array.isArray(edgeMeta)
        && vertexMeta.length === 0 && edgeMeta.length === 0;
    const isEmptyGraph = !hasGraphData && (hasKnownEmptyCounts || hasKnownEmptySchema);
    const [showAddNodeDrawer, setShowAddNodeDrawer] = useState(false);
    const [showAddEdgeDrawer, setShowAddEdgeDrawer] = useState(false);
    const addEdgeData = useMemo(() => showAddEdgeDrawer && graphRenderMode === CANVAS2D
        ? {vertices: graphData.nodes.map(node => node.data.raw)} : null,
    [CANVAS2D, graphData, graphRenderMode, showAddEdgeDrawer]);
    const [isClickNew, setIsClickNew] = useState(false);
    const [searchVisible, setSearchVisible] = useState(false);
    const [searchVertex, setSearchVertex] = useState({});
    const [addEdgeDrawerInfo, setAddEdgeDrawerInfo]  = useState({});
    const [isOutEdge, setOutEdge] = useState(false);
    const [showEditElement, setShowEditElement] = useState(false);
    const [editElementInfo, setEditElementInfo] = useState();
    const {jaccardsimilarity, rankObj, rankArray} = options || {};
    const showCanvasInfo =  (_.size(data.vertices) !== 0 || _.size(data.edges) !== 0) && queryStatus === SUCCESS;

    const resetTargetDrawers = useCallback(() => {
        setShowEditElement(false);
        setEditElementInfo();
        setShowAddNodeDrawer(false);
        setShowAddEdgeDrawer(false);
        setAddEdgeDrawerInfo({});
        setIsClickNew(false);
        setOutEdge(false);
        setSearchVisible(false);
        setSearchVertex({});
    }, []);
    useLayoutEffect(() => {
        resetTargetDrawers();
    }, [graphSpaceInfo.graphSpace, graphSpaceInfo.graph, resetTargetDrawers]);
    useEffect(() => {
        window.addEventListener(USER_CHANGE_EVENT, resetTargetDrawers);
        return () => window.removeEventListener(USER_CHANGE_EVENT, resetTargetDrawers);
    }, [resetTargetDrawers]);

    const layoutPanelRef = useRef(null);
    const handleQuickLayoutChange = useCallback(selection => {
        layoutPanelRef.current?.cancel();
        handleLayoutChange(selection);
    }, [handleLayoutChange]);
    const handleUpdatePanel = useCallback(type => {
        if (panelType === LAYOUT) layoutPanelRef.current?.flush();
        updatePanelType(type);
    }, [LAYOUT, panelType, updatePanelType]);

    const handleUpdateStatus = useCallback(
        (status, message, result) => {
            resetGraphStatus && resetGraphStatus(status, message, result);
        },
        [resetGraphStatus]
    );

    const handleTogglePanel = useCallback(
        type => {
            handleUpdatePanel(panelType === type ? CLOSED : type);
        }, [CLOSED, handleUpdatePanel, panelType]
    );

    const handleClickNewAddNode = useCallback(
        () => {
            setShowAddNodeDrawer(true);
        }, []);

    const handleClickNewAddEdge = useCallback(
        isOut => {
            setIsClickNew(true);
            setShowAddEdgeDrawer(true);
            setOutEdge(isOut);
        }, []);

    const handledbClickNode = useCallback(event => {
        if (event.kind === 'node') return getExpandInfo({vertex_id: event.data.raw.id, vertex_label: event.data.raw.label});
    }, [getExpandInfo]);

    const handleAddElements = useCallback(async raw => {
        // The drawer has already saved on the server. Counts refresh even if
        // canvas application fails. The parent marks unavailable counts;
        // refresh failures must not change the write/apply outcome.
        void Promise.resolve().then(() => onRefreshGraphNums?.()).catch(() => {});
        const result = await addElements(raw);
        return result ?? {status: 'disposed'};
    }, [addElements, onRefreshGraphNums]);

    const closeAddNodeDrawer = useCallback(
        () => {
            setShowAddNodeDrawer(false);
        }, []
    );

    const closeAddEdgeDrawer = useCallback(
        () => {
            setShowAddEdgeDrawer(false);
        }, []
    );

    const handleClosePanel = useCallback(
        () => {
            handleUpdatePanel(CLOSED);
        },
        [CLOSED, handleUpdatePanel]
    );

    const handleClearGraph = useCallback(
        () => {
            layoutPanelRef.current?.cancel();
            resetGraphStatus && resetGraphStatus(STANDBY, undefined, {});
            updatePanelType(CLOSED);
        },
        [CLOSED, STANDBY, resetGraphStatus, updatePanelType]
    );

    const handleClickAddNode = useCallback(() => {
        setShowAddNodeDrawer(true);
    }, []);

    const handleClickAddEdge = useCallback(
        (info, isOutEdge) => {
            setIsClickNew(false);
            setShowAddEdgeDrawer(true);
            setAddEdgeDrawerInfo(info);
            setOutEdge(isOutEdge);
        },
        []
    );

    const handleClickMenuExpand = useCallback(params => getExpandInfo(params), [getExpandInfo]);

    const handleSearch = useCallback(
        vertex => {
            setSearchVisible(true);
            setSearchVertex(vertex);
        },
        []
    );

    const onCloseEditElement = useCallback(
        () => {
            setShowEditElement(false);
        },
        []
    );

    const handleCloseSearch = useCallback(
        () => {
            setSearchVisible(false);
        },
        []
    );

    const handleChangeSearch = useCallback(params => {
        getExpandInfo(params);
        handleCloseSearch();
    }, [getExpandInfo, handleCloseSearch]);
    const renderCanvas2D = () => (
        <Graph
            clearGraph={clearGraph}
            key={scopeKey}
            data={initialData}
            datasetKey={datasetKey}
            layout={layout}
            onReady={onGraphReady}
            onClick={handleClickGraphElement}
            onDoubleClick={handledbClickNode}
        >
            <MiniMap />
            <GraphToolBar
                handleClearGraph={handleClearGraph}
                panelType={panelType}
                updatePanelType={handleUpdatePanel}
            />
            <Menu
                onClickAddNode={handleClickAddNode}
                onClickAddEdge={handleClickAddEdge}
                onClickExpand={handleClickMenuExpand}
                onClickSearch={handleSearch}
            />
            <Legend />
            <DynamicAddNode
                open={showAddNodeDrawer}
                onOK={handleAddElements}
                onCancel={closeAddNodeDrawer}
                drawerInfo={vertexMeta}
            />
            <DynamicAddEdge
                open={showAddEdgeDrawer}
                onCancel={closeAddEdgeDrawer}
                onOk={handleAddElements}
                graphData={addEdgeData}
                drawerInfo={addEdgeDrawerInfo}
                isClickNew={isClickNew}
                isOutEdge={isOutEdge}
            />
            <PanelControlButton show={panelType !== CLOSED} onClick={handleClosePanel} />
            <LayoutConfigPanel
                ref={layoutPanelRef}
                layout={layoutSettings}
                data={graphData}
                onChange={handleLayoutChange}
                open={panelType === LAYOUT}
            />
            <SettingConfigPanel
                key={datasetKey}
                selectedFields={labelFields}
                renderMode={graphRenderMode}
                onRenderModeChange={handleSwitchRenderMode}
                data={graphData}
                metaData={metaData}
                onChange={handleSettingChange}
                open={panelType === SETTING}
            />
            <EditElement
                show={showEditElement}
                cancel={onCloseEditElement}
                drawerInfo={editElementInfo}
                edgeMeta={edgeMeta}
                onChange={onEditElementChange}
            />
            <Search
                open={searchVisible}
                onClose={handleCloseSearch}
                onChange={handleChangeSearch}
                propertykeys={propertyKeysRecords}
                {...searchVertex}
            />
            <StatisticPanel
                open={panelType === STATISTICS}
                graphDataNums={{nodesNum: graphData.nodes.length, edgesNum: graphData.edges.length}}
                statistics={data?.statistics || {}}
            />
        </Graph>
    );

    const renderCanvas3D = () => (
        <Canvas3D data={data3D} onExit={() => handleSwitchRenderMode(CANVAS2D)}>
            <SettingConfigPanel open={panelType === SETTING} renderMode={graphRenderMode}
                onRenderModeChange={handleSwitchRenderMode} />
            <PanelControlButton show={panelType === SETTING} onClick={() => updatePanelType(CLOSED)} />
        </Canvas3D>
    );

    const statusMessage = useMemo(() => ({
        [STANDBY]: t('analysis.query_result.no_data'),
        [LOADING]: t('analysis.query_result.loading'),
        [FAILED]: queryMessage || t('analysis.query_result.run_failed'),
        [UPLOAD_FAILED]: queryMessage || t('analysis.query_result.import_failed'),
    }), [LOADING, FAILED, STANDBY, UPLOAD_FAILED, queryMessage, t]);

    const renderMainContent = () => {
        if (queryStatus === STANDBY && isEmptyGraph) {
            return (
                <Empty
                    className={c.emptyGraph}
                    description={(
                        <div>
                            <strong>{t('analysis.algorithm.empty_graph_title')}</strong>
                            <p>{t('analysis.algorithm.empty_graph_description')}</p>
                        </div>
                    )}
                >
                    <Space>
                        <Link
                            to={`/graphspace/${graphSpaceInfo.graphSpace}/graph/`
                                + `${graphSpaceInfo.graph}/meta`}
                        >
                            {t('analysis.algorithm.create_schema')}
                        </Link>
                        <Link to='/source'>
                            {t('analysis.algorithm.prepare_data')}
                        </Link>
                    </Space>
                </Empty>
            );
        }
        if (queryStatus === LOADING && graphData.nodes.length + graphData.edges.length > 0) {
            return graphRenderMode === CANVAS2D ? renderCanvas2D() : renderCanvas3D();
        }
        if (queryStatus === SUCCESS) {
            if (!isQueryMode) {
                return (
                    <TaskNavigateView
                        message={t('analysis.algorithm.task_submit_success', {
                            name: displayAlgorithmName,
                        })}
                        taskId={asyncTaskId}
                    />
                );
            }
            if (!showCanvasInfo && !noneGraphAlgorithm.includes(canonicalAlgorithmName)) {
                return (
                    <GraphStatusView
                        status={SUCCESS}
                        message={t('analysis.algorithm.result.no_graph_result')}
                    />
                );
            }
            switch (canonicalAlgorithmName) {
                case JACCARD_SIMILARITY:
                case ADAMIC_ADAR:
                case RESOURCE_ALLOCATION:
                    return <JaccView jaccardsimilarity={jaccardsimilarity} />;
                case JACCARD_SIMILARITY_POST:
                case RANK_API:
                    return <RankApiView rankObj={rankObj} />;
                case NEIGHBOR_RANK_API:
                    return <NeighborRankApiView rankArray={rankArray} />;
            };
            return graphRenderMode === CANVAS2D ? renderCanvas2D() : renderCanvas3D();
        }
        return <GraphStatusView status={queryStatus} message={statusMessage[queryStatus]} />;
    };

    const handleSwitchRenderMode = useCallback(
        value => {
            layoutPanelRef.current?.cancel();
            onGraphRenderModeChange(value);
            updatePanelType(CLOSED);
        },
        [CLOSED, onGraphRenderModeChange, updatePanelType]
    );

    return (
        <div className={c.graphResult}>
            <GraphMenuBar
                layout={layoutSettings}
                defaultLayout={defaultLayout}
                startId={options?.startId}
                layoutReady={controller?.getSnapshot().status === 'ready'}
                handleQuickLayoutChange={handleQuickLayoutChange}
                styleConfigData={styleConfigData}
                graphData={graphData}
                exportData={currentRawData}
                handleImportData={handleUpdateStatus}
                handleExportPng={handleExportPng}
                handleGraphStyleChange={handleGraphStyleChange}
                handleFilterChange={handleFilterChange}
                handleTogglePanel={handleTogglePanel}
                handleClickNewAddNode={handleClickNewAddNode}
                handleClickNewAddEdge={handleClickNewAddEdge}
                showCanvasInfo={showCanvasInfo}
                graphRenderMode={graphRenderMode}
            />
            {renderMainContent()}
            <NumberCard
                hasPadding={panelType !== CLOSED}
                data={{
                    currentGraphNodesNum: graphData.nodes.length,
                    currentGraphEdgesNum: graphData.edges.length,
                    allGraphNodesNum: graphNums?.vertexCount,
                    allGraphEdgesNum: graphNums?.edgeCount,
                }}
            />
        </div>
    );
};

export default GraphResult;
