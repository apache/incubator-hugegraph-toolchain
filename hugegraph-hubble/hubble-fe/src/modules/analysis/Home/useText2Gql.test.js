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

import {useState} from 'react';
import {act, renderHook, waitFor} from '@testing-library/react';
import useText2Gql from './useText2Gql';
import * as api from '../../../api';
import {setUser} from '../../../utils/user';

jest.mock('../../../api', () => ({analysis: {generateText2Gql: jest.fn(), getText2GqlConfig: jest.fn()}}));
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
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {available: false, reason: 'not_configured'}});
    sessionStorage.clear();
    localStorage.clear();
    setUser({user_name: 'alice'});
    props = {
        scope: {graphSpace: 'DEFAULT', graph: 'one', routeGraphSpace: 'DEFAULT', routeGraph: 'one'},
        active: true, ready: true, cypherEnabled: true, execute: jest.fn().mockResolvedValue(),
        clearResult: jest.fn(), clearEditor: jest.fn(), t: key => key,
    };
});
const configure = async result => {
    await waitFor(() => expect(result.current.sharedConfig.loading).toBe(false));
    act(() => {
        result.current.updateConfig('provider', 'ollama');
        result.current.updateConfig('model', 'test-model');
        result.current.updateConfig('apiKey', 'private-key');
        result.current.setQuestion('Find people');
    });
};

