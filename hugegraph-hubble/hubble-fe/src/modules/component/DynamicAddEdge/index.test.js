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

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {message} from 'antd';
import DynamicAddEdge from './index';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';

jest.mock('react-i18next', () => {
    const t = key => key;
    return {...jest.requireActual('react-i18next'), useTranslation: () => ({t})};
});
jest.mock('../../../api', () => ({analysis: {addGraphNode: jest.fn(), addEdge: jest.fn(),
    fetchVertexlinks: jest.fn(), fetchEdgeLabels: jest.fn()}}));
const data = {vertices: [{id: 'alice', label: 'person', properties: {}}], edges: []};
const callback = jest.fn();
const onCancel = jest.fn();
const scopeA = {graphSpace: 'space-a', graph: 'graph-a'};
const renderDrawer = (scope = scopeA, open = true) => (
    <GraphAnalysisContext.Provider value={scope}>
        <DynamicAddEdge open={open} onCancel={onCancel} onOk={callback} graphData={data}
            drawerInfo={{vertexId: 'alice', vertexLabel: 'person'}} isClickNew={false} isOutEdge />
    </GraphAnalysisContext.Provider>
);
const label = key => `analysis.canvas.dynamic_add.${key}`;
let success;
let failure;
let warning;
beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
beforeEach(() => {
    jest.resetAllMocks();
    success = jest.spyOn(message, 'success').mockImplementation(() => {});
    failure = jest.spyOn(message, 'error').mockImplementation(() => {});
    warning = jest.spyOn(message, 'warning').mockImplementation(() => {});
    api.analysis.fetchVertexlinks.mockResolvedValue({status: 200, data: ['knows']});
    api.analysis.fetchEdgeLabels.mockResolvedValue({status: 200, data: {properties: []}});
});
afterEach(() => jest.restoreAllMocks());
const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
    return {promise, resolve, reject};
};
const start = async () => {
    await waitFor(() => expect(api.analysis.fetchVertexlinks).toHaveBeenCalled());
    fireEvent.mouseDown(screen.getByRole('combobox', {name: label('edge_type')}));
    fireEvent.click(await screen.findByText('knows', {selector: '.ant-select-item-option-content'}));
    fireEvent.change(await screen.findByRole('textbox', {name: /analysis.canvas.dynamic_add.target/}),
        {target: {value: 'bob'}});
    await waitFor(() => expect(screen.getByRole('button', {name: label('add')})).toBeEnabled());
    fireEvent.click(screen.getByRole('button', {name: label('add')}));
};

test('notifies and closes only after an active scope succeeds', async () => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    expect(onCancel).not.toHaveBeenCalled();
    expect(callback).not.toHaveBeenCalled();
    expect(screen.getByRole('button', {name: label('add')})).toBeDisabled();
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledWith(data);
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(success).toHaveBeenCalledWith(label('save_success'));
});

test.each(['graph', 'space', 'owner_hide', 'unmount'])('drops an old pending response after %s', async action => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    const result = render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    if (action === 'graph') result.rerender(renderDrawer({...scopeA, graph: 'graph-b'}));
    if (action === 'space') result.rerender(renderDrawer({...scopeA, graphSpace: 'space-b'}));
    if (action === 'owner_hide') result.rerender(renderDrawer(scopeA, false));
    if (action === 'unmount') result.unmount();
    const cancelCount = onCancel.mock.calls.length;
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalledTimes(cancelCount);
    expect(success).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
});

test.each(['transport', 'response'])('restores closing after a current %s failure', async failureType => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => {
        if (failureType === 'transport') request.reject(new Error('network failed'));
        else request.resolve({status: 400});
    });
    expect(callback).not.toHaveBeenCalled();
    expect(onCancel).not.toHaveBeenCalled();
    expect(failure).toHaveBeenCalledWith(label('save_failed'));
    expect(screen.getByRole('button', {name: label('add')})).toBeEnabled();
    expect(screen.getByRole('button', {name: label('cancel')})).toBeEnabled();
    expect(screen.getByRole('button', {name: /^close$/i})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: label('cancel')}));
    expect(onCancel).toHaveBeenCalledTimes(1);
});

test('does not install edge metadata delivered after a scope change', async () => {
    const old = deferred();
    api.analysis.fetchVertexlinks.mockReturnValueOnce(old.promise)
        .mockResolvedValueOnce({status: 200, data: ['knows-b']});
    const result = render(renderDrawer());
    await waitFor(() => expect(api.analysis.fetchVertexlinks).toHaveBeenCalledTimes(1));
    result.rerender(renderDrawer({...scopeA, graph: 'graph-b'}));
    await waitFor(() => expect(api.analysis.fetchVertexlinks).toHaveBeenCalledTimes(2));
    await act(async () => old.resolve({status: 200, data: ['knows-a']}));
    fireEvent.mouseDown(screen.getByRole('combobox', {name: label('edge_type')}));
    expect(await screen.findByText('knows-b', {selector: '.ant-select-item-option-content'})).toBeInTheDocument();
    expect(screen.queryByText('knows-a', {selector: '.ant-select-item-option-content'})).not.toBeInTheDocument();
});

