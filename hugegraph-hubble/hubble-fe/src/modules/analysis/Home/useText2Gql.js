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
import {DEFAULT_QUERY_LANGUAGE, MODEL_PROVIDERS, supportsQueryLanguage} from '../../../utils/text2gql';

const restoreConfig = identity => {
    try {
        const saved = JSON.parse(sessionStorage.getItem(identity) || '{}');
        // Previous DeepSeek and GLM presets use the same OpenAI-compatible protocol.
        const provider = saved.provider === 'ollama' ? 'ollama' : 'openai';
        return {
            provider,
            baseUrl: typeof saved.baseUrl === 'string' ? saved.baseUrl : MODEL_PROVIDERS[provider],
            model: typeof saved.model === 'string' ? saved.model : '',
            apiKey: '',
        };
    }
    catch {
        return {provider: 'openai', baseUrl: MODEL_PROVIDERS.openai, model: '', apiKey: ''};
    }
};

export default function useText2Gql({scope, active, ready, execute, clearResult, clearEditor, t,
    language = DEFAULT_QUERY_LANGUAGE, cypherEnabled = false, onLanguageChange, editorContent = ''}) {
    const [identity, setIdentity] = useState(() => scopedStorageKey('hubble.text2gql'));
    const [identityVersion, setIdentityVersion] = useState(0);
    const [source, setSource] = useState('custom');
    const [sharedMetadata, setSharedMetadata] = useState({
        scope: null, loading: false, available: false, customAvailable: false,
    });
    const metadataRequest = useRef(null);
    const [config, setConfig] = useState(() => restoreConfig(identity));
    const [question, setQuestion] = useState('');
    const [sourceQuestion, setSourceQuestion] = useState('');
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');
    const [errorType, setErrorType] = useState('error');
    const [errorStage, setErrorStage] = useState('');
    const requestRef = useRef(null);
    const languageDrafts = useRef({});
    const connectionScope = JSON.stringify([identity, identityVersion, scope.graphSpace, scope.graph,
        scope.routeGraphSpace, scope.routeGraph, active]);
    const baseScopeRef = useRef(connectionScope);
    baseScopeRef.current = connectionScope;
    const currentScope = JSON.stringify([connectionScope, source, language,
        supportsQueryLanguage(language, cypherEnabled)]);
    const sharedConfig = sharedMetadata.scope === connectionScope ? sharedMetadata
        : {loading: active && ready, available: false, customAvailable: false};
    const scopeRef = useRef(currentScope);
    scopeRef.current = currentScope;

    const invalidate = useCallback(() => {
        requestRef.current?.controller.abort();
        requestRef.current = null;
        setPending(false);
    }, []);

    useEffect(() => {
        if (supportsQueryLanguage(language, cypherEnabled)) {
            return;
        }
        invalidate();
        languageDrafts.current = {};
        setSourceQuestion('');
        clearEditor('');
        clearResult();
        setError('');
        setErrorStage('');
        onLanguageChange?.(DEFAULT_QUERY_LANGUAGE);
    }, [clearEditor, clearResult, cypherEnabled, invalidate, language, onLanguageChange]);

    useEffect(() => {
        const onIdentityChange = () => {
            // Invalidate synchronously, before the identity change re-renders.
            invalidate();
            metadataRequest.current?.controller.abort();
            metadataRequest.current = null;
            setIdentityVersion(previous => previous + 1);
            setSharedMetadata({scope: null, loading: false, available: false, customAvailable: false});
            setSource('custom');
            languageDrafts.current = {};
            setConfig(restoreConfig(scopedStorageKey('hubble.text2gql')));
            setIdentity(scopedStorageKey('hubble.text2gql'));
            clearEditor('');
            setQuestion('');
            setSourceQuestion('');
            setError('');
            setErrorStage('');
            clearResult();
        };
        window.addEventListener(USER_CHANGE_EVENT, onIdentityChange);
        return () => window.removeEventListener(USER_CHANGE_EVENT, onIdentityChange);
    }, [clearEditor, clearResult, invalidate]);

    useEffect(() => {
        invalidate();
        languageDrafts.current = {};
        setQuestion('');
        setSourceQuestion('');
        setError('');
        setErrorStage('');
        return () => {
            requestRef.current?.controller.abort();
            requestRef.current = null;
        };
    }, [connectionScope, invalidate]);

    useEffect(() => {
        if (!active || !ready) {
            return undefined;
        }
        const request = {controller: new AbortController(), scope: connectionScope};
        metadataRequest.current = request;
        const isCurrent = () => metadataRequest.current === request
            && baseScopeRef.current === request.scope;
        setSharedMetadata({scope: connectionScope, loading: true, available: false, customAvailable: false});
        api.analysis.getText2GqlConfig(scope.graphSpace, scope.graph, {signal: request.controller.signal})
            .then(response => {
                if (!isCurrent()) {
                    return;
                }
                const data = response?.data;
                const valid = response?.status === 200 && typeof data?.available === 'boolean';
                const available = valid && data.available;
                // Older successful metadata responses predate the separate custom capability.
                const customAvailable = valid
                    && (data.custom_available === true || data.custom_available === undefined);
                setSharedMetadata({scope: connectionScope, loading: false, available, customAvailable,
                    model: available && typeof data.model === 'string' ? data.model : '',
                    reason: available ? undefined : data?.reason || 'unavailable'});
                if (available) {
                    invalidate();
                }
                setSource(available ? 'shared' : 'custom');
            })
            .catch(() => {
                if (isCurrent()) {
                    setSharedMetadata({scope: connectionScope, loading: false,
                        available: false, customAvailable: false, reason: 'unavailable'});
                    setSource('custom');
                }
            });
        return () => {
            request.controller.abort();
            if (metadataRequest.current === request) {
                metadataRequest.current = null;
            }
        };
    }, [active, connectionScope, invalidate, ready, scope.graph, scope.graphSpace]);

    const changeSource = useCallback(value => {
        if (!((value === 'custom' && sharedConfig.customAvailable)
            || (value === 'shared' && sharedConfig.available))) {
            return;
        }
        invalidate();
        setSource(value);
        languageDrafts.current = {};
        setSourceQuestion('');
        setError('');
        setErrorStage('');
        clearResult();
        clearEditor('');
    }, [clearEditor, clearResult, invalidate, sharedConfig.available, sharedConfig.customAvailable]);

    const changeLanguage = useCallback(value => {
        if (!supportsQueryLanguage(value, cypherEnabled) || value === language) {
            return;
        }
        invalidate();
        languageDrafts.current[language] = {content: editorContent, sourceQuestion};
        const draft = languageDrafts.current[value];
        setSourceQuestion(draft?.sourceQuestion || '');
        clearEditor(draft?.content || '');
        clearResult();
        setError('');
        setErrorStage('');
        onLanguageChange?.(value);
    }, [clearEditor, clearResult, cypherEnabled, editorContent, invalidate, language,
        onLanguageChange, sourceQuestion]);

    const updateConfig = useCallback((field, value) => {
        invalidate();
        clearResult();
        setError('');
        setErrorStage('');
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
        if (!supportsQueryLanguage(language, cypherEnabled) || !ready || !active || requestRef.current
            || (content !== undefined && !content.trim())) {
            return;
        }
        if (content === undefined && (sharedConfig.loading
            || (source === 'shared' ? !sharedConfig.available : !sharedConfig.customAvailable))) {
            return;
        }
        const request = {controller: new AbortController(), scope: currentScope};
        requestRef.current = request;
        const isCurrent = () => requestRef.current === request && scopeRef.current === request.scope;
        setPending(true);
        setError('');
        setErrorStage('');
        setErrorType('error');
        let stage = content === undefined ? 'generation' : 'execution';
        try {
            let query = content;
            if (content === undefined) {
                clearResult();
                const params = source === 'shared' ? {question: text, source: 'shared', language} : {
                    question: text, source: 'custom', language, base_url: config.baseUrl.trim(),
                    api_key: config.apiKey, model: config.model.trim(),
                };
                const response = await api.analysis.generateText2Gql(scope.graphSpace, scope.graph,
                    params, {signal: request.controller.signal});
                if (!isCurrent()) {
                    return;
                }
                query = response?.data?.query;
                if (response?.status !== 200 || response?.data?.language !== language
                    || typeof query !== 'string' || !query.trim()) {
                    throw new Error(response?.message || t('analysis.query.text2gql_failed'));
                }
                setSourceQuestion(text);
            }
            if (!isCurrent()) {
                return;
            }
            stage = 'execution';
            await execute(query, text, {signal: request.controller.signal, isCurrent, language});
        }
        catch (failure) {
            if (isCurrent()) {
                // Do not render server/provider errors that may echo a credential.
                const message = failure?.response?.data?.message || failure?.message;
                const safeMessage = config.apiKey && typeof message === 'string'
                    ? message.split(config.apiKey).join('[redacted]') : message;
                setErrorStage(stage);
                setError(sanitizePublicError(safeMessage, t('analysis.query.text2gql_failed')));
            }
        }
        finally {
            if (isCurrent()) {
                requestRef.current = null;
                setPending(false);
            }
        }
    }, [active, clearResult, config, currentScope, cypherEnabled, execute, ready, scope, sharedConfig.available,
        sharedConfig.customAvailable, sharedConfig.loading, source, language, t]);

    const cancel = useCallback(() => {
        invalidate();
        clearResult();
        setErrorStage('');
        setErrorType('info');
        setError(t('analysis.query.text2gql_cancelled'));
    }, [clearResult, invalidate, t]);

    return {
        config, updateConfig, question, setQuestion, pending, error, errorType, errorStage,
        source, changeSource, sharedConfig, language, changeLanguage,
        generate: () => run(undefined, question.trim()),
        rerun: content => run(content, sourceQuestion),
        cancel,
        loadSource: text => {
            invalidate();
            clearResult();
            setError('');
            setErrorStage('');
            const source = typeof text === 'string' ? text : '';
            setSourceQuestion(source);
            setQuestion(source);
        },
    };
}
