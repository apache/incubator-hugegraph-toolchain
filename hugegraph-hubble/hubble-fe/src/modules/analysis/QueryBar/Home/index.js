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

/**
 * @file Gremlin语法分析 Header
 */

import React, {useCallback, useEffect, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Alert, Button, Input, Select, Space, Tabs, Tooltip} from 'antd';
import {DownOutlined, SwapOutlined, UpOutlined} from '@ant-design/icons';
import {DEFAULT_QUERY_LANGUAGE, MODEL_PROVIDERS, QUERY_LANGUAGES,
    supportsQueryLanguage} from '../../../../utils/text2gql';
import CodeEditor from '../../../../components/CodeEditor';
import ModelHistoryInput from '../../../../components/ModelHistoryInput';
import {ANALYSIS_TYPE} from '../../../../utils/constants';
import {PrimaryActions, SecondaryActions} from '../ContentCommon';
import c from './index.module.scss';

const {GREMLIN, CYPHER, TEXT2GQL} = ANALYSIS_TYPE;

const QueryBar = props => {
    const {t} = useTranslation();
    const {...args} = props;

    const {
        codeEditorContent,
        setCodeEditorContent,
        activeTab,
        onTabsChange,
        onExecute,
        isExecuting,
        isGraphReady = true,
        cypherEnabled = false,
        text2gql,
    } = args;

    const [isEmptyQuery, setIsEmptyQuery] = useState(() => !codeEditorContent);
    const [favoriteCardVisible, setFavoriteCardVisible] = useState(false);
    const [isEditorExpanded, setEditorExpanded] = useState(true);
    const [modelSettingsOpen, setModelSettingsOpen] = useState(false);
    const [configPrompt, setConfigPrompt] = useState(false);
    const modelSettingsRef = useRef(null);
    useEffect(() => {
        setIsEmptyQuery(!codeEditorContent);
    }, [codeEditorContent]);

    const handleCodeEditorChange = useCallback(
        value => {
            const existQuery = Boolean(value);
            setCodeEditorContent(value);
            setIsEmptyQuery(!existQuery);
            if (!existQuery) {
                setFavoriteCardVisible(false);
            }
        },
        [setCodeEditorContent]
    );

    const handleTabsChange = useCallback(
        nextTab => {
            setFavoriteCardVisible(false);
            onTabsChange(nextTab);
        },
        [onTabsChange]
    );

    const onExecution = useCallback(
        () => {
            if (isGraphReady && !isEmptyQuery && !isExecuting) {
                onExecute(activeTab);
            }
        },
        [activeTab, isEmptyQuery, isExecuting, isGraphReady, onExecute]
    );

    const toggleEditor = useCallback(() => {
        setEditorExpanded(expanded => !expanded);
    }, []);

    const executingGenerated = text2gql?.pending && text2gql.phase === 'execution';
    const language = text2gql?.language || DEFAULT_QUERY_LANGUAGE;
    const dialect = QUERY_LANGUAGES[language];
    const languageEnabled = supportsQueryLanguage(language, cypherEnabled);
    const onLanguageChange = useCallback(value => {
        if (supportsQueryLanguage(value, cypherEnabled)) {
            text2gql.changeLanguage(value);
        }
    }, [cypherEnabled, text2gql]);
    const source = text2gql?.source || 'custom';
    const sharedConfig = text2gql?.sharedConfig || {available: false, loading: false};
    const customAvailable = sharedConfig.customAvailable !== false;
    const nextLanguage = Object.keys(QUERY_LANGUAGES).find(value => (
        value !== language && supportsQueryLanguage(value, cypherEnabled)
    ));
    const nextSource = source === 'shared' ? 'custom' : 'shared';
    const sourceLabel = value => t(`analysis.query.source_${value}`);
    const canSwitchSource = !sharedConfig.loading && (
        nextSource === 'shared' ? sharedConfig.available : customAvailable
    );
    const sourceSwitchHint = sharedConfig.loading ? t('analysis.query.shared_loading')
        : canSwitchSource ? t('analysis.query.switch_to', {name: sourceLabel(nextSource)})
            : t(nextSource === 'custom' ? 'analysis.query.custom_unavailable'
                : sharedConfig.reason === 'authentication_required' ? 'analysis.query.shared_auth_required'
                    : sharedConfig.reason === 'not_configured' ? 'analysis.query.shared_not_configured'
                        : 'analysis.query.shared_unavailable');
    const onSourceChange = useCallback(value => {
        setModelSettingsOpen(false);
        setConfigPrompt(false);
        text2gql.changeSource(value);
    }, [text2gql]);
    const toggleLanguage = useCallback(() => onLanguageChange(nextLanguage), [nextLanguage, onLanguageChange]);
    const toggleSource = useCallback(() => {
        if (canSwitchSource) {
            onSourceChange(nextSource);
        }
    }, [canSwitchSource, nextSource, onSourceChange]);
    const onProviderChange = useCallback(value => text2gql.updateConfig('provider', value), [text2gql]);
    const onBaseUrlChange = useCallback(event => text2gql.updateConfig('baseUrl', event.target.value), [text2gql]);
    const onModelChange = useCallback(event => text2gql.updateConfig('model', event.target.value), [text2gql]);
    const onApiKeyChange = useCallback(event => text2gql.updateConfig('apiKey', event.target.value), [text2gql]);
    const onQuestionChange = useCallback(event => text2gql.setQuestion(event.target.value), [text2gql]);

    const missingUrl = !text2gql?.config.baseUrl.trim();
    const missingModel = !text2gql?.config.model.trim();
    const missingKey = text2gql?.config.provider !== 'ollama' && !text2gql?.config.apiKey.trim();
    const missingCustomConfig = source === 'custom' && (missingUrl || missingModel || missingKey);
    const onGenerate = useCallback(() => {
        if (missingCustomConfig) {
            setModelSettingsOpen(true);
            setConfigPrompt(true);
            return;
        }
        setConfigPrompt(false);
        setModelSettingsOpen(false);
        text2gql.generate();
    }, [missingCustomConfig, text2gql]);
    const toggleModelSettings = useCallback(event => {
        event.preventDefault();
        setModelSettingsOpen(open => !open);
    }, []);
    useEffect(() => {
        setModelSettingsOpen(false);
        setConfigPrompt(false);
    }, [activeTab]);
    const inspectModelSettings = useCallback(() => {
        setModelSettingsOpen(true);
        setConfigPrompt(true);
    }, []);
    useEffect(() => {
        if (modelSettingsOpen && configPrompt) {
            const field = modelSettingsRef.current?.querySelector('input[aria-invalid="true"]')
                || modelSettingsRef.current?.querySelector('#text2gql-base-url');
            field?.focus();
        }
    }, [configPrompt, modelSettingsOpen]);

    const renderEditor = (language, mode = language) => (
        <div className={c.editorRegion}>
            <Button
                className={c.editorToggle}
                type='text'
                size='small'
                icon={isEditorExpanded ? <UpOutlined /> : <DownOutlined />}
                onClick={toggleEditor}
                aria-expanded={isEditorExpanded}
                aria-controls={`query-editor-${mode.toLowerCase()}-${language}`}
            >
                {isEditorExpanded
                    ? t('analysis.query.collapse')
                    : t('analysis.query.expand')}
            </Button>
            {isEditorExpanded && (
                <div className={c.editorShell} id={`query-editor-${mode.toLowerCase()}-${language}`}>
                    <CodeEditor
                        value={codeEditorContent}
                        onChange={handleCodeEditorChange}
                        onExecutionShortcut={onExecution}
                        lang={language}
                        ariaLabel={t(mode === TEXT2GQL ? dialect.editorTitleKey
                            : 'analysis.query.' + language + '_editor')}
                        readOnly={mode === TEXT2GQL && text2gql?.pending}
                        minHeight={mode === TEXT2GQL ? 112 : 64}
                        placeholder={language === 'gremlin'
                            ? t('analysis.query.gremlin_placeholder')
                            : t('analysis.query.cypher_placeholder')}
                    />
                    <div className={c.editorShortcutHints} aria-hidden='true'>
                        <span>{t('analysis.query.shortcut_hint')}</span>
                    </div>
                </div>
            )}
        </div>
    );

    const commonActionProps = {
        ...args,
        isEmptyQuery,
        favoriteCardVisible,
        setFavoriteCardVisible,
    };

    const tabItems = [
        {
            label: t('analysis.query.gremlin_tab'),
            key: GREMLIN,
            children: renderEditor('gremlin'),
        },
        ...(cypherEnabled ? [{
            label: t('analysis.query.cypher_tab'),
            key: CYPHER,
            children: renderEditor('cypher'),
        }] : []),
        {
            label: t('analysis.query.text2gql_tab'),
            key: TEXT2GQL,
            children: text2gql && (
                <div className={c.text2gqlPanel} aria-busy={text2gql.pending}>
                    <div className={c.queryToolbar}>
                        <Tooltip
                            trigger={['hover', 'focus']}
                            title={nextLanguage
                                ? t('analysis.query.switch_to', {name: QUERY_LANGUAGES[nextLanguage].label})
                                : t('analysis.query.cypher_unavailable')}
                        >
                            <Button
                                id='text2gql-language'
                                aria-label={`${t('analysis.query.generation_language')}: ${dialect.label}`}
                                className={c.modeToggle}
                                icon={<SwapOutlined aria-hidden='true' />}
                                size='small'
                                disabled={!nextLanguage}
                                onClick={toggleLanguage}
                            >
                                {dialect.label}
                            </Button>
                        </Tooltip>
                        <Tooltip trigger={['hover', 'focus']} title={sourceSwitchHint}>
                            <Button
                                id='text2gql-source'
                                aria-label={`${t('analysis.query.model_source')}: ${sourceLabel(source)}`}
                                className={`${c.modeToggle} ${c.sourceToggle}`}
                                icon={<SwapOutlined aria-hidden='true' />}
                                size='small'
                                disabled={!canSwitchSource}
                                onClick={toggleSource}
                            >
                                {sourceLabel(source)}
                            </Button>
                        </Tooltip>
                        <p className={c.modelStatus} role='status'>
                            {sharedConfig.loading ? t('analysis.query.shared_loading')
                                : source === 'shared' ? [t('analysis.query.shared_ready'), sharedConfig.model]
                                    .filter(Boolean).join(' ')
                                    : !customAvailable ? t('analysis.query.custom_unavailable')
                                        : sharedConfig.available ? t('analysis.query.custom_hint')
                                            : t(sharedConfig.reason === 'authentication_required'
                                                ? 'analysis.query.shared_auth_required'
                                                : sharedConfig.reason === 'not_configured'
                                                    ? 'analysis.query.shared_not_configured'
                                                    : 'analysis.query.shared_unavailable')}
                        </p>
                        {!cypherEnabled && (
                            <span className={c.fieldHint}>{t('analysis.query.cypher_unavailable')}</span>
                        )}
                    </div>
                    {source === 'custom' && customAvailable && !sharedConfig.loading && (
                        <details className={c.modelSettings} open={modelSettingsOpen} ref={modelSettingsRef}>
                            <summary onClick={toggleModelSettings}>
                                <strong>{t('analysis.query.model_settings')}</strong>
                                {text2gql.config.model && (
                                    <span className={c.selectedModel}> · {text2gql.config.model}</span>
                                )}
                            </summary>
                            {configPrompt && missingCustomConfig && (
                                <Alert showIcon type='warning' message={t('analysis.query.complete_model_config')} />
                            )}
                            <div className={c.modelConfig}>
                                <div className={c.modelField}>
                                    <label htmlFor='text2gql-provider'>{t('analysis.query.connection_type')}</label>
                                    <Select
                                        id='text2gql-provider'
                                        aria-label={t('analysis.query.connection_type')}
                                        value={text2gql.config.provider}
                                        onChange={onProviderChange}
                                        options={Object.keys(MODEL_PROVIDERS).map(value => ({
                                            value, label: t(value === 'ollama' ? 'analysis.query.connection_ollama'
                                                : 'analysis.query.connection_openai'),
                                        }))}
                                    />
                                    {text2gql.config.provider === 'openai' && (
                                        <p className={c.fieldHint}>{t('analysis.query.connection_openai_hint')}</p>
                                    )}
                                </div>
                                <div className={c.modelField}>
                                    <label htmlFor='text2gql-base-url'>
                                        Base URL
                                    </label>
                                    <ModelHistoryInput
                                        field='baseUrl'
                                        id='text2gql-base-url'
                                        aria-required='true'
                                        aria-describedby='text2gql-url-hint'
                                        placeholder={t('analysis.query.base_url_placeholder')}
                                        status={configPrompt && missingUrl ? 'error' : undefined}
                                        aria-invalid={configPrompt && missingUrl}
                                        value={text2gql.config.baseUrl}
                                        maxLength={2048}
                                        onChange={onBaseUrlChange}
                                    />
                                    <p id='text2gql-url-hint' className={c.fieldHint}>
                                        {t('analysis.query.local_url_hint')}
                                    </p>
                                </div>
                                <div className={c.modelField}>
                                    <label htmlFor='text2gql-model'>
                                        {t('analysis.query.model_name')}
                                    </label>
                                    <ModelHistoryInput
                                        baseUrl={text2gql.config.baseUrl}
                                        id='text2gql-model'
                                        aria-label={t('analysis.query.model_name')}
                                        aria-required='true'
                                        placeholder={t('analysis.query.model_name_placeholder')}
                                        status={configPrompt && missingModel ? 'error' : undefined}
                                        aria-invalid={configPrompt && missingModel}
                                        value={text2gql.config.model}
                                        maxLength={256}
                                        onChange={onModelChange}
                                    />
                                </div>
                                <div className={c.modelField}>
                                    <label htmlFor='text2gql-api-key'>
                                        API Key {text2gql.config.provider === 'ollama' && (
                                            <span className={c.fieldHint}>{t('analysis.query.optional_field')}</span>
                                        )}
                                    </label>
                                    <Input.Password
                                        id='text2gql-api-key'
                                        aria-label='API Key'
                                        aria-required={text2gql.config.provider !== 'ollama'}
                                        aria-describedby='text2gql-key-hint'
                                        autoComplete='new-password'
                                        placeholder={text2gql.config.provider === 'ollama'
                                            ? t('analysis.query.optional_key') : t('analysis.query.required_key')}
                                        status={configPrompt && missingKey ? 'error' : undefined}
                                        aria-invalid={configPrompt && missingKey}
                                        value={text2gql.config.apiKey}
                                        maxLength={4096}
                                        onChange={onApiKeyChange}
                                    />
                                    <p id='text2gql-key-hint' className={c.fieldHint}>
                                        {t('analysis.query.api_key_hint')}
                                    </p>
                                </div>
                            </div>
                        </details>
                    )}
                    {text2gql.error && <Alert
                        showIcon
                        type={text2gql.errorType || 'error'}
                        message={text2gql.error}
                        action={source === 'custom' && customAvailable && text2gql.errorStage === 'generation'
                            && text2gql.errorType !== 'info' && (
                            <Button size='small' onClick={inspectModelSettings}>
                                {t('analysis.query.inspect_model_settings')}
                            </Button>
                        )}
                    />}
                    <div className={c.queryWorkspace}>
                        <section aria-labelledby='text2gql-question-label' className={c.questionSection}>
                            <label id='text2gql-question-label' htmlFor='text2gql-question'>
                                {t('analysis.query.question_label')}
                            </label>
                            <Input.TextArea
                                id='text2gql-question'
                                disabled={text2gql.pending}
                                aria-required='true'
                                aria-describedby='text2gql-privacy'
                                placeholder={t('analysis.query.text2gql_placeholder')}
                                value={text2gql.question}
                                onChange={onQuestionChange}
                                maxLength={4000}
                                rows={4}
                            />
                            <div className={c.questionFooter}>
                                <p id='text2gql-privacy' className={c.fieldHint}>
                                    {t(source === 'shared' ? 'analysis.query.shared_privacy'
                                        : 'analysis.query.text2gql_privacy')}
                                </p>
                                <Space wrap>
                                    <Button
                                        type='primary'
                                        loading={text2gql.pending && !executingGenerated}
                                        disabled={!languageEnabled || text2gql.pending || sharedConfig.loading
                                        || !isGraphReady || !text2gql.question.trim()
                                        || (source === 'shared' ? !sharedConfig.available : !customAvailable)}
                                        onClick={onGenerate}
                                    >
                                        {t(text2gql.pending && !executingGenerated
                                            ? text2gql.repairing
                                                ? 'analysis.query.repairing' : 'analysis.query.generating'
                                            : 'analysis.query.generate_query')}
                                    </Button>
                                    {text2gql.pending && (
                                        <Button onClick={text2gql.cancel}>
                                            {t('analysis.query.cancel_generation')}
                                        </Button>
                                    )}
                                </Space>
                            </div>
                        </section>
                        <section aria-labelledby='text2gql-query-heading' className={c.generatedSection}>
                            <h4 id='text2gql-query-heading'>{t(dialect.editorTitleKey)}</h4>
                            {renderEditor(language, TEXT2GQL)}
                            <div className={c.questionFooter}>
                                <p className={c.fieldHint}>{t('analysis.query.generated_query_hint')}</p>
                                <Space wrap>
                                    <Button
                                        disabled={!languageEnabled || !isGraphReady || isEmptyQuery || text2gql.pending}
                                        loading={executingGenerated}
                                        onClick={onExecution}
                                    >
                                        {t(executingGenerated ? 'analysis.query.querying'
                                            : 'analysis.query.run_edited_query')}
                                    </Button>
                                </Space>
                            </div>
                        </section>
                    </div>
                </div>
            ),
        },
    ];

    const queryActions = activeTab === TEXT2GQL ? undefined : {
        right: (
            <div className={c.queryActions}>
                <SecondaryActions
                    {...commonActionProps}
                    favoriteCardVisible={favoriteCardVisible}
                />
                <PrimaryActions {...commonActionProps} />
            </div>
        ),
    };

    return (
        <div className={c.queryBar} id='queryBar'>
            <Tabs
                defaultActiveKey={GREMLIN}
                activeKey={activeTab}
                type="card"
                onChange={handleTabsChange}
                items={tabItems}
                size='small'
                tabBarExtraContent={queryActions}
            />
        </div>
    );
};

export default QueryBar;
