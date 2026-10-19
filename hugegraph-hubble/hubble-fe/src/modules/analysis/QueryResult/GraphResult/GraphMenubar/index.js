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
 * @file  GraphMenuBar(图分析)
 */

import {useCallback} from 'react';
import {useTranslation} from 'react-i18next';
import MenuBar from '../../../../component/MenuBar';
import QuickLayoutSwitcher from '../../../../component/QuickLayoutSwitcher';
import ImportData from '../../../../component/ImportData';
import ExportData from '../../../../component/ExportData';
import StyleConfig from '../../../../component/styleConfig/Home';
import FilterHome from '../../../../component/filter/Home';
import LayoutConfig from '../../../../component/LayoutConfig';
import SettingConfig from '../../../../component/SettingConfig';
import NewConfig from '../../../../component/NewConfig';
import Statistics from '../../../../component/Statistics';
import {PANEL_TYPE, GRAPH_RENDER_MODE} from '../../../../../utils/constants';
import {exportHubbleGraph} from '../../../../../visualization/hubble/adapter';
import useDownloadJson from '../../../../../customHook/useDownloadJson';

const {LAYOUT, SETTING, STATISTICS} = PANEL_TYPE;
const {CANVAS2D} = GRAPH_RENDER_MODE;

const GraphMenuBar = props => {
    const {t} = useTranslation();
    const {
        styleConfigData,
        graphData,
        layout,
        defaultLayout,
        startId,
        layoutReady,
        handleQuickLayoutChange,
        exportData,
        handleImportData,
        handleExportPng,
        handleGraphStyleChange,
        handleFilterChange,
        handleTogglePanel,
        handleClickNewAddNode,
        handleClickNewAddEdge,
        showCanvasInfo,
        graphRenderMode,
    } = props;

    const isCanvas2D = graphRenderMode === CANVAS2D;
    const buttonEnableForCanvas2D = showCanvasInfo && isCanvas2D;
    const buttonEnableForImport = !showCanvasInfo && isCanvas2D;

    const {downloadJsonHandler} = useDownloadJson();

    const handleExportJson = useCallback(
        fileName => {
            downloadJsonHandler(fileName, exportData ? exportData() : exportHubbleGraph(graphData));
        },
        [downloadJsonHandler, exportData, graphData]
    );

    const handleLayoutPanel = useCallback(
        () => handleTogglePanel(LAYOUT),
        [handleTogglePanel]
    );
    const handleSettingPanel = useCallback(
        () => handleTogglePanel(SETTING),
        [handleTogglePanel]
    );
    const handleStatisticsPanel = useCallback(
        () => handleTogglePanel(STATISTICS),
        [handleTogglePanel]
    );

    const menubarContent = [
        {
            key: 1,
            content: (
                <ImportData
                    buttonEnable={buttonEnableForImport}
                    onUploadChange={handleImportData}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.import_2d')
                        : t('analysis.canvas.tooltip.import_3d')}
                />),
        },
        {
            key: 2,
            content: (
                <ExportData
                    buttonEnable={buttonEnableForCanvas2D}
                    onExportJsonChange={handleExportJson}
                    onExportPngChange={handleExportPng}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.export_2d')
                        : t('analysis.canvas.tooltip.export_3d')}
                />),
        },
        {
            key: 3,
            content: (
                <StyleConfig
                    styleConfig={styleConfigData}
                    onChange={handleGraphStyleChange}
                    buttonEnable={buttonEnableForCanvas2D}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.style_2d')
                        : t('analysis.canvas.tooltip.style_3d')}
                />),
        },
        {
            key: 4,
            content: (
                <FilterHome
                    graphData={graphData}
                    onChange={handleFilterChange}
                    buttonEnable={buttonEnableForCanvas2D}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.filter_2d')
                        : t('analysis.canvas.tooltip.filter_3d')}
                />
            ),
        },
        {
            key: 5,
            content: (
                <LayoutConfig
                    buttonEnable={buttonEnableForCanvas2D}
                    onClick={handleLayoutPanel}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.layout_2d')
                        : t('analysis.canvas.tooltip.layout_3d')}
                />
            ),
        },
        {
            key: 6,
            content: (
                <SettingConfig
                    buttonEnable={showCanvasInfo}
                    onClick={handleSettingPanel}
                    tooltip={t('analysis.canvas.tooltip.setting_2d')}
                />
            ),
        },
        {
            key: 7,
            content: (
                <NewConfig
                    buttonEnable={buttonEnableForCanvas2D}
                    onClickAddNode={handleClickNewAddNode}
                    onClickAddEdge={handleClickNewAddEdge}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.new_2d')
                        : t('analysis.canvas.tooltip.new_3d')}
                />),
        },
        {
            key: 8,
            content: (
                <Statistics
                    buttonEnable={buttonEnableForCanvas2D}
                    onClick={handleStatisticsPanel}
                    tooltip={isCanvas2D
                        ? t('analysis.canvas.tooltip.statistics_2d')
                        : t('analysis.canvas.tooltip.statistics_3d')}
                />
            ),
        },
    ];

    const extraContent = [{
        key: 'quick-layout',
        content: <QuickLayoutSwitcher layout={layout} defaultLayout={defaultLayout} startId={startId}
            onClick={handleQuickLayoutChange}
            buttonEnable={buttonEnableForCanvas2D && layoutReady
                && Boolean(graphData?.nodes?.length || graphData?.edges?.length)} />,
    }];
    return (
        <MenuBar content={menubarContent} extra={extraContent} />
    );
};

export default GraphMenuBar;