test('blocks every user close entry during the write and still applies success and accounting before closing', async () => {
    const request = deferred();
    const commit = deferred();
    let appliedCount = 0;
    callback.mockImplementationOnce(async () => { await commit.promise; appliedCount++; });
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    const cancelButton = screen.getByRole('button', {name: label('cancel')});
    const closeButton = screen.getByRole('button', {name: /^close$/i});
    expect(cancelButton).toBeDisabled();
    expect(closeButton).toBeDisabled();
    fireEvent.click(cancelButton);
    fireEvent.click(closeButton);
    fireEvent.click(document.querySelector('.ant-drawer-mask'));
    fireEvent.keyDown(document, {key: 'Escape', code: 'Escape', keyCode: 27});
    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledTimes(1);
    expect(appliedCount).toBe(0);
    expect(cancelButton).toBeDisabled();
    fireEvent.click(cancelButton);
    expect(onCancel).not.toHaveBeenCalled();
    await act(async () => commit.resolve());
    expect(appliedCount).toBe(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(success).toHaveBeenCalledWith(label('save_success'));
    expect(failure).not.toHaveBeenCalled();
});

test('reports a completed server write separately from failed local synchronization and blocks another POST', async () => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    callback.mockRejectedValueOnce(new Error('canvas update failed'));
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledWith(data);
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(warning).toHaveBeenCalledWith(label('sync_failed'));
    expect(failure).not.toHaveBeenCalled();
    expect(success).not.toHaveBeenCalled();
    const add = screen.getByRole('button', {name: label('add')});
    expect(add).toBeDisabled();
    fireEvent.click(add);
    expect(api.analysis.addEdge).toHaveBeenCalledTimes(1);
});

test.each(['transport', 'response'])('allows a deliberate retry after an unsuccessful %s write', async failureType => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise).mockResolvedValueOnce({status: 200, data});
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => {
        if (failureType === 'transport') request.reject(new Error('network failed'));
        else request.resolve({status: 400});
    });
    expect(screen.getByRole('button', {name: label('add')})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: label('add')}));
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(callback).toHaveBeenCalledTimes(1));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(warning).not.toHaveBeenCalled();
});

test('does not send a late local-sync warning or close into a new graph scope', async () => {
    const request = deferred();
    const commit = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    callback.mockReturnValueOnce(commit.promise);
    const result = render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledTimes(1);
    result.rerender(renderDrawer({...scopeA, graph: 'graph-b'}));
    await act(async () => commit.reject(new Error('old canvas no longer exists')));
    expect(warning).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
    expect(success).not.toHaveBeenCalled();
    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByRole('button', {name: label('add')})).toBeEnabled();
});

test.each(['superseded', 'disposed'])('does not report server-only success when the canvas returns %s', async status => {
    const request = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    callback.mockResolvedValueOnce({status});
    render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledWith(data);
    expect(warning).toHaveBeenCalledWith(label('sync_failed'));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(failure).not.toHaveBeenCalled();
    expect(success).not.toHaveBeenCalled();
    const add = screen.getByRole('button', {name: label('add')});
    expect(add).toBeDisabled();
    fireEvent.click(add);
    expect(api.analysis.addEdge).toHaveBeenCalledTimes(1);
});

test.each(['superseded', 'disposed'])('drops late %s synchronization results after switching graph scope', async status => {
    const request = deferred();
    const commit = deferred();
    api.analysis.addEdge.mockReturnValueOnce(request.promise);
    callback.mockReturnValueOnce(commit.promise);
    const result = render(renderDrawer());
    await start();
    await waitFor(() => expect(api.analysis.addEdge).toHaveBeenCalledTimes(1));
    await act(async () => request.resolve({status: 200, data}));
    expect(callback).toHaveBeenCalledTimes(1);
    result.rerender(renderDrawer({...scopeA, graph: 'graph-b'}));
    await act(async () => commit.resolve({status}));
    expect(warning).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
    expect(success).not.toHaveBeenCalled();
    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByRole('button', {name: label('add')})).toBeEnabled();
});

test('accepts an explicitly applied canvas result', async () => {
    api.analysis.addEdge.mockResolvedValueOnce({status: 200, data});
    callback.mockResolvedValueOnce({status: 'applied'});
    render(renderDrawer());
    await start();
    await waitFor(() => expect(onCancel).toHaveBeenCalledTimes(1));
    expect(callback).toHaveBeenCalledWith(data);
    expect(success).toHaveBeenCalledTimes(1);
    expect(warning).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
});
