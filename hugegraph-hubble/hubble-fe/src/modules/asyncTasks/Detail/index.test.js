/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import GraphAnalysisContext from '../../Context';
import AsyncTaskDetail from './index';

jest.mock('../../../api/index', () => ({
    analysis: {
        deleteAsyncTask: jest.fn(),
        abortAsyncTask: jest.fn(),
    },
}));
jest.mock('lodash-es', () => ({
    intersection: (left, right) => left.filter(value => right.includes(value)),
    size: value => value.length,
}));
jest.mock('../Result', () => ({
    AsyncTaskResultContent: ({taskId}) => (
        <div data-testid="inline-result">result {taskId}</div>
    ),
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

const renderDetail = () => render(
    <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
        <GraphAnalysisContext.Provider
            value={{
                graphSpace: 'DEFAULT',
                graph: 'hugegraph',
                isVermeer: false,
            }}
        >
            <AsyncTaskDetail
                page={1}
                pageSize={10}
                onPageChange={jest.fn()}
                getAsynTaskList={jest.fn()}
                asyncManageTaskData={{
                    records: [{
                        id: 4,
                        task_name: 'g.V().count()',
                        task_type: 'gremlin',
                        task_status: 'success',
                        task_create: 1787991956000,
                        task_update: 1787991957000,
                    }],
                    total: 1,
                }}
                loading={false}
            />
        </GraphAnalysisContext.Provider>
    </MemoryRouter>
);

it('controls row expansion without opening a new browser tab', () => {
    window.open = jest.fn();
    renderDetail();

    const view = screen.getByRole('button', {
        name: 'analysis.async_task.action.check_result',
    });
    expect(view).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(view);

    expect(screen.getByRole('button', {
        name: 'analysis.async_task.action.collapse_result',
    })).toHaveAttribute('aria-expanded', 'true');
    expect(document.querySelector('.ant-table-row-expand-icon'))
        .toHaveClass('ant-table-row-expand-icon-expanded');
    expect(window.open).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', {
        name: 'analysis.async_task.action.collapse_result',
    }));
    expect(document.querySelector('.ant-table-row-expand-icon'))
        .not.toHaveClass('ant-table-row-expand-icon-expanded');
});
