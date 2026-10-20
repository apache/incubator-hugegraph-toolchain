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

import {useEffect} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {message} from 'antd';
import EditElement from './index';
import {GraphContext} from '../Context';
import GraphAnalysisContext from '../../Context';
import {useAnalysisGraph} from '../Graph/useAnalysisGraph';
import {adaptAnalysisGraph} from '../../../utils/analysisGraphData';
import * as api from '../../../api';

const mockEmpty = {status: 'idle', data: {nodes: [], edges: []}};
jest.mock('../../../visualization/react', () => ({
    useGraphSnapshot: controller => controller?.getSnapshot() ?? mockEmpty,
}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../api', () => ({analysis: {
    getVertexProperties: jest.fn(), updateVertexProperties: jest.fn(),
}}));

const raw = {vertices: [{id: 'alice', label: 'person', properties: {name: 'Alice'}}], edges: []};
const defaultScope = {graphSpace: 'DEFAULT', graph: 'g'};
const initial = {kind: 'node', renderingId: 'node:string:alice', id: 'alice',
    itemType: 'person', properties: {name: 'Alice'}};

function Harness({owner, cancel, scope = defaultScope}) {
    const {onReady, editElement} = useAnalysisGraph({data: raw, graphSpaceInfo: scope, queryStatus: 'success'});
    useEffect(() => { onReady(owner); }, [onReady, owner]);
    return <GraphContext.Provider value={{controller: owner,
        run: operation => Promise.resolve().then(operation).catch(error => message.error(error.message))}}>
        <GraphAnalysisContext.Provider value={scope}>
            <EditElement show drawerInfo={initial} onChange={editElement} cancel={cancel} />
        </GraphAnalysisContext.Provider>
    </GraphContext.Provider>;
}

const createOwner = () => {
    const snapshot = {status: 'ready', data: adaptAnalysisGraph(raw)};
    return {getSnapshot: () => snapshot, updateElements: jest.fn().mockResolvedValue({status: 'applied'})};
};
const startSave = async () => {
    fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'}));
    const input = await screen.findByRole('textbox', {name: 'name'});
    fireEvent.change(input, {target: {value: 'Bob'}});
    fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.save'}));
};
let success;
let failure;
let warning;

beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query,
        addListener: jest.fn(), removeListener: jest.fn(), addEventListener: jest.fn(),
        removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
beforeEach(() => {
    jest.clearAllMocks();
    api.analysis.getVertexProperties.mockResolvedValue({status: 200,
        data: {nullableProps: ['name'], nonNullableProps: [], primaryKeys: []}});
    api.analysis.updateVertexProperties.mockResolvedValue({status: 200});
    success = jest.spyOn(message, 'success').mockImplementation(() => {});
    failure = jest.spyOn(message, 'error').mockImplementation(() => {});
    warning = jest.spyOn(message, 'warning').mockImplementation(() => {});
});
afterEach(() => jest.restoreAllMocks());

test.each(['rejected', 'superseded', 'disposed', 'missing'])(
    'reports a committed write separately from a local %s outcome and blocks duplicate writes', async outcome => {
    const cancel = jest.fn();
    const owner = {...createOwner(), updateElements: outcome === 'rejected'
        ? jest.fn().mockRejectedValue(new Error('engine draw failed'))
        : jest.fn().mockResolvedValue(outcome === 'missing' ? undefined : {status: outcome})};
    render(<Harness owner={owner} cancel={cancel} />);
    await startSave();
    await waitFor(() => expect(owner.updateElements).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(cancel).toHaveBeenCalledTimes(1));
    expect(api.analysis.updateVertexProperties).toHaveBeenCalledTimes(1);
    expect(success).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
    expect(warning).toHaveBeenCalledWith('analysis.canvas.edit_element.sync_failed');
    const edit = screen.getByRole('button', {name: /analysis\.canvas\.edit_element\.edit$/});
    expect(edit).toBeDisabled();
    expect(edit).not.toHaveClass('ant-btn-loading');
    expect(screen.getByRole('button', {name: 'analysis.canvas.edit_element.close'})).toBeEnabled();
    fireEvent.click(edit);
    expect(api.analysis.updateVertexProperties).toHaveBeenCalledTimes(1);
});

test('reports success only after the local update is applied', async () => {
    const owner = createOwner();
    const cancel = jest.fn();
    render(<Harness owner={owner} cancel={cancel} />);
    await startSave();
    await waitFor(() => expect(cancel).toHaveBeenCalledTimes(1));
    expect(success).toHaveBeenCalledWith('analysis.canvas.edit_element.save_success');
    expect(warning).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
    const edit = screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'});
    expect(edit).toBeDisabled();
    expect(edit).not.toHaveClass('ant-btn-loading');
    const close = screen.getByRole('button', {name: 'analysis.canvas.edit_element.close'});
    expect(close).toBeEnabled();
    fireEvent.click(close);
    await waitFor(() => expect(cancel).toHaveBeenCalledTimes(2));
});

test.each(['transport', 'response'])('retains the form and permits retry after a backend %s failure', async outcome => {
    if (outcome === 'transport') api.analysis.updateVertexProperties.mockRejectedValueOnce(new Error('network failed'));
    else api.analysis.updateVertexProperties.mockResolvedValueOnce({status: 500});
    const owner = createOwner();
    const cancel = jest.fn();
    render(<Harness owner={owner} cancel={cancel} />);
    await startSave();
    await waitFor(() => expect(failure).toHaveBeenCalledTimes(1));
    expect(owner.updateElements).not.toHaveBeenCalled();
    expect(success).not.toHaveBeenCalled();
    expect(warning).not.toHaveBeenCalled();
    expect(cancel).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox', {name: 'name'})).toHaveValue('Bob');
    const save = screen.getByRole('button', {name: 'analysis.canvas.edit_element.save'});
    expect(save).toBeEnabled();
    fireEvent.click(save);
    await waitFor(() => expect(cancel).toHaveBeenCalledTimes(1));
    expect(api.analysis.updateVertexProperties).toHaveBeenCalledTimes(2);
    expect(success).toHaveBeenCalledTimes(1);
});

test.each(['scope', 'unmount'])('a late local failure remains silent after %s changes', async action => {
    let reject;
    const pending = new Promise((_resolve, fail) => { reject = fail; });
    const owner = createOwner();
    owner.updateElements.mockReturnValueOnce(pending);
    const cancel = jest.fn();
    const view = render(<Harness owner={owner} cancel={cancel} />);
    await startSave();
    await waitFor(() => expect(owner.updateElements).toHaveBeenCalledTimes(1));
    expect(screen.getByRole('button', {name: 'analysis.canvas.edit_element.close'})).toBeDisabled();
    if (action === 'unmount') view.unmount();
    else view.rerender(<Harness owner={owner} cancel={cancel} scope={{graphSpace: 'OTHER', graph: 'g'}} />);
    await act(async () => reject(new Error('stale canvas update')));
    expect(success).not.toHaveBeenCalled();
    expect(warning).not.toHaveBeenCalled();
    expect(failure).not.toHaveBeenCalled();
    expect(cancel).not.toHaveBeenCalled();
});