test('automatically executes the returned Gremlin and keeps its original question when rerunning edits', async () => {
    api.analysis.generateText2Gql.mockResolvedValue({status: 200,
        data: {language: 'gremlin', query: 'g.V().limit(2)'}});
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
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
    await configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    const signal = api.analysis.generateText2Gql.mock.calls[0][3].signal;
    rerender(field === 'active' ? {...props, active: false}
        : {...props, scope: {...props.scope, [field]: 'two'}});
    expect(signal.aborted).toBe(true);
    await act(async () => {
        delayed.resolve({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
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
    await configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    act(() => result.current.cancel());
    await act(async () => {
        delayed.resolve({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
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
    await configure(result);
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
        delayed.resolve({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
});

test('model edits invalidate a pending generation and redact echoed keys from generation errors', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValueOnce(delayed.promise);
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    act(() => result.current.updateConfig('model', 'another-model'));
    await act(async () => {
        delayed.resolve({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    api.analysis.generateText2Gql.mockRejectedValue(new Error('invalid private-key'));
    await act(async () => result.current.generate());
    expect(result.current.error).not.toContain('private-key');
});

test('loading a historical question binds edited reruns to it and loading a favorite clears it', async () => {
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
    act(() => result.current.loadSource('Historical question'));
    expect(result.current.question).toBe('Historical question');
    await act(async () => result.current.rerun('g.V().limit(3)'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().limit(3)', 'Historical question', expect.any(Object));
    act(() => result.current.loadSource());
    expect(result.current.question).toBe('');
    await act(async () => result.current.rerun('g.V().count()'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().count()', '', expect.any(Object));
});


test.each([
    ['deepseek', 'https://api.deepseek.com/v1'],
    ['glm', 'https://open.bigmodel.cn/api/paas/v4'],
    ['openai', 'https://custom-model.example/v1'],
])('restores a legacy %s preset as OpenAI-compatible without losing URL or model', async (provider, baseUrl) => {
    sessionStorage.setItem('hubble.text2gql.alice', JSON.stringify({provider, baseUrl, model: 'saved-model'}));
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {language: 'gremlin', query: 'g.V().count()'}});
    const {result} = renderHook(() => useText2Gql(props));
    expect(result.current.config).toEqual({provider: 'openai', baseUrl, model: 'saved-model', apiKey: ''});
    act(() => {
        result.current.updateConfig('apiKey', 'private-key');
        result.current.setQuestion('Count vertices');
    });
    await waitFor(() => expect(result.current.sharedConfig.loading).toBe(false));
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenCalledWith('DEFAULT', 'one', {
        question: 'Count vertices', source: 'custom', language: 'gremlin', base_url: baseUrl,
        model: 'saved-model', api_key: 'private-key',
    }, expect.any(Object));
    expect(JSON.parse(sessionStorage.getItem('hubble.text2gql.alice'))).toEqual({
        provider: 'openai', baseUrl, model: 'saved-model',
    });
});


test('defaults to the available shared model and sends no temporary URL, model or key', async () => {
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {available: true, model: 'shared-model'}});
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {language: 'gremlin', query: 'g.V().count()'}});
    const {result} = renderHook(() => useText2Gql(props));
    await waitFor(() => expect(result.current.source).toBe('shared'));
    await configure(result);
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenCalledWith('DEFAULT', 'one', {
        question: 'Find people', source: 'shared', language: 'gremlin',
    }, expect.any(Object));
    expect(result.current.sharedConfig.model).toBe('shared-model');
    expect(props.execute).toHaveBeenCalledWith('g.V().count()', 'Find people', expect.any(Object));
});

test('switching model source aborts the old generation and keeps shared and custom credentials separate', async () => {
    const pending = deferred();
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {available: true, model: 'shared-model'}});
    api.analysis.generateText2Gql.mockReturnValueOnce(pending.promise);
    const {result} = renderHook(() => useText2Gql(props));
    await waitFor(() => expect(result.current.source).toBe('shared'));
    await configure(result);
    let generation;
    act(() => {
        generation = result.current.generate();
    });
    const signal = api.analysis.generateText2Gql.mock.calls[0][3].signal;
    act(() => result.current.changeSource('custom'));
    expect(signal.aborted).toBe(true);
    await act(async () => {
        pending.resolve({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
        await generation;
    });
    expect(props.execute).not.toHaveBeenCalled();
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {language: 'gremlin', query: 'g.E()'}});
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenLastCalledWith('DEFAULT', 'one', {
        question: 'Find people', source: 'custom', language: 'gremlin', base_url: 'http://localhost:11434/v1',
        model: 'test-model', api_key: 'private-key',
    }, expect.any(Object));
});

test('discards metadata from the previous graph before it can select a shared model', async () => {
    const oldMetadata = deferred();
    api.analysis.getText2GqlConfig.mockReturnValueOnce(oldMetadata.promise)
        .mockResolvedValue({status: 200, data: {available: false, reason: 'not_configured'}});
    const {result, rerender} = renderHook(value => useText2Gql(value), {initialProps: props});
    const signal = api.analysis.getText2GqlConfig.mock.calls[0][2].signal;
    rerender({...props, scope: {...props.scope, graph: 'two', routeGraph: 'two'}});
    expect(signal.aborted).toBe(true);
    await waitFor(() => expect(result.current.sharedConfig.loading).toBe(false));
    await act(async () => oldMetadata.resolve({status: 200, data: {available: true, model: 'old-model'}}));
    expect(result.current.source).toBe('custom');
    expect(result.current.sharedConfig.available).toBe(false);
    expect(result.current.sharedConfig.model).not.toBe('old-model');
});


test('aborts and discards shared metadata when the signed-in account changes', async () => {
    const oldMetadata = deferred();
    api.analysis.getText2GqlConfig.mockReturnValueOnce(oldMetadata.promise)
        .mockResolvedValue({status: 200, data: {available: false, reason: 'not_configured'}});
    const {result} = renderHook(() => useText2Gql(props));
    const signal = api.analysis.getText2GqlConfig.mock.calls[0][2].signal;
    act(() => setUser({user_name: 'bob'}));
    expect(signal.aborted).toBe(true);
    await waitFor(() => expect(result.current.sharedConfig.loading).toBe(false));
    await act(async () => oldMetadata.resolve({status: 200, data: {available: true, model: 'old-model'}}));
    expect(result.current.source).toBe('custom');
    expect(result.current.sharedConfig.available).toBe(false);
});


test('does not call generation when the backend denies temporary custom access', async () => {
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {
        available: false, custom_available: false, reason: 'authentication_required',
    }});
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
    expect(result.current.sharedConfig.customAvailable).toBe(false);
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).not.toHaveBeenCalled();
    expect(props.execute).not.toHaveBeenCalled();
});

test('allows the shared model while custom access is disabled and refuses switching to custom', async () => {
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {
        available: true, custom_available: false, model: 'shared-model',
    }});
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {language: 'gremlin', query: 'g.V()'}});
    const {result} = renderHook(() => useText2Gql(props));
    await waitFor(() => expect(result.current.source).toBe('shared'));
    act(() => {
        result.current.changeSource('custom');
        result.current.setQuestion('Find vertices');
    });
    expect(result.current.source).toBe('shared');
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenCalledWith('DEFAULT', 'one', {
        source: 'shared', language: 'gremlin', question: 'Find vertices',
    }, expect.any(Object));
});

test('honors explicit custom access from deployments that allow it', async () => {
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {
        available: false, custom_available: true, reason: 'not_configured',
    }});
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {language: 'gremlin', query: 'g.E()'}});
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenCalledWith('DEFAULT', 'one', {
        source: 'custom', language: 'gremlin', question: 'Find people', base_url: 'http://localhost:11434/v1',
        model: 'test-model', api_key: 'private-key',
    }, expect.any(Object));
});

