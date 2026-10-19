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
import {GRAPH_RENDER_MODE} from '../../../../../utils/constants';
import useDownloadJson from '../../../../../customHook/useDownloadJson';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../../../customHook/useDownloadJson', () => ({__esModule: true, default: jest.fn()}));
jest.mock('../../../../component/MenuBar', () => ({__esModule: true,
    default: ({content}) => <>{content.map(item => <span key={item.key}>{item.content}</span>)}</>}));
jest.mock('../../../../component/ExportData', () => ({__esModule: true,
    default: ({onExportJsonChange}) => <button onClick={() => onExportJsonChange('graph-export')}>export JSON</button>}));
jest.mock('../../../../component/ImportData', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/styleConfig/Home', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/filter/Home', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/LayoutConfig', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/SettingConfig', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/NewConfig', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/Statistics', () => ({__esModule: true, default: () => null}));
jest.mock('../../../../component/GraphRenderModeSwitcher', () => ({__esModule: true, default: () => null}));

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
