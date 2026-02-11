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
 * @author
 */

import React, {useCallback} from 'react';
import MenuBar from '../../../../component/MenuBar';
import ImportData from '../../../../component/ImportData';
import ExportData from '../../../../component/ExportData';
import StyleConfig from '../../../../component/styleConfig/Home';
import FilterHome from '../../../../component/filter/Home';
import LayoutConfig from '../../../../component/LayoutConfig';
import SettingConfig from '../../../../component/SettingConfig';
import NewConfig from '../../../../component/NewConfig';
import Statistics from '../../../../component/Statistics';
import RenderModeSwitcher from '../../../../component/GraphRenderModeSwitcher';
import {PANEL_TYPE, GRAPH_RENDER_MODE, MENUBAR_TOOLTIPS_2D, MENUBAR_TOOLTIPS_3D} from '../../../../../utils/constants';
import {formatToDownloadData} from '../../../../../utils/formatGraphResultData';
import useDownloadJson from '../../../../../customHook/useDownloadJson';

const {LAYOUT, SETTING, STATISTICS} = PANEL_TYPE;
const {CANVAS2D} = GRAPH_RENDER_MODE;

const GraphMenuBar = props => {
    const {
        styleConfigData,
        graphData,
        handleImportData,
        handleExportPng,
        handleGraphStyleChange,
        handleFilterChange,
        handleTogglePanel,
        handleClickNewAddNode,
        handleClickNewAddEdge,
        handleSwitchRenderMode,
        refreshExcuteCount,
        showCanvasInfo,
        graphRenderMode,
    } = props;

    const isCanvas2D = graphRenderMode === CANVAS2D;
    const buttonEnableForCanvas2D = showCanvasInfo && isCanvas2D;
    const buttonEnableForImport = !showCanvasInfo && isCanvas2D;

    const {downloadJsonHandler} = useDownloadJson();

    const handleExportJson = useCallback(
        fileName => {
            downloadJsonHandler(fileName, formatToDownloadData(graphData));
        },
        [downloadJsonHandler]
    );

    const menubarContent = [
        {
            key: 1,
            content: (
                <ImportData
                    buttonEnable={buttonEnableForImport}
                    onUploadChange={handleImportData}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.IMPORT : MENUBAR_TOOLTIPS_3D.IMPORT}
                />),
        },
        {
            key: 2,
            content: (
                <ExportData
                    buttonEnable={buttonEnableForCanvas2D}
                    onExportJsonChange={handleExportJson}
                    onExportPngChange={handleExportPng}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.EXPORT : MENUBAR_TOOLTIPS_3D.EXPORT}
                />),
        },
        {
            key: 3,
            content: (
                <StyleConfig
                    styleConfig={styleConfigData}
                    onChange={handleGraphStyleChange}
                    buttonEnable={buttonEnableForCanvas2D}
                    refreshExcuteCount={refreshExcuteCount}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.STYLE : MENUBAR_TOOLTIPS_3D.STYLE}
                />),
        },
        {
            key: 4,
            content: (
                <FilterHome
                    graphData={graphData}
                    onChange={handleFilterChange}
                    buttonEnable={buttonEnableForCanvas2D}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.FILTER : MENUBAR_TOOLTIPS_3D.FILTER}
                />
            ),
        },
        {
            key: 5,
            content: (
                <LayoutConfig
                    buttonEnable={buttonEnableForCanvas2D}
                    onClick={() => {
                        handleTogglePanel(LAYOUT);
                    }}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.LAYOUT : MENUBAR_TOOLTIPS_3D.LAYOUT}
                />
            ),
        },
        {
            key: 6,
            content: (
                <SettingConfig
                    buttonEnable={buttonEnableForCanvas2D}
                    onClick={() => {
                        handleTogglePanel(SETTING);
                    }}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.SETTING : MENUBAR_TOOLTIPS_3D.SETTING}
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
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.NEW : MENUBAR_TOOLTIPS_3D.NEW}
                />),
        },
        {
            key: 8,
            content: (
                <Statistics
                    buttonEnable={buttonEnableForCanvas2D}
                    onClick={() => {
                        handleTogglePanel(STATISTICS);
                    }}
                    tooltip={isCanvas2D ? MENUBAR_TOOLTIPS_2D.STATISTICS : MENUBAR_TOOLTIPS_3D.STATISTICS}
                />
            ),
        },
    ];

    const extraContent = [
        {
            key: 1,
            content: (
                <RenderModeSwitcher
                    onClick={handleSwitchRenderMode}
                    buttonEnable={showCanvasInfo}
                    value={graphRenderMode}
                    tooltip={MENUBAR_TOOLTIPS_2D.SWITCH}
                />
            ),
        },
    ];

    return (
        <MenuBar content={menubarContent} extra={extraContent} />
    );
};

export default GraphMenuBar;
