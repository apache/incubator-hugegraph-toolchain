/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Form} from 'antd';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import BaseForm from './index';
import * as api from '../../../api';
import {isPdEnabled} from '../../../utils/config';

jest.mock('../../../api', () => ({
    manage: {
        getDatasourceList: jest.fn(),
        getGraphList: jest.fn(),
        getGraphSpaceList: jest.fn(),
        getGraphView: jest.fn(),
        getTaskList: jest.fn(),
    },
}));
jest.mock('../../../utils/config', () => ({isPdEnabled: jest.fn()}));
jest.mock('../../../utils/rules', () => ({required: () => ({required: true}), isNoramlName: () => ({})}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

const graphList = {status: 200, data: {records: [{name: 'selected'}, {name: 'other'}]}};
const readySchema = {status: 200, data: {vertices: [{name: 'person'}], edges: []}};
const emptySchema = {status: 200, data: {vertices: [], edges: []}};
const inlineConfig = {suppressBusinessErrorToast: true};
const deferred = () => {
    let resolve;
    const promise = new Promise(done => {resolve = done;});
    return {promise, resolve};
};
const renderForm = onFormFinish => {
    const view = render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <Form.Provider onFormFinish={onFormFinish}>
                <BaseForm visible cancel={jest.fn()} loading={false} />
            </Form.Provider>
        </MemoryRouter>
    );
    fireEvent.change(screen.getByPlaceholderText('task.edit.name_placeholder'), {
        target: {value: 'import_task'},
    });
    return view;
};

beforeAll(() => {
    window.matchMedia = window.matchMedia || (() => ({
        matches: false, addListener: jest.fn(), removeListener: jest.fn(),
    }));
});
beforeEach(() => {
    jest.resetAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem('hubble_workbench_graph_context', JSON.stringify({
        graphspace: 'DEFAULT', graph: 'selected',
    }));
    isPdEnabled.mockReturnValue(false);
    api.manage.getDatasourceList.mockResolvedValue({
        status: 200, data: {records: [{datasource_id: '9', datasource_name: 'fixture.csv'}]},
    });
    api.manage.getGraphList.mockResolvedValue(graphList);
    api.manage.getGraphView.mockResolvedValue(readySchema);
    api.manage.getTaskList.mockResolvedValue({status: 200, data: {records: []}});
});

it('does not fetch schema for graph options before a target is selected', async () => {
    localStorage.clear();
    renderForm(jest.fn());
    await waitFor(() => expect(api.manage.getGraphList).toHaveBeenCalled());
    fireEvent.mouseDown(document.querySelector('#base_form_ingestion_option_graph'));

    expect(await screen.findByText('other', {selector: '.ant-select-item-option-content'})).toBeInTheDocument();
    expect(api.manage.getGraphView).not.toHaveBeenCalled();
});

it('checks only the selected metadata-only graph and blocks submission while pending or empty', async () => {
    const schema = deferred();
    api.manage.getGraphView.mockReturnValue(schema.promise);
    const finish = jest.fn();
    renderForm(finish);

    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledWith(
        'DEFAULT', 'selected', inlineConfig
    ));
    expect(api.manage.getGraphView).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));
    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledTimes(2));
    expect(finish).not.toHaveBeenCalled();
    await act(async () => schema.resolve(emptySchema));

    expect(await screen.findByText('task.edit.prepare_schema_first')).toBeInTheDocument();
    expect(finish).not.toHaveBeenCalled();
    expect(api.manage.getGraphView.mock.calls.every(([, graph]) => graph === 'selected')).toBe(true);
});

it('allows submission when the selected graph schema is ready', async () => {
    const finish = jest.fn();
    renderForm(finish);
    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));

    await waitFor(() => expect(finish).toHaveBeenCalledWith('base_form', expect.objectContaining({
        values: expect.objectContaining({ingestion_option: {graphspace: 'DEFAULT', graph: 'selected'}}),
    })));
});

it.each([
    ['request failure', () => Promise.reject(new Error('transport secret'))],
    ['non-success response', () => Promise.resolve({status: 503})],
    ['missing schema arrays', () => Promise.resolve({status: 200, data: {}})],
])('blocks %s and retries readiness when the user submits again', async (_, response) => {
    api.manage.getGraphView.mockImplementation(response);
    const finish = jest.fn();
    renderForm(finish);

    expect(await screen.findByText('graph.card.schema_unavailable')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));
    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledTimes(2));
    expect(finish).not.toHaveBeenCalled();
    expect(screen.queryByText('transport secret')).not.toBeInTheDocument();

    api.manage.getGraphView.mockResolvedValue(readySchema);
    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));
    await waitFor(() => expect(finish).toHaveBeenCalledTimes(1));
});

it('ignores ready schema from a previous space with the same graph name', async () => {
    isPdEnabled.mockReturnValue(true);
    localStorage.setItem('hubble_workbench_graph_context', JSON.stringify({
        graphspace: 'SPACE_A', graph: 'selected',
    }));
    api.manage.getGraphSpaceList.mockResolvedValue({
        status: 200, data: {records: [{name: 'SPACE_A'}, {name: 'SPACE_B'}]},
    });
    const oldSchema = deferred();
    api.manage.getGraphView.mockImplementation(space => (
        space === 'SPACE_A' ? oldSchema.promise : Promise.resolve(emptySchema)
    ));
    const finish = jest.fn();
    renderForm(finish);
    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledWith(
        'SPACE_A', 'selected', inlineConfig
    ));
    fireEvent.mouseDown(document.querySelector('#base_form_ingestion_option_graphspace'));
    fireEvent.click(await screen.findByText('SPACE_B', {selector: '.ant-select-item-option-content'}));
    await waitFor(() => expect(api.manage.getGraphList).toHaveBeenCalledWith('SPACE_B', {page_size: -1}));
    fireEvent.mouseDown(document.querySelector('#base_form_ingestion_option_graph'));
    fireEvent.click(await screen.findByText('selected', {selector: '.ant-select-item-option-content'}));
    expect(await screen.findByText('task.edit.prepare_schema_first')).toBeInTheDocument();

    await act(async () => oldSchema.resolve(readySchema));
    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));
    await waitFor(() => expect(api.manage.getGraphView).toHaveBeenCalledWith(
        'SPACE_B', 'selected', inlineConfig
    ));
    expect(screen.getByText('task.edit.prepare_schema_first')).toBeInTheDocument();
    expect(finish).not.toHaveBeenCalled();
});
