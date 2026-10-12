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

import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ViewLayer from './ViewLayer';
import * as api from '../../../api';
import enPages from '../../../i18n/resources/en-US/modules/pages.json';
import zhPages from '../../../i18n/resources/zh-CN/modules/pages.json';

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

jest.mock('../../../api', () => ({
    manage: {getTaskDetail: jest.fn()},
}));

jest.mock('react-json-view', () => ({src}) => (
    <pre data-testid='raw-task-json'>{JSON.stringify(src)}</pre>
));

const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return {promise, resolve, reject};
};

beforeEach(() => {
    jest.clearAllMocks();
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

test('clears stale task data and offers retry for the current task', async () => {
    const taskB = deferred();
    api.manage.getTaskDetail
        .mockResolvedValueOnce({status: 200, data: {name: 'Task A'}})
        .mockReturnValueOnce(taskB.promise)
        .mockResolvedValueOnce({status: 200, data: {name: 'Task B'}});

    const {rerender} = render(
        <ViewLayer visible task_id='A' onCancel={jest.fn()} />
    );
    expect(await screen.findByText('Task A')).toBeInTheDocument();

    rerender(<ViewLayer visible task_id='B' onCancel={jest.fn()} />);
    expect(screen.queryByText('Task A')).not.toBeInTheDocument();

    taskB.reject(new Error('down'));
    expect(await screen.findByText('task.view.unavailable')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'task.view.retry'}));
    await waitFor(() => expect(screen.getByText('Task B')).toBeInTheDocument());
});

test('shows a user summary while keeping raw backend fields collapsed', async () => {
    api.manage.getTaskDetail.mockResolvedValue({
        status: 200,
        data: {
            id: 7,
            conn_id: 3,
            job_name: 'movie_import',
            graphspace: 'DEFAULT',
            graph: 'hugegraph',
            job_status: 'SUCCESS',
            job_size: '12 MB',
            job_duration: '5 s',
            create_time: '2026-08-10 09:00:00',
            update_time: '2026-08-10 09:00:05',
        },
    });

    render(<ViewLayer visible task_id='7' onCancel={jest.fn()} />);

    expect(await screen.findByText('movie_import')).toBeVisible();
    expect(screen.getByText('DEFAULT')).toBeVisible();
    expect(screen.getByText('hugegraph')).toBeVisible();
    expect(screen.getByText('task.status.success')).toBeVisible();
    expect(screen.getByText('12 MB')).toBeVisible();
    expect(screen.getByText('5 s')).toBeVisible();
    expect(screen.getByText('2026-08-10 09:00:00')).toBeVisible();
    expect(screen.getByText('2026-08-10 09:00:05')).toBeVisible();

    const technicalDetails = screen.getByRole('button', {
        name: /task\.view\.technical_details/,
    });
    expect(technicalDetails).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('raw-task-json')).not.toBeInTheDocument();
    expect(screen.queryByText(/SUCCESS/)).not.toBeInTheDocument();

    await userEvent.click(technicalDetails);

    expect(technicalDetails).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByTestId('raw-task-json')).toBeVisible();
    expect(screen.getByText(/SUCCESS/)).toBeVisible();
    expect(screen.getByText(/conn_id/)).toBeVisible();
});

test('labels missing summary values instead of leaving blank fields', async () => {
    api.manage.getTaskDetail.mockResolvedValue({
        status: 200,
        data: {
            job_name: null,
            graphspace: '',
            graph: undefined,
            job_status: null,
            job_size: null,
            job_duration: '',
            create_time: null,
            update_time: undefined,
        },
    });

    render(<ViewLayer visible task_id='8' onCancel={jest.fn()} />);

    expect(await screen.findAllByText('task.view.unavailable_value')).toHaveLength(7);
    expect(screen.getByText('task.status.unknown')).toBeVisible();
});

test('shows a valid intermediate JobStatus as running instead of unknown', async () => {
    api.manage.getTaskDetail.mockResolvedValue({
        status: 200,
        data: {
            job_name: 'active_import',
            graphspace: 'DEFAULT',
            graph: 'hugegraph',
            job_status: 'LOADING',
        },
    });

    render(<ViewLayer visible task_id='9' onCancel={jest.fn()} />);

    expect(await screen.findByText('task.status.running')).toBeVisible();
    expect(screen.queryByText('task.status.unknown')).not.toBeInTheDocument();
    expect(screen.queryByText('LOADING')).not.toBeInTheDocument();
});

test('ships accurate Task Information copy in both languages', () => {
    expect(zhPages.task.action.config).toBe('查看任务信息');
    expect(enPages.task.action.config).toBe('View task information');
    expect(zhPages.task.view).toMatchObject({
        title: '任务信息',
        status: '状态',
        data_size: '数据量',
        duration: '持续时间',
        technical_details: '技术详情',
    });
    expect(enPages.task.view).toMatchObject({
        title: 'Task Information',
        status: 'Status',
        data_size: 'Data Size',
        duration: 'Duration',
        technical_details: 'Technical Details',
    });
});
