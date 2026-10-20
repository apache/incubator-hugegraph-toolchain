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

import {useCallback, useRef, useState} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import useAlgorithmSubmission from './useAlgorithmSubmission';
import Paths from './Oltp/Paths';
import Louvain from './Olap/Louvain';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';
import {GRAPH_STATUS} from '../../../utils/constants';

const mockForm = {submit: jest.fn(), validateFields: jest.fn(() => Promise.resolve())};

jest.mock('./PersistentForm', () => {
    const Form = ({children, onFinish}) => (
        <form aria-label='algorithm form' onSubmit={event => {
            event.preventDefault();
            onFinish({source: 'source', target: 'target'});
        }}>{children}</form>
    );
    Form.Item = ({children}) => <div>{children}</div>;
    Form.useForm = () => [mockForm];
    return Form;
});
jest.mock('./AlgorithmPanel', () => ({children}) => <div>{children}</div>);
jest.mock('./AlgorithmNameHeader', () => () => null);
jest.mock('./LimitItem', () => () => null);
jest.mock('./DirectionItem', () => () => null);
jest.mock('./Olap/OlapComputerItem', () => () => null);
jest.mock('../../../api', () => ({
    analysis: {runOltpInfo: jest.fn(), postOlapInfo: jest.fn()},
}));
jest.mock('react-i18next', () => ({
    ...jest.requireActual('react-i18next'),
    useTranslation: () => ({t: key => key}),
}));

beforeEach(() => {
    jest.clearAllMocks();
});

test.each([
    ['OLTP success', Paths, 'runOltpInfo', 200],
    ['OLTP failure', Paths, 'runOltpInfo', 400],
    ['OLAP success', Louvain, 'postOlapInfo', 200],
    ['OLAP failure', Louvain, 'postOlapInfo', 400],
])('%s keeps each async request bound to its captured completion', async (
    _name, FormComponent, method, lateStatus
) => {
    let resolveA;
    let resolveB;
    api.analysis[method]
        .mockReturnValueOnce(new Promise(resolve => { resolveA = resolve; }))
        .mockReturnValueOnce(new Promise(resolve => { resolveB = resolve; }));
    const completeA = jest.fn();
    const completeB = jest.fn();
    const begin = jest.fn();
    const FormWithOwnership = () => {
        const [result, setResult] = useState('empty');
        const latestRequest = useRef(0);
        // Match Home's existing rule: only the newest request may update the result.
        const beginSubmission = useCallback(() => {
            const request = ++latestRequest.current;
            begin(request);
            const report = request === 1 ? completeA : completeB;
            return (status, ...args) => {
                report(status, ...args);
                if (request === latestRequest.current && status !== GRAPH_STATUS.LOADING) {
                    setResult(`${status}:${args[1]}`);
                }
            };
        }, []);
        const handleFormSubmit = useAlgorithmSubmission(beginSubmission);
        return <>
            <FormComponent
                canRun
                handleFormSubmit={handleFormSubmit}
                updateCurrentAlgorithm={jest.fn()}
            />
            <output aria-label='algorithm result'>{result}</output>
        </>;
    };
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <FormWithOwnership />
        </GraphAnalysisContext.Provider>
    );
    fireEvent.submit(screen.getByRole('form', {name: 'algorithm form'}));
    fireEvent.submit(screen.getByRole('form', {name: 'algorithm form'}));
    expect(api.analysis[method]).toHaveBeenCalledTimes(2);

    await act(async () => resolveB({
        status: 200, message: 'new result',
        data: {graph_view: {vertices: [{id: 'b'}]}, task_id: 'task-b'},
    }));
    expect(screen.getByLabelText('algorithm result')).toHaveTextContent(
        `${GRAPH_STATUS.SUCCESS}:new result`
    );
    await act(async () => resolveA({
        status: lateStatus, message: 'old result',
        data: {graph_view: {vertices: [{id: 'a'}]}, task_id: 'task-a'},
    }));

    expect(screen.getByLabelText('algorithm result')).toHaveTextContent(
        `${GRAPH_STATUS.SUCCESS}:new result`
    );
    expect(begin.mock.calls).toEqual([[1], [2]]);
    const isOltp = method === 'runOltpInfo';
    expect(completeB.mock.calls[0]).toEqual([GRAPH_STATUS.LOADING]);
    expect(completeB).toHaveBeenCalledTimes(2);
    expect(completeB.mock.calls[1]).toEqual(isOltp
        ? [GRAPH_STATUS.SUCCESS, {vertices: [{id: 'b'}]}, 'new result', {}]
        : [GRAPH_STATUS.SUCCESS, 'task-b', 'new result']);
    expect(completeA.mock.calls[0]).toEqual([GRAPH_STATUS.LOADING]);
    expect(completeA).toHaveBeenCalledTimes(2);
    expect(completeA.mock.calls[1]).toEqual(lateStatus === 200
        ? (isOltp
            ? [GRAPH_STATUS.SUCCESS, {vertices: [{id: 'a'}]}, 'old result', {}]
            : [GRAPH_STATUS.SUCCESS, 'task-a', 'old result'])
        : [GRAPH_STATUS.FAILED, isOltp ? {} : '', 'old result']);
});