test.each(['loading', 'failure', 'invalid'])(
    'keeps custom generation unavailable without valid capability metadata (%s)', async mode => {
        const pending = deferred();
        if (mode === 'loading') {
            api.analysis.getText2GqlConfig.mockReturnValue(pending.promise);
        }
        else if (mode === 'failure') {
            api.analysis.getText2GqlConfig.mockRejectedValue(new Error('unavailable'));
        }
        else {
            api.analysis.getText2GqlConfig.mockResolvedValue({status: 503, data: {available: false}});
        }
        const {result} = renderHook(() => useText2Gql(props));
        if (mode !== 'loading') {
            await waitFor(() => expect(result.current.sharedConfig.loading).toBe(false));
        }
        act(() => result.current.setQuestion('Find vertices'));
        await act(async () => result.current.generate());
        expect(result.current.sharedConfig.customAvailable).toBe(false);
        expect(api.analysis.generateText2Gql).not.toHaveBeenCalled();
    }
);


test('switching language cancels old generation while preserving question, config and dialect drafts', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValueOnce(delayed.promise);
    const {result} = renderHook(() => {
        const [language, setLanguage] = useState('gremlin');
        const [editorContent, setEditorContent] = useState('g.V().limit(4)');
        return useText2Gql({...props, language, onLanguageChange: setLanguage,
            editorContent, clearEditor: setEditorContent});
    });
    await configure(result);
    act(() => result.current.loadSource('Original question'));
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    const signal = api.analysis.generateText2Gql.mock.calls[0][3].signal;
    act(() => result.current.changeLanguage('cypher'));
    expect(signal.aborted).toBe(true);
    expect(result.current.language).toBe('cypher');
    expect(result.current.question).toBe('Original question');
    expect(result.current.config.apiKey).toBe('private-key');
    await act(async () => {
        delayed.resolve({status: 200, data: {query: 'g.V()', language: 'gremlin'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    act(() => result.current.changeLanguage('gremlin'));
    await act(async () => result.current.rerun('g.V().limit(4)'));
    expect(props.execute).toHaveBeenLastCalledWith('g.V().limit(4)', 'Original question',
        expect.objectContaining({language: 'gremlin'}));
});

test('generates and executes Cypher through the same flow and refuses a mismatched dialect response', async () => {
    const query = 'MATCH (n) RETURN n LIMIT 3';
    props.language = 'cypher';
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {query, language: 'cypher'}});
    const {result} = renderHook(() => useText2Gql(props));
    await configure(result);
    await act(async () => result.current.generate());
    expect(api.analysis.generateText2Gql).toHaveBeenCalledWith('DEFAULT', 'one', expect.objectContaining({
        question: 'Find people', language: 'cypher',
    }), expect.any(Object));
    expect(props.execute).toHaveBeenCalledWith(query, 'Find people', expect.objectContaining({language: 'cypher'}));
    props.execute.mockClear();
    api.analysis.generateText2Gql.mockResolvedValue({status: 200, data: {query: 'g.V()', language: 'gremlin'}});
    await act(async () => result.current.generate());
    expect(props.execute).not.toHaveBeenCalled();
    expect(result.current.error).toBe('analysis.query.text2gql_failed');
});


test('refuses Cypher switching and generation without server capability', async () => {
    props.cypherEnabled = false;
    props.onLanguageChange = jest.fn();
    const {result, rerender} = renderHook(value => useText2Gql(value), {initialProps: props});
    await configure(result);
    act(() => result.current.changeLanguage('cypher'));
    expect(props.onLanguageChange).not.toHaveBeenCalled();
    rerender({...props, language: 'cypher'});
    expect(props.onLanguageChange).toHaveBeenCalledWith('gremlin');
    await act(async () => result.current.generate());
    await act(async () => result.current.rerun('MATCH (n) RETURN n'));
    expect(api.analysis.generateText2Gql).not.toHaveBeenCalled();
    expect(props.execute).not.toHaveBeenCalled();
});

test('revoking Cypher aborts generation and clears stale execution context before reverting to Gremlin', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValueOnce(delayed.promise);
    const {result, rerender} = renderHook(({cypherEnabled}) => {
        const [language, setLanguage] = useState('gremlin');
        const [editorContent, setEditorContent] = useState('');
        return useText2Gql({...props, language, cypherEnabled, onLanguageChange: setLanguage,
            editorContent, clearEditor: setEditorContent});
    }, {initialProps: {cypherEnabled: false}});
    await configure(result);
    act(() => result.current.changeLanguage('cypher'));
    expect(result.current.language).toBe('gremlin');
    rerender({cypherEnabled: true});
    act(() => result.current.changeLanguage('cypher'));
    expect(result.current.language).toBe('cypher');
    act(() => result.current.loadSource('Old Cypher question'));
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    const signal = api.analysis.generateText2Gql.mock.calls[0][3].signal;
    props.clearResult.mockClear();
    rerender({cypherEnabled: false});
    expect(signal.aborted).toBe(true);
    expect(result.current.pending).toBe(false);
    expect(result.current.language).toBe('gremlin');
    expect(props.clearResult).toHaveBeenCalled();
    await act(async () => {
        delayed.resolve({status: 200, data: {query: 'MATCH (n) RETURN n', language: 'cypher'}});
        await pending;
    });
    expect(props.execute).not.toHaveBeenCalled();
    await act(async () => result.current.rerun('g.V().limit(1)'));
    expect(props.execute).toHaveBeenCalledWith('g.V().limit(1)', '',
        expect.objectContaining({language: 'gremlin'}));
});

test('keeps a valid Gremlin request current when only Cypher capability changes', async () => {
    const delayed = deferred();
    api.analysis.generateText2Gql.mockReturnValueOnce(delayed.promise);
    const {result, rerender} = renderHook(value => useText2Gql(value), {initialProps: props});
    await configure(result);
    let pending;
    act(() => {
        pending = result.current.generate();
    });
    rerender({...props, cypherEnabled: false});
    await act(async () => {
        delayed.resolve({status: 200, data: {query: 'g.V()', language: 'gremlin'}});
        await pending;
    });
    expect(props.execute).toHaveBeenCalledWith('g.V()', 'Find people',
        expect.objectContaining({language: 'gremlin'}));
    expect(result.current.pending).toBe(false);
});
