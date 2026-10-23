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
import {Button} from 'antd';
import GraphAnalysisContext from '../../../../Context';
import Graph from '../../../../component/Graph';
import Legend from '../../../../component/Legend';
import Menu from '../../../../component/Menu';
import Search from '../../../../component/Search';
import DynamicAddNode from '../../../../component/DynamicAddNode';
import DynamicAddEdge from '../../../../component/DynamicAddEdge';
import LayoutConfigPanel from '../../../../component/layoutConfigPanel/Home';
import SettingConfigPanel from '../../../../component/SettingConfigPanel';
import PanelControlButton from '../../../../component/ClosePanelButton';
import Canvas3D from '../../../../component/Canvas3D';
import StatisticPanel from '../../../../component/StatisticsPanel/Home';
import MiniMap from '../../../../component/MiniMap';
import NumberCard from '../../../../component/NumberCard';
import GraphStatusView from '../../../../component/GraphStatusView';
import TaskNavigateView from '../../../../component/TaskNavigateView';
import EditElement from '../../../../component/EditElement';
import GraphToolBar from '../GraphToolBar';
import GraphMenuBar from '../GraphMenubar';
import {GRAPH_STATUS, PANEL_TYPE, GRAPH_RENDER_MODE} from '../../../../../utils/constants';

import {shouldKeepGraphCanvas} from '../../../../component/Graph/data';
import {getQueryResultStandbyMessage} from '../../Home/utils';
import c from './index.module.scss';
import _ from 'lodash';
import {USER_CHANGE_EVENT} from '../../../../../utils/user';

const {STANDBY, LOADING, SUCCESS, FAILED, UPLOAD_FAILED} = GRAPH_STATUS;
const {CLOSED, LAYOUT, SETTING, STATISTICS} = PANEL_TYPE;
const {CANVAS2D} = GRAPH_RENDER_MODE;

import {useAnalysisGraph} from '../../../../component/Graph/useAnalysisGraph';
import {toCanvas3DData, toElementDetails} from '../../../../../utils/analysisGraphData';
const EMPTY_DATA = {vertices: [], edges: []};

