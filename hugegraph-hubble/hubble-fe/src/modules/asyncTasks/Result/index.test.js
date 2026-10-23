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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import AsyncTaskResult, {AsyncTaskResultContent} from './index';
import * as api from '../../../api/index';
import {isCypherEnabled} from '../../../utils/config';

jest.mock('../../../api/index', () => ({
    analysis: {fetchAsyncTaskResult: jest.fn(), fetchVermeerTaskValues: jest.fn()},
}));
jest.mock('../../../utils/config', () => ({isCypherEnabled: jest.fn(() => true)}));
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useParams: () => ({graphspace: 'DEFAULT', graph: 'hugegraph', taskId: '9'}),
}));
jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: (key, values) => ({
        'analysis.async_task.result_load_failed': 'Could not load task result.',
        'analysis.async_task.retry_result': 'Retry result',
        'analysis.async_task.result_loading': 'Loading task result',
        'analysis.async_task.no_result': 'No task result is available.',
        'analysis.async_task.result_title': 'Task result',
        'analysis.async_task.result_back': 'Back to task management',
        'analysis.async_task.result_context': 'DEFAULT / hugegraph / Task 9',
        'analysis.async_task.result_failure_title': 'Task failed',
        'analysis.async_task.result_size_limit_title': 'Task result exceeds the server limit',
        'analysis.async_task.result_size_limit_description': `Result ${values?.size} MiB exceeds ${values?.limit} MiB`,
        'analysis.async_task.refine_query': 'Refine query',
    })[key] || key}),
}));

const renderResult = () => render(
    <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
        <AsyncTaskResult />
    </MemoryRouter>
);

beforeEach(() => isCypherEnabled.mockReturnValue(true));

it('keeps result failure visible and retries the same task', async () => {
    api.analysis.fetchAsyncTaskResult
        .mockRejectedValueOnce(new Error('offline'))
        .mockResolvedValueOnce({status: 200, data: {task_result: '{"ok":true}'}});

    renderResult();

    expect(await screen.findByRole('alert')).toHaveTextContent(
        'Could not load task result.'
    );
    fireEvent.click(screen.getByRole('button', {name: 'Retry result'}));
    expect(await screen.findByText('true')).toBeInTheDocument();
    await waitFor(() => expect(api.analysis.fetchAsyncTaskResult).toHaveBeenCalledTimes(2));
});

it('renders a clear empty state instead of a literal null result', async () => {
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({
        status: 200,
        data: {task_result: 'null'},
    });

    renderResult();

    expect(await screen.findByText('No task result is available.')).toBeInTheDocument();
    expect(screen.queryByText('null')).not.toBeInTheDocument();
});

it('keeps task context and a graph-scoped return entry visible', async () => {
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({
        status: 200,
        data: {task_result: '{"ok":true}'},
    });

    renderResult();

    expect(screen.getByRole('heading', {name: 'Task result'})).toBeInTheDocument();
    expect(screen.getByText('DEFAULT / hugegraph / Task 9')).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'Back to task management'}))
        .toHaveAttribute('href', '/asyncTasks/DEFAULT/hugegraph');
    expect(await screen.findByText('true')).toBeInTheDocument();
});


it('pages direct Vermeer values and resets the cursor after a graph change', async () => {
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({status: 200, data: {
        task_result: JSON.stringify({vertices: [{ID: 'first'}], cursor: 1000, has_more: true}),
    }});
    api.analysis.fetchVermeerTaskValues.mockResolvedValue({status: 200, data: {
        vertices: [{ID: 'next'}], cursor: 1001, has_more: false,
    }});
    const view = render(<AsyncTaskResultContent graphspace='DEFAULT' graph='one' taskId={-9} />);
    fireEvent.click(await screen.findByRole('button', {name: 'analysis.async_task.result_next'}));
    expect(await screen.findByText('"next"')).toBeInTheDocument();
    expect(api.analysis.fetchVermeerTaskValues).toHaveBeenCalledWith('DEFAULT', 'one', -9, 1000);
    expect(screen.queryByRole('button', {name: 'analysis.async_task.result_next'})).not.toBeInTheDocument();
    view.rerender(<AsyncTaskResultContent graphspace='DEFAULT' graph='two' taskId={-9} />);
    await waitFor(() => expect(api.analysis.fetchAsyncTaskResult).toHaveBeenLastCalledWith('DEFAULT', 'two', -9));
    expect(await screen.findByText('"first"')).toBeInTheDocument();
});

it.each(['gremlin', 'cypher'])('explains an oversized %s result and links its query language', async taskType => {
    const failure = 'org.apache.hugegraph.exception.LimitExceedException: '
        + 'Task result size 77849617 exceeded limit 16777216 bytes';
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({status: 200, data: {
        task_status: 'failed', task_type: taskType, task_result: failure,
    }});
    renderResult();
    expect(await screen.findByRole('alert')).toHaveTextContent('Task result exceeds the server limit');
    expect(screen.getByRole('alert')).toHaveTextContent('Result 74.24 MiB exceeds 16.00 MiB');
    expect(screen.getByText(failure)).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'Refine query'}))
        .toHaveAttribute('href', `/gremlin/DEFAULT/hugegraph?language=${taskType}`);
});

it.each(['algorithm', undefined])('does not suggest a query editor for task type %s', async taskType => {
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({status: 200, data: {
        task_status: 'failed', task_type: taskType,
        task_result: 'Task result size 77849617 exceeded limit 16777216 bytes',
    }});
    renderResult();
    expect(await screen.findByRole('alert')).toHaveTextContent('Task result exceeds the server limit');
    expect(screen.queryByRole('link', {name: 'Refine query'})).not.toBeInTheDocument();
});

it.each(['gremlin', 'cypher'])('respects disabled Cypher when recovering a %s task', async taskType => {
    isCypherEnabled.mockReturnValue(false);
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({status: 200, data: {
        task_status: 'failed', task_type: taskType,
        task_result: 'Task result size 77849617 exceeded limit 16777216 bytes',
    }});
    renderResult();

    expect(await screen.findByRole('alert')).toHaveTextContent('Task result exceeds the server limit');
    const link = screen.queryByRole('link', {name: 'Refine query'});
    if (taskType === 'gremlin') {
        expect(link).toHaveAttribute('href', '/gremlin/DEFAULT/hugegraph?language=gremlin');
    }
    else {
        expect(link).not.toBeInTheDocument();
    }
});

it('does not classify unrelated task failures as a result size limit', async () => {
    api.analysis.fetchAsyncTaskResult.mockResolvedValue({status: 200, data: {
        task_status: 'failed', task_result: 'Query timed out',
    }});
    renderResult();
    expect(await screen.findByRole('alert')).toHaveTextContent('Task failed');
    expect(screen.getByText('Query timed out')).toBeInTheDocument();
    expect(screen.queryByText('Task result exceeds the server limit')).not.toBeInTheDocument();
});
