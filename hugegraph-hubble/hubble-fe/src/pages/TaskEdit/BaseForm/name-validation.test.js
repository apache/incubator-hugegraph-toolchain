/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Form} from 'antd';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import BaseForm from './index';
import * as api from '../../../api';
import {isPdEnabled} from '../../../utils/config';

jest.mock('../../../api', () => ({
    manage: {
        getDatasourceList: jest.fn(),
        getGraphList: jest.fn(),
        getTaskList: jest.fn(),
    },
}));
jest.mock('../../../utils/config', () => ({isPdEnabled: jest.fn()}));
jest.mock('../../../i18n', () => ({
    t: key => key,
}));
jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

beforeAll(() => {
    window.matchMedia = window.matchMedia || (() => ({
        matches: false,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

beforeEach(() => {
    jest.clearAllMocks();
    isPdEnabled.mockReturnValue(false);
    api.manage.getDatasourceList.mockResolvedValue({
        status: 200,
        data: {records: [{datasource_id: '9', datasource_name: 'fixture.csv'}]},
    });
    api.manage.getGraphList.mockResolvedValue({
        status: 200,
        data: {records: [{
            name: 'hugegraph',
            schemaview: {vertices: [{name: 'person'}], edges: []},
        }]},
    });
    api.manage.getTaskList.mockResolvedValue({status: 200, data: {total: 0}});
});

const renderForm = onFormFinish => render(
    <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
        <Form.Provider onFormFinish={onFormFinish}>
            <BaseForm visible cancel={jest.fn()} loading={false} />
        </Form.Provider>
    </MemoryRouter>
);

it('shows only the required error and skips duplicate lookup for an empty name', async () => {
    renderForm(jest.fn());

    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));

    const nameItem = screen.getByPlaceholderText('task.edit.name_placeholder')
        .closest('.ant-form-item');
    await waitFor(() => expect(nameItem).toHaveTextContent('common.validation.required'));
    expect(nameItem).not.toHaveTextContent('task.edit.name_rule');
    expect(nameItem).not.toHaveTextContent('task.edit.duplicate_name');
    expect(api.manage.getTaskList).not.toHaveBeenCalled();
});

it('keeps the duplicate-name error for an existing non-empty task name', async () => {
    api.manage.getTaskList.mockResolvedValue({
        status: 200,
        data: {records: [{task_name: 'existing_task'}], total: 1},
    });
    renderForm(jest.fn());

    const input = screen.getByPlaceholderText('task.edit.name_placeholder');
    fireEvent.change(input, {target: {value: 'existing_task'}});
    fireEvent.blur(input);

    expect(await screen.findByText('task.edit.duplicate_name')).toBeInTheDocument();
    expect(api.manage.getTaskList).toHaveBeenCalledWith({
        query: 'existing_task',
        page_size: -1,
    });
});

it('submits the first step when a legal new task name and required options are set', async () => {
    const onFormFinish = jest.fn();
    renderForm(onFormFinish);

    await screen.findByText('fixture.csv', {selector: '.ant-select-selection-item'});
    const graphSelect = document.querySelector('#base_form_ingestion_option_graph');
    fireEvent.mouseDown(graphSelect);
    fireEvent.click(await screen.findByText('hugegraph', {
        selector: '.ant-select-item-option-content',
    }));
    fireEvent.change(screen.getByPlaceholderText('task.edit.name_placeholder'), {
        target: {value: 'new_task'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.next'}));

    await waitFor(() => expect(onFormFinish).toHaveBeenCalledWith(
        'base_form',
        expect.objectContaining({
            values: expect.objectContaining({task_name: 'new_task'}),
        })
    ));
});