const GraphResult = props => {
    const {t} = useTranslation();
    const {
        data = EMPTY_DATA,
        fitHeight = false,
        metaData,
        isQueryMode,
        queryStatus,
        queryMessage,
        queryProgress,
        previewNotice,
        asyncTaskId,
        resetGraphStatus,
        panelType,
        updatePanelType,
        propertyKeysRecords,
        graphNums,
        onRefreshGraphNums,
        graphRenderMode,
        onGraphRenderModeChange,
    } = props;
    const contentRef = useRef(null);

    useLayoutEffect(() => {
        if (!fitHeight) {
            return;
        }
        const content = contentRef.current;
        const updateHeight = () => {
            const height = content.getBoundingClientRect().height;
            content.style.setProperty('--analysis-result-height', `${height}px`);
        };
        updateHeight();
        const observer = new ResizeObserver(updateHeight);
        observer.observe(content);
        return () => observer.disconnect();
    }, [fitHeight]);

    const {edgeMeta, vertexMeta} = metaData || {};
    const graphSpaceInfo = useContext(GraphAnalysisContext);
    const {controller, graphData, currentRawData, clearGraph, styleConfigData, initialData, datasetKey, scopeKey, labelFields, layout, layoutSettings, defaultLayout, onReady: onGraphReady,
        changeStyles: handleGraphStyleChange, changeLayout: handleLayoutChange,
        changeSettings: handleSettingChange, exportPng: handleExportPng,
        editElement: onEditElementChange, expand: getExpandInfo, filter: handleFilterChange, addElements}
        = useAnalysisGraph({data, metaData, graphSpaceInfo, queryStatus});
    const data3D = useMemo(() => graphRenderMode === CANVAS2D ? null : toCanvas3DData(graphData, metaData),
        [graphData, graphRenderMode, metaData]);
    const handleClickGraphElement = useCallback(event => {
        setEditElementInfo(toElementDetails(event, metaData));
        setShowEditElement(true);
    }, [metaData]);


    const [showEditElement, setShowEditElement] = useState(false);
    const [showAddNodeDrawer, setShowAddNodeDrawer] = useState(false);
    const [showAddEdgeDrawer, setShowAddEdgeDrawer] = useState(false);
    const addEdgeData = useMemo(() => showAddEdgeDrawer && graphRenderMode === CANVAS2D
        ? {vertices: graphData.nodes.map(node => node.data.raw)} : null,
    [graphData, graphRenderMode, showAddEdgeDrawer]);
    const [editElementInfo, setEditElementInfo] = useState();
    const [isClickNew, setIsClickNew] = useState(false);
    const [addEdgeDrawerInfo, setAddEdgeDrawerInfo]  = useState({});
    const [isOutEdge, setOutEdge] = useState(false);
    const [searchVisible, setSearchVisible] = useState(false);
    const [searchVertex, setSearchVertex] = useState({});
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
    }, [panelType, updatePanelType]);

    const handleUpdateStatus = useCallback(
        (status, message, result) => {
            const uploadedData = {
                graph_view: result,
                json_view: {},
                table_view: {},
            };
            resetGraphStatus && resetGraphStatus(status, message, uploadedData);
        },
        [resetGraphStatus]
    );

    const handleClosePanel = useCallback(
        () => {
            handleUpdatePanel(CLOSED);
        },
        [handleUpdatePanel]
    );

    const handleClearGraph = useCallback(
        () => {
            layoutPanelRef.current?.cancel();
            resetGraphStatus && resetGraphStatus(STANDBY, undefined, {});
            updatePanelType(CLOSED);
        },
        [resetGraphStatus, updatePanelType]
    );

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
        },
        []
    );

    const closeAddEdgeDrawer = useCallback(
        () => {
            setShowAddEdgeDrawer(false);
        },
        []
    );

    const handleClickNewAddNode = useCallback(
        () => {
            setShowAddNodeDrawer(true);
        },
        []
    );

    const handleClickAddNode = useCallback(
        () => {
            setShowAddNodeDrawer(true);
        },
        []
    );

    const handleClickAddEdge = useCallback(
        (info, isOutEdge) => {
            setIsClickNew(false);
            setShowAddEdgeDrawer(true);
            setAddEdgeDrawerInfo(info);
            setOutEdge(isOutEdge);
        },
        []
    );

    const handleClickNewAddEdge = useCallback(
        isOut => {
            setIsClickNew(true);
            setShowAddEdgeDrawer(true);
            setOutEdge(isOut);
        }, []
    );

    const handleSwitchRenderMode = useCallback(
        value => {
            layoutPanelRef.current?.cancel();
            onGraphRenderModeChange(value);
            updatePanelType(CLOSED);

        },
        [onGraphRenderModeChange, updatePanelType]
    );

    const onCloseEditElement = useCallback(
        () => {
            setShowEditElement(false);
        },
        []
    );

    const handleSearch = useCallback(
        vertex => {
            setSearchVisible(true);
            setSearchVertex(vertex);
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

    const handledbClickNode = useCallback(event => {
        if (event.kind === 'node') return getExpandInfo({vertex_id: event.data.raw.id, vertex_label: event.data.raw.label});
    }, [getExpandInfo]);

    const handleClickMenuExpand = useCallback(params => getExpandInfo(params), [getExpandInfo]);

    const handleTogglePanel = useCallback(
        type => {
            handleUpdatePanel(panelType === type ? CLOSED : type);
        }, [handleUpdatePanel, panelType]
    );

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
            {renderRetainedProgress()}
            <Legend />
            <Menu
                onClickAddNode={handleClickAddNode}
                onClickAddEdge={handleClickAddEdge}
                onClickExpand={handleClickMenuExpand}
                onClickSearch={handleSearch}
            />
            <EditElement
                show={showEditElement}
                cancel={onCloseEditElement}
                drawerInfo={editElementInfo}
                onChange={onEditElementChange}
                edgeMeta={edgeMeta}
            />
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
            <Search
                open={searchVisible}
                onClose={handleCloseSearch}
                onChange={handleChangeSearch}
                propertykeys={propertyKeysRecords}
                {...searchVertex}
            />
            <GraphToolBar
                handleClearGraph={handleClearGraph}
                panelType={panelType}
                updatePanelType={handleUpdatePanel}
            />
            <MiniMap />
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

    // A terminal error wins over any progress object delivered by an older request.
    const activeProgress = isQueryMode && queryStatus !== FAILED && queryStatus !== UPLOAD_FAILED
        ? queryProgress : undefined;
    const hasGraphData = graphData.nodes.length + graphData.edges.length > 0;
    const renderQueryProgress = () => (
        <div className={c.queryProgress}>
            <span aria-live="off">
                {t('analysis.query.elapsed_seconds', {seconds: activeProgress.elapsedSeconds})}
            </span>
            <p>{t(activeProgress.phase === 'execution' ? 'analysis.query.execution_timeout_hint'
                : 'analysis.query.model_timeout_hint')}</p>
            {activeProgress.modelSlow && <p role="status">{t('analysis.query.model_slow')}</p>}
            <Button onClick={activeProgress.cancel}>{t('analysis.query.cancel_generation')}</Button>
        </div>
    );
    const renderRetainedProgress = () => activeProgress && hasGraphData ? (
        <aside className={`${c.retainedQueryProgress} ${panelType !== CLOSED ? c.progressWithPanel : ''}`}
            role="region" aria-label={t('analysis.query_result.loading')}>
            <div role="status">{t(`analysis.query.phase_${activeProgress.phase || 'waiting'}`)}</div>
            {renderQueryProgress()}
        </aside>
    ) : null;
    const renderGraphView = () => {
        if (activeProgress) {
            if (hasGraphData) return graphRenderMode === CANVAS2D ? renderCanvas2D() : renderCanvas3D();
            return <GraphStatusView status={LOADING}
                message={t(`analysis.query.phase_${activeProgress.phase || 'waiting'}`)}
                details={renderQueryProgress()} />;
        }
        switch (queryStatus) {
            case STANDBY:
                return (
                    <GraphStatusView
                        status={STANDBY}
                        message={getQueryResultStandbyMessage(t, isQueryMode)}
                    />
                );
            case LOADING:
                if (shouldKeepGraphCanvas(isQueryMode, queryStatus, graphData)) {
                    return graphRenderMode === CANVAS2D ? renderCanvas2D() : renderCanvas3D();
                }
                return (
                    <GraphStatusView
                        status={LOADING}
                        message={isQueryMode
                            ? t('analysis.query_result.loading')
                            : t('analysis.query_result.submitting_task')}
                    />
                );
            case FAILED:
                return (
                    <GraphStatusView
                        status={FAILED}
                        message={queryMessage || t('analysis.query_result.submit_failed')}
                    />
                );
            case UPLOAD_FAILED:
                return (
                    <GraphStatusView
                        status={UPLOAD_FAILED}
                        message={queryMessage || t('analysis.query_result.import_failed')}
                    />
                );
            case SUCCESS:
                if (isQueryMode) {
                    if (!showCanvasInfo) {
                        return (
                            <GraphStatusView
                                status={SUCCESS}
                                message={t('analysis.query_result.no_graph_result')}
                            />
                        );
                    }
                    return graphRenderMode === CANVAS2D ? renderCanvas2D() : renderCanvas3D();
                }
                return (
                    <TaskNavigateView message={t('analysis.query_result.submit_success')} taskId={asyncTaskId} />
                );
        }
    };

    return (
        <div className={`${c.graphResult} ${fitHeight ? c.fitHeight : ''}`}>
            <GraphMenuBar
                layout={layoutSettings}
                defaultLayout={defaultLayout}
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
            <div ref={contentRef} className={c.content}>
                {renderGraphView()}
                {previewNotice && <div className={c.previewNotice}>{previewNotice}</div>}
                {graphRenderMode !== CANVAS2D && renderRetainedProgress()}
            </div>
            <NumberCard
                hasPadding={panelType !== CLOSED}
                data={{
                    currentGraphNodesNum: graphData.nodes.length,
                    currentGraphEdgesNum: graphData.edges.length,
                    allGraphNodesNum: graphNums?.vertexCount,
                    allGraphEdgesNum: graphNums?.edgeCount,
                    approximate: graphNums?.approximate,
                    scan_limit: graphNums?.scan_limit,
                }}
            />
        </div>
    );
};

export default GraphResult;
