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

import {act, renderHook} from '@testing-library/react';
import useText2Gql from './useText2Gql';
import * as api from '../../../api';
import {setUser} from '../../../utils/user';

jest.mock('../../../api', () => ({analysis: {generateText2Gql: jest.fn()}}));
const deferred = () => {
    let resolve;
    const promise = new Promise(done => {
        resolve = done;
    });
    return {promise, resolve};
};
let props;
beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
    setUser({user_name: 'alice'});
    props = {
        scope: {graphSpace: 'DEFAULT', graph: 'one', routeGraphSpace: 'DEFAULT', routeGraph: 'one'},
        active: true, ready: true, execute: jest.fn().mockResolvedValue(),
        clearResult: jest.fn(), clearEditor: jest.fn(), t: key => key,
    };
});
const configure = result => {
    act(() => {
        result.current.updateConfig('provider', 'ollama');
        result.current.updateConfig('model', 'test-model');
        result.current.updateConfig('apiKey', 'private-key');
        result.current.setQuestion('Find people');
    });
};

test('automatically executes the returned Gremlin and keeps its original question when rerunning edits', async () => {
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {gremlin: 'g.V().limit(2)'}});
    const {result} = renderHook(() => useText2Gql(props));
    configure(result);
    await act(async () => result.current.generate());
    expect(props.execute).toHaveBeenCalledWith('g.V().limit(2)', 'Find people', expect.objectContaining({
        isCurrent: expect.any(Function), signal: expect.any(Object),
    }));
    act(() => result.current.setQuestion('A new question'));
    await act(async () => result.current.rerun('g.V().count()'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().count()', 'Find people', expect.any(Object));
});

test.each(['graph', 'routeGraph', 'active'])('discards generation after %s changes', async field => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValue(delayed.promise);
    const {result, rerender} = renderHook(value => useText2Gql(value), {initialProps: props});
    configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    const signal = api.analysis.generateText2Gql.mock.calls[0][3].signal;
    rerender(field === 'active' ? {...props, active: false}
        : {...props, scope: {...props.scope, [field]: 'two'}});
    expect(signal.aborted).toBe(true);
    await act(async () => {
        delayed.resolve({status: 200, data: {gremlin: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    expect(result.current.config.apiKey).toBe('private-key');
    expect(JSON.stringify({...sessionStorage, ...localStorage})).not.toContain('private-key');
});

test('cancel aborts and prevents subsequent automatic execution', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValue(delayed.promise);
    const {result} = renderHook(() => useText2Gql(props));
    configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    act(() => result.current.cancel());
    await act(async () => {
        delayed.resolve({status: 200, data: {gremlin: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    expect(result.current.error).toBe('analysis.query.text2gql_cancelled');
    expect(result.current.errorType).toBe('info');
});

test('stores only non-secret config and clears key and pending on identity change', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValue(delayed.promise);
    const {result} = renderHook(() => useText2Gql(props));
    configure(result);
    expect(sessionStorage.getItem('hubble.text2gql.alice')).toBe(JSON.stringify({
        provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'test-model',
    }));
    expect(JSON.stringify({...sessionStorage, ...localStorage})).not.toContain('private-key');
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    act(() => setUser({user_name: 'bob'}));
    expect(result.current.config.apiKey).toBe('');
    expect(result.current.pending).toBe(false);
    expect(props.clearEditor).toHaveBeenCalledWith('');
    await act(async () => {
        delayed.resolve({status: 200, data: {gremlin: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
});

test('model edits invalidate a pending generation and redact echoed keys from generation errors', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValueOnce(delayed.promise);
    const {result} = renderHook(() => useText2Gql(props));
    configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    act(() => result.current.updateConfig('model', 'another-model'));
    await act(async () => {
        delayed.resolve({status: 200, data: {gremlin: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    api.analysis.generateText2Gql.mockRejectedValue(new Error('invalid private-key'));
    await act(async () => result.current.generate());
    expect(result.current.error).not.toContain('private-key');
});

test('loading a historical question binds edited reruns to it and loading a favorite clears it', async () => {
    const {result} = renderHook(() => useText2Gql(props));
    configure(result);
    act(() => result.current.loadSource('Historical question'));
    expect(result.current.question).toBe('Historical question');
    await act(async () => result.current.rerun('g.V().limit(3)'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().limit(3)', 'Historical question', expect.any(Object));
    act(() => result.current.loadSource());
    expect(result.current.question).toBe('');
    await act(async () => result.current.rerun('g.V().count()'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().count()', '', expect.any(Object));
});
