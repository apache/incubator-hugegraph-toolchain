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
 * @file  GraphToolBar
 * @author
 */

import React, {useState, useCallback} from 'react';
import ToolBar from '../../../../component/ToolBar';
import ZeroDegreeNodeSearch from '../../../../component/ZeroDegreeNode';
import RedoUndo from '../../../../component/RedoUndo';
import FitCenter from '../../../../component/FitCenter';
import ZoomGraph from '../../../../component/ZoomGraph';
import ClearGraph from '../../../../component/ClearGraph';
import FullScreen from '../../../../component/FullScreen';
import RefreshGraph from '../../../../component/RefreshGraph';
import FixNode from '../../../../component/FixNode';
import {PANEL_TYPE} from '../../../../../utils/constants';

const {CLOSED} = PANEL_TYPE;

const GraphToolBar = props => {
    const {
        handleRedoUndoChange,
        handleClearGraph,
        panelType,
        updatePanelType,
    } = props;

    const [isFullScreen, setFullScreen] = useState(false);

    const handleChangeFullScreen = useCallback(
        () => {
            setFullScreen(pre => !pre);
            updatePanelType(CLOSED);
        },
        [updatePanelType]
    );

    const toolBarExtras = [
        {key: '1', content: (<ZeroDegreeNodeSearch />)},
        {key: '2', content: (<RefreshGraph />)},
        {key: '3', content: (<FixNode />)},
        {key: '45', content: (<RedoUndo onChange={handleRedoUndoChange} />)},
        {key: '6', content: (<ClearGraph enable={!isFullScreen} onChange={handleClearGraph} />)},
        {key: '7', content: (<FitCenter />)},
        {key: '89', content: (<ZoomGraph />)},
        {key: '10', content: (<FullScreen onChange={handleChangeFullScreen} />)},
    ];

    return (
        <ToolBar extra={toolBarExtras} hasPadding={panelType !== CLOSED} />
    );
};

export default GraphToolBar;
