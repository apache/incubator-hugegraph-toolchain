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

import {useCallback, useEffect, useRef, useState} from 'react';
import {scopedStorageKey, USER_CHANGE_EVENT} from '../../../utils/user';
import {sanitizePublicError} from '../../../utils/publicError';
import * as api from '../../../api';
import {MODEL_PROVIDERS} from '../../../utils/text2gql';

const restoreConfig = identity => {
    try {
        const saved = JSON.parse(sessionStorage.getItem(identity) || '{}');
        return {
            provider: Object.prototype.hasOwnProperty.call(MODEL_PROVIDERS, saved.provider) ? saved.provider : 'openai',
            baseUrl: typeof saved.baseUrl === 'string' ? saved.baseUrl : MODEL_PROVIDERS.openai,
            model: typeof saved.model === 'string' ? saved.model : '',
            apiKey: '',
        };
    }
    catch {
        return {provider: 'openai', baseUrl: MODEL_PROVIDERS.openai, model: '', apiKey: ''};
    }
};

export default function useText2Gql({scope, active, ready, execute, clearResult, clearEditor, t}) {
    const [identity, setIdentity] = useState(() => scopedStorageKey('hubble.text2gql'));
    const [config, setConfig] = useState(() => restoreConfig(identity));
    const [question, setQuestion] = useState('');
    const [sourceQuestion, setSourceQuestion] = useState('');
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');
    const [errorType, setErrorType] = useState('error');
    const requestRef = useRef(null);
    const currentScope = JSON.stringify([identity, scope.graphSpace, scope.graph,
        scope.routeGraphSpace, scope.routeGraph, active]);
    const scopeRef = useRef(currentScope);
    scopeRef.current = currentScope;

    const invalidate = useCallback(() => {
        requestRef.current?.controller.abort();
        requestRef.current = null;
        setPending(false);
    }, []);

    useEffect(() => {
        const onIdentityChange = () => {
            // Invalidate synchronously, before the identity change re-renders.
            invalidate();
            setConfig(restoreConfig(scopedStorageKey('hubble.text2gql')));
            setIdentity(scopedStorageKey('hubble.text2gql'));
            clearEditor('');
            setQuestion('');
            setSourceQuestion('');
            setError('');
            clearResult();
        };
        window.addEventListener(USER_CHANGE_EVENT, onIdentityChange);
        return () => window.removeEventListener(USER_CHANGE_EVENT, onIdentityChange);
    }, [clearEditor, clearResult, invalidate]);

    useEffect(() => {
        invalidate();
        setQuestion('');
        setSourceQuestion('');
        setError('');
        return () => {
            requestRef.current?.controller.abort();
            requestRef.current = null;
        };
    }, [currentScope, invalidate]);

    const updateConfig = useCallback((field, value) => {
        invalidate();
        clearResult();
        setError('');
        setConfig(previous => {
            const next = {...previous, [field]: value};
            if (field === 'provider') {
                next.baseUrl = MODEL_PROVIDERS[value];
                next.model = '';
                next.apiKey = '';
            }
            try {
                // Never serialize credentials, even to sessionStorage.
                const {provider, baseUrl, model} = next;
                sessionStorage.setItem(identity, JSON.stringify({provider, baseUrl, model}));
            }
            catch {
                // Model calls also work when browser storage is unavailable.
            }
            return next;
        });
    }, [clearResult, identity, invalidate]);

    const run = useCallback(async (content, text) => {
        if (!ready || !active || requestRef.current || (content !== undefined && !content.trim())) {
            return;
        }
        const request = {controller: new AbortController(), scope: currentScope};
        requestRef.current = request;
        const isCurrent = () => requestRef.current === request && scopeRef.current === request.scope;
        setPending(true);
        setError('');
        setErrorType('error');
        try {
            let gremlin = content;
            if (content === undefined) {
                clearResult();
                const response = await api.analysis.generateText2Gql(scope.graphSpace, scope.graph, {
                    question: text,
                    base_url: config.baseUrl.trim(),
                    api_key: config.apiKey,
                    model: config.model.trim(),
                }, {signal: request.controller.signal});
                if (!isCurrent()) {
                    return;
                }
                gremlin = response?.data?.gremlin;
                if (response?.status !== 200 || typeof gremlin !== 'string' || !gremlin.trim()) {
                    throw new Error(response?.message || t('analysis.query.text2gql_failed'));
                }
                setSourceQuestion(text);
            }
            if (!isCurrent()) {
                return;
            }
            await execute(gremlin, text, {signal: request.controller.signal, isCurrent});
        }
        catch (failure) {
            if (isCurrent()) {
                // Do not render server/provider errors that may echo a credential.
                const message = failure?.response?.data?.message || failure?.message;
                const safeMessage = config.apiKey && typeof message === 'string'
                    ? message.split(config.apiKey).join('[redacted]') : message;
                setError(sanitizePublicError(safeMessage, t('analysis.query.text2gql_failed')));
            }
        }
        finally {
            if (isCurrent()) {
                requestRef.current = null;
                setPending(false);
            }
        }
    }, [active, clearResult, config, currentScope, execute, ready, scope, t]);

    const cancel = useCallback(() => {
        invalidate();
        clearResult();
        setErrorType('info');
        setError(t('analysis.query.text2gql_cancelled'));
    }, [clearResult, invalidate, t]);

    return {
        config, updateConfig, question, setQuestion, pending, error, errorType,
        generate: () => run(undefined, question.trim()),
        rerun: content => run(content, sourceQuestion),
        cancel,
        loadSource: text => {
            invalidate();
            clearResult();
            setError('');
            const source = typeof text === 'string' ? text : '';
            setSourceQuestion(source);
            setQuestion(source);
        },
    };
}
