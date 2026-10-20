/*
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

import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import JSONbig from 'json-bigint';
import QueryMenuBar from './index';
import AlgorithmMenuBar from '../../../../algorithm/GraphResult/GraphMenuBar';
import {adaptAnalysisGraph} from '../../../../../utils/analysisGraphData';
import {GRAPH_RENDER_MODE, PANEL_TYPE} from '../../../../../utils/constants';
import useDownloadJson from '../../../../../customHook/useDownloadJson';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../../../customHook/useDownloadJson', () => ({__esModule: true, default: jest.fn()}));
jest.mock('../../../../component/MenuBar', () => ({__esModule: true,
    default: ({content, extra}) => <>
        {content.map(item => <span key={item.key}>{item.content}</span>)}
        {extra?.map(item => <aside key={item.key}>{item.content}</aside>)}
    </>}));
jest.mock('../../../../component/ExportData', () => ({__esModule: true,
    default: ({onExportJsonChange}) => <button onClick={() => onExportJsonChange('graph-export')}>export JSON</button>}));
jest.mock('../../../../component/ImportData', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/styleConfig/Home', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/filter/Home', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/LayoutConfig', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/SettingConfig', () => ({__esModule: true,
    default: ({buttonEnable, onClick}) => <button disabled={!buttonEnable} onClick={onClick}>Settings</button>}));
jest.mock('../../../../component/NewConfig', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/Statistics', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/GraphRenderModeSwitcher', () => ({__esModule: true,
    default: () => <button>Experimental renderer shortcut</button>}));

test.each([['query', QueryMenuBar], ['algorithm', AlgorithmMenuBar]])(
    '%s JSON export preserves full business records and exact IDs rather than rendering IDs', async (_kind, Menu) => {
        const raw = JSONbig.parse('{"vertices":[{"id":900719925474099312345,"label":"person",'
            + '"properties":{"name":"Alice"},"statistics":{"degree":2},"extra":[1,2]}],'
            + '"edges":[{"id":0,"source":900719925474099312345,"target":900719925474099312345,'
            + '"label":"knows","properties":{},"custom":false}]}');
        const downloadJsonHandler = jest.fn();
        useDownloadJson.mockReturnValue({downloadJsonHandler});
        render(<Menu graphData={adaptAnalysisGraph(raw)} showCanvasInfo
            graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} />);
        await userEvent.click(screen.getByRole('button', {name: 'export JSON'}));
        expect(downloadJsonHandler).toHaveBeenCalledWith('graph-export', raw);
        const payload = downloadJsonHandler.mock.calls[0][1];
        expect(payload.vertices[0]).toBe(raw.vertices[0]);
        expect(payload.edges[0]).toBe(raw.edges[0]);
        expect(payload.vertices[0].id.toFixed()).toBe('900719925474099312345');
        expect(payload.edges[0].id).toBe(0);
        expect(payload.vertices[0].extra).toEqual([1, 2]);
    }
);


test.each([['query', QueryMenuBar], ['algorithm', AlgorithmMenuBar]])(
    '%s JSON export reads the current complete business projection, including undrawn typed edges', async (_kind, Menu) => {
        const raw = JSONbig.parse('{"vertices":[{"id":1}],"edges":[{"id":900719925474099312345,'
            + '"source":1,"target":"missing","properties":{"kept":true}}]}');
        const exportData = jest.fn(() => raw);
        const downloadJsonHandler = jest.fn();
        useDownloadJson.mockReturnValue({downloadJsonHandler});
        render(<Menu graphData={adaptAnalysisGraph({vertices: raw.vertices, edges: []})}
            exportData={exportData} showCanvasInfo graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} />);
        await userEvent.click(screen.getByRole('button', {name: 'export JSON'}));
        expect(exportData).toHaveBeenCalledTimes(1);
        expect(downloadJsonHandler).toHaveBeenCalledWith('graph-export', raw);
        expect(downloadJsonHandler.mock.calls[0][1].edges[0].id.toFixed()).toBe('900719925474099312345');
    }
);

test.each([['query', QueryMenuBar], ['algorithm', AlgorithmMenuBar]])(
    '%s keeps only the existing Settings entry available in 3D', async (_kind, Menu) => {
        useDownloadJson.mockReturnValue({downloadJsonHandler: jest.fn()});
        const handleTogglePanel = jest.fn();
        const view = render(<Menu graphData={{nodes: [], edges: []}} showCanvasInfo
            graphRenderMode={GRAPH_RENDER_MODE.CANVAS3D} handleTogglePanel={handleTogglePanel} />);
        expect(screen.queryByRole('button', {name: 'Experimental renderer shortcut'})).not.toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', {name: 'Settings'}));
        expect(handleTogglePanel).toHaveBeenCalledWith(PANEL_TYPE.SETTING);
        view.rerender(<Menu graphData={{nodes: [], edges: []}} showCanvasInfo={false}
            graphRenderMode={GRAPH_RENDER_MODE.CANVAS3D} handleTogglePanel={handleTogglePanel} />);
        expect(screen.getByRole('button', {name: 'Settings'})).toBeDisabled();
    }
);

test.each([['query', QueryMenuBar], ['algorithm', AlgorithmMenuBar]])(
    '%s offers the next quick layout and disables it for 3D, loading and empty results', async (_kind, Menu) => {
        useDownloadJson.mockReturnValue({downloadJsonHandler: jest.fn()});
        const handleQuickLayoutChange = jest.fn();
        const props = {graphData: {nodes: [{id: 'visible'}], edges: []}, showCanvasInfo: true,
            graphRenderMode: GRAPH_RENDER_MODE.CANVAS2D, layout: {type: 'grid'}, layoutReady: true, handleQuickLayoutChange};
        const view = render(<Menu {...props} />);
        await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'}));
        expect(handleQuickLayoutChange).toHaveBeenLastCalledWith({type: 'radial'});
        view.rerender(<Menu {...props} layout={{type: 'radial'}} />);
        await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_force'}));
        expect(handleQuickLayoutChange).toHaveBeenLastCalledWith({type: 'd3-force'});
        for (const unavailable of [
            {graphRenderMode: GRAPH_RENDER_MODE.CANVAS3D}, {showCanvasInfo: false}, {layoutReady: false},
            {graphData: {nodes: [], edges: []}},
        ]) {
            view.rerender(<Menu {...props} {...unavailable} />);
            const button = screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'});
            expect(button).toBeDisabled();
            await userEvent.click(button);
        }
        expect(handleQuickLayoutChange).toHaveBeenCalledTimes(2);
    }
);

test.each([['query', QueryMenuBar], ['algorithm', AlgorithmMenuBar]])(
    '%s forwards the algorithm default and raw start vertex to the existing shortcut', async (_kind, Menu) => {
        useDownloadJson.mockReturnValue({downloadJsonHandler: jest.fn()});
        const handleQuickLayoutChange = jest.fn();
        const props = {graphData: {nodes: [{id: 'visible'}], edges: []}, showCanvasInfo: true, layoutReady: true,
            graphRenderMode: GRAPH_RENDER_MODE.CANVAS2D, defaultLayout: {type: 'columns'}, startId: 0,
            handleQuickLayoutChange};
        const view = render(<Menu {...props} layout={{type: 'columns'}} />);
        await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'}));
        expect(handleQuickLayoutChange).toHaveBeenLastCalledWith({type: 'radial', options: {focusNode: 'node:number:0'}});
        view.rerender(<Menu {...props} layout={{type: 'radial'}} />);
        await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_default'}));
        expect(handleQuickLayoutChange).toHaveBeenLastCalledWith(props.defaultLayout);
    }
);
