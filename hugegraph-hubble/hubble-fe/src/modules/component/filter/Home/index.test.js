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
import FilterHome from './index';
import {getAnalysisStyles} from '../../../../utils/analysisGraphPresentation';
import {exportHubbleGraph} from '../../../../visualization/hubble/adapter';
import {adaptAnalysisGraph} from '../../../../utils/analysisGraphData';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../../utils/analysisGraphPresentation', () => {
    const actual = jest.requireActual('../../../../utils/analysisGraphPresentation');
    return {...actual, getAnalysisStyles: jest.fn(actual.getAnalysisStyles)};
});
jest.mock('../../../../visualization/hubble/adapter', () => {
    const actual = jest.requireActual('../../../../visualization/hubble/adapter');
    return {...actual, exportHubbleGraph: jest.fn(actual.exportHubbleGraph)};
});
jest.mock('../FilterDrawer', () => ({__esModule: true, default: ({open, onCancel}) => open
    ? <button onClick={onCancel}>close filter</button> : null}));

const fixture = id => adaptAnalysisGraph({vertices: [{id, label: 'person', properties: {}}], edges: []});

test('does no whole-graph projection while closed and refreshes only at explicit drawer opening', async () => {
    const first = fixture(0);
    const second = fixture('0');
    const {rerender} = render(<FilterHome buttonEnable graphData={first} />);
    rerender(<FilterHome buttonEnable graphData={second} />);
    expect(getAnalysisStyles).not.toHaveBeenCalled();
    expect(exportHubbleGraph).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', {name: /analysis.canvas.filter/}));
    expect(getAnalysisStyles).toHaveBeenCalledTimes(1);
    expect(exportHubbleGraph).toHaveBeenLastCalledWith(second);
    expect(exportHubbleGraph.mock.results[0].value.vertices[0]).toBe(second.nodes[0].data.raw);
    rerender(<FilterHome buttonEnable graphData={first} />);
    expect(getAnalysisStyles).toHaveBeenCalledTimes(1);
    await userEvent.click(screen.getByRole('button', {name: 'close filter'}));
    rerender(<FilterHome buttonEnable graphData={second} />);
    expect(getAnalysisStyles).toHaveBeenCalledTimes(1);
    await userEvent.click(screen.getByRole('button', {name: /analysis.canvas.filter/}));
    expect(getAnalysisStyles).toHaveBeenCalledTimes(2);
});
