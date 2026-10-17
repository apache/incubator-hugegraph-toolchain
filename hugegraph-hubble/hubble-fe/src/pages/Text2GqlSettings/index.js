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
import {Alert, Button, Checkbox, Input, Select, Spin, Switch} from 'antd';
import {useTranslation} from 'react-i18next';
import {useAuthContext} from '../../auth/AuthContext';
import * as api from '../../api';
import {MODEL_PROVIDERS} from '../../utils/text2gql';
import {sanitizePublicError} from '../../utils/publicError';
import c from './index.module.scss';

const safeSettings = data => ({
    enabled: data.enabled === true,
    connection_type: data.connection_type === 'ollama' ? 'ollama' : 'openai',
    base_url: typeof data.base_url === 'string' ? data.base_url : MODEL_PROVIDERS.openai,
    model: typeof data.model === 'string' ? data.model : '',
    has_api_key: data.has_api_key === true,
    source: data.source === 'managed' ? 'managed' : 'deployment',
});

const Text2GqlSettingsForm = () => {
    const {t} = useTranslation();
    const {identityEpoch} = useAuthContext();
    const translateRef = useRef(t);
    translateRef.current = t;
    const [settings, setSettings] = useState();
    const [saved, setSaved] = useState();
    const [apiKey, setApiKey] = useState('');
    const [clearKey, setClearKey] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [feedback, setFeedback] = useState();
    const [revision, setRevision] = useState(0);
    const requestRef = useRef(null);
    const savePendingRef = useRef(false);
    const epochRef = useRef(identityEpoch);
    epochRef.current = identityEpoch;

    useEffect(() => {
        const request = {controller: new AbortController(), epoch: identityEpoch};
        requestRef.current = request;
        const current = () => requestRef.current === request && epochRef.current === request.epoch;
        setLoading(true);
        setSaving(false);
        savePendingRef.current = false;
        setSettings();
        setSaved();
        setApiKey('');
        setClearKey(false);
        setFeedback();
        api.config.getText2GqlSettings({signal: request.controller.signal})
            .then(response => {
                if (!current()) {
                    return;
                }
                if (response?.status !== 200 || !response.data) {
                    throw new Error(translateRef.current('analysis.model_settings.load_failed'));
                }
                const next = safeSettings(response.data);
                setSettings(next);
                setSaved(next);
            })
            .catch(() => {
                if (current()) {
                    setFeedback({type: 'error', message: translateRef.current('analysis.model_settings.load_failed')});
                }
            })
            .finally(() => {
                if (current()) {
                    setLoading(false);
                }
            });
        return () => {
            requestRef.current?.controller.abort();
            requestRef.current = null;
        };
    }, [identityEpoch, revision]);

    const update = useCallback((field, value) => {
        setFeedback();
        setSettings(previous => ({...previous, [field]: value}));
    }, []);
    const onEnabled = useCallback(value => update('enabled', value), [update]);
    const onBaseUrl = useCallback(event => update('base_url', event.target.value), [update]);
    const onModel = useCallback(event => update('model', event.target.value), [update]);
    const onKey = useCallback(event => setApiKey(event.target.value), []);
    const onClearKey = useCallback(event => setClearKey(event.target.checked), []);
    const onConnection = useCallback(value => {
        setSettings(previous => ({...previous, connection_type: value, base_url: MODEL_PROVIDERS[value], model: ''}));
        setApiKey('');
        setClearKey(false);
        setFeedback();
    }, []);
    const reload = useCallback(() => setRevision(previous => previous + 1), []);

    const urlChanged = settings?.base_url.trim() !== saved?.base_url.trim();
    const endpointNeedsKey = saved?.has_api_key && urlChanged && !clearKey && !apiKey.trim()
        && settings?.connection_type !== 'ollama';
    const requiresKey = endpointNeedsKey || (settings?.enabled && settings.connection_type !== 'ollama'
        && (!saved?.has_api_key || urlChanged || clearKey) && !apiKey.trim());
    const cannotSave = saving || loading || !settings || requiresKey || (settings.enabled
        && (!settings.base_url.trim() || !settings.model.trim()));

    const save = useCallback(async () => {
        if (cannotSave || savePendingRef.current) {
            return;
        }
        const request = {controller: new AbortController(), epoch: identityEpoch};
        requestRef.current = request;
        const current = () => requestRef.current === request && epochRef.current === request.epoch;
        const params = {
            enabled: settings.enabled, connection_type: settings.connection_type,
            base_url: settings.base_url.trim(), model: settings.model.trim(),
        };
        if (apiKey.trim()) {
            params.api_key = apiKey;
        }
        else if (clearKey || (urlChanged && settings.connection_type === 'ollama')) {
            params.clear_api_key = true;
        }
        savePendingRef.current = true;
        setSaving(true);
        setFeedback();
        try {
            const response = await api.config.updateText2GqlSettings(params, {signal: request.controller.signal});
            if (!current()) {
                return;
            }
            if (response?.status !== 200 || !response.data) {
                throw new Error(response?.message || t('analysis.model_settings.save_failed'));
            }
            const next = safeSettings(response.data);
            setSettings(next);
            setSaved(next);
            setApiKey('');
            setClearKey(false);
            setFeedback({type: 'success', message: t('analysis.model_settings.saved')});
        }
        catch (failure) {
            if (current()) {
                const detail = failure?.response?.data?.message || failure?.message;
                const redacted = apiKey && typeof detail === 'string'
                    ? detail.split(apiKey).join('[redacted]') : detail;
                setFeedback({type: 'error', message: sanitizePublicError(redacted,
                    t('analysis.model_settings.save_failed'))});
            }
        }
        finally {
            if (current()) {
                savePendingRef.current = false;
                setSaving(false);
            }
        }
    }, [apiKey, cannotSave, clearKey, identityEpoch, settings, t, urlChanged]);

    return (
        <main className={c.page}>
            <header>
                <h2>{t('analysis.model_settings.title')}</h2>
                <p>{t('analysis.model_settings.description')}</p>
            </header>
            {feedback && <Alert showIcon type={feedback.type} message={feedback.message} />}
            {loading ? <Spin /> : !settings ? (
                <Button onClick={reload}>{t('analysis.model_settings.retry')}</Button>
            ) : (
                <section className={c.form} aria-label={t('analysis.model_settings.title')}>
                    <div className={c.enabledRow}>
                        <div>
                            <label htmlFor='shared-model-enabled'>{t('analysis.model_settings.enabled')}</label>
                            <p>{t('analysis.model_settings.enabled_hint')}</p>
                        </div>
                        <Switch
                            id='shared-model-enabled'
                            checked={settings.enabled}
                            disabled={saving}
                            onChange={onEnabled}
                        />
                    </div>
                    <div className={c.field}>
                        <label htmlFor='shared-model-connection'>{t('analysis.query.connection_type')}</label>
                        <Select
                            id='shared-model-connection'
                            value={settings.connection_type}
                            disabled={saving}
                            onChange={onConnection}
                            options={[
                                {value: 'openai', label: t('analysis.query.connection_openai')},
                                {value: 'ollama', label: t('analysis.query.connection_ollama')},
                            ]}
                        />
                        {settings.connection_type === 'openai' && <p>{t('analysis.query.connection_openai_hint')}</p>}
                    </div>
                    <div className={c.field}>
                        <label htmlFor='shared-model-url' className={settings.enabled ? c.requiredLabel : undefined}>
                            Base URL
                        </label>
                        <Input
                            id='shared-model-url'
                            value={settings.base_url}
                            status={settings.enabled && !settings.base_url.trim() ? 'error' : undefined}
                            aria-invalid={settings.enabled && !settings.base_url.trim()}
                            aria-required={settings.enabled}
                            maxLength={2048}
                            disabled={saving}
                            onChange={onBaseUrl}
                        />
                        <p>{t('analysis.query.local_url_hint')}</p>
                    </div>
                    <div className={c.field}>
                        <label htmlFor='shared-model-name' className={settings.enabled ? c.requiredLabel : undefined}>
                            {t('analysis.query.model_name')}
                        </label>
                        <Input
                            id='shared-model-name'
                            value={settings.model}
                            status={settings.enabled && !settings.model.trim() ? 'error' : undefined}
                            aria-invalid={settings.enabled && !settings.model.trim()}
                            aria-required={settings.enabled}
                            maxLength={256}
                            disabled={saving}
                            onChange={onModel}
                        />
                    </div>
                    <div className={c.field}>
                        <label htmlFor='shared-model-key'>{t('analysis.model_settings.replace_key')}</label>
                        <Input.Password
                            id='shared-model-key'
                            value={apiKey}
                            maxLength={4096}
                            autoComplete='new-password'
                            disabled={saving || clearKey}
                            onChange={onKey}
                            aria-describedby='shared-model-key-hint'
                        />
                        <p id='shared-model-key-hint'>{t(saved.has_api_key
                            ? 'analysis.model_settings.key_configured' : 'analysis.model_settings.key_missing')}
                        </p>
                        <p>{t('analysis.model_settings.key_hint')}</p>
                        {saved.has_api_key && (
                            <Checkbox
                                checked={clearKey}
                                disabled={saving || Boolean(apiKey.trim())}
                                onChange={onClearKey}
                            >
                                {t('analysis.model_settings.clear_key')}
                            </Checkbox>
                        )}
                        {requiresKey && (
                            <p className={c.required} role='alert'>
                                {t(endpointNeedsKey ? 'analysis.model_settings.key_endpoint_changed'
                                    : 'analysis.model_settings.key_required')}
                            </p>
                        )}
                    </div>
                    <footer className={c.actions}>
                        <span className={c.source}>{t(saved.source === 'managed'
                            ? 'analysis.model_settings.source_managed' : 'analysis.model_settings.source_deployment')}
                        </span>
                        <Button type='primary' disabled={cannotSave} loading={saving} onClick={save}>
                            {t('analysis.model_settings.save')}
                        </Button>
                    </footer>
                </section>
            )}
        </main>
    );
};

const Text2GqlSettings = () => {
    const {loading, hasCapability} = useAuthContext();
    if (loading) {
        return <Spin />;
    }
    return hasCapability('model_settings_manage') ? <Text2GqlSettingsForm /> : null;
};

export default Text2GqlSettings;
