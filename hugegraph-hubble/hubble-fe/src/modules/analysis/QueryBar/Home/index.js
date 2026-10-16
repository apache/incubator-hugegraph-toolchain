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

import React, {useCallback, useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Alert, Button, Input, Select, Space, Tabs} from 'antd';
import {DownOutlined, UpOutlined} from '@ant-design/icons';
import {MODEL_PROVIDERS} from '../../../../utils/text2gql';
import CodeEditor from '../../../../components/CodeEditor';
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

    const onProviderChange = useCallback(value => text2gql.updateConfig('provider', value), [text2gql]);
    const onBaseUrlChange = useCallback(event => text2gql.updateConfig('baseUrl', event.target.value), [text2gql]);
    const onModelChange = useCallback(event => text2gql.updateConfig('model', event.target.value), [text2gql]);
    const onApiKeyChange = useCallback(event => text2gql.updateConfig('apiKey', event.target.value), [text2gql]);
    const onQuestionChange = useCallback(event => text2gql.setQuestion(event.target.value), [text2gql]);

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
                        ariaLabel={t(mode === TEXT2GQL ? 'analysis.query.generated_gremlin'
                            : 'analysis.query.' + language + '_editor')}
                        readOnly={mode === TEXT2GQL && text2gql?.pending}
                        minHeight={64}
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
                    <header className={c.text2gqlHeader}>
                        <p>{t('analysis.query.text2gql_description')}</p>
                    </header>
                    <details className={c.modelSettings} open>
                        <summary>
                            <strong>{t('analysis.query.model_settings')}</strong>
                            {text2gql.config.model && (
                                <span className={c.selectedModel}> · {text2gql.config.model}</span>
                            )}
                            <span className={c.requiredLegend}>{t('analysis.query.required_fields')}</span>
                        </summary>
                        <div className={c.modelConfig}>
                            <div className={c.modelField}>
                                <label htmlFor='text2gql-provider'>{t('analysis.query.model_provider')}</label>
                                <Select
                                    id='text2gql-provider'
                                    aria-label={t('analysis.query.model_provider')}
                                    value={text2gql.config.provider}
                                    onChange={onProviderChange}
                                    options={Object.keys(MODEL_PROVIDERS).map(value => ({
                                        value, label: {ollama: 'Ollama', deepseek: 'DeepSeek',
                                            glm: 'GLM', openai: 'OpenAI'}[value],
                                    }))}
                                />
                            </div>
                            <div className={c.modelField}>
                                <label htmlFor='text2gql-base-url'>
                                    Base URL <span className={c.requiredField} aria-hidden='true'>*</span>
                                </label>
                                <Input
                                    id='text2gql-base-url'
                                    aria-required='true'
                                    aria-describedby='text2gql-url-hint'
                                    placeholder='https://…/v1'
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
                                    <span className={c.requiredField} aria-hidden='true'> *</span>
                                </label>
                                <Input
                                    id='text2gql-model'
                                    aria-label={t('analysis.query.model_name')}
                                    aria-required='true'
                                    placeholder={t('analysis.query.model_name_placeholder')}
                                    value={text2gql.config.model}
                                    maxLength={256}
                                    onChange={onModelChange}
                                />
                            </div>
                            <div className={c.modelField}>
                                <label htmlFor='text2gql-api-key'>
                                    API Key {text2gql.config.provider === 'ollama'
                                        ? <span className={c.fieldHint}>{t('analysis.query.optional_field')}</span>
                                        : <span className={c.requiredField} aria-hidden='true'>*</span>}
                                </label>
                                <Input.Password
                                    id='text2gql-api-key'
                                    aria-label='API Key'
                                    aria-required={text2gql.config.provider !== 'ollama'}
                                    aria-describedby='text2gql-key-hint'
                                    autoComplete='new-password'
                                    placeholder={text2gql.config.provider === 'ollama'
                                        ? t('analysis.query.optional_key') : t('analysis.query.required_key')}
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
                    <section aria-labelledby='text2gql-question-label' className={c.questionSection}>
                        <label id='text2gql-question-label' htmlFor='text2gql-question'>
                            {t('analysis.query.question_label')}
                            <span className={c.requiredField} aria-hidden='true'> *</span>
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
                            rows={2}
                        />
                        <div className={c.questionFooter}>
                            <p id='text2gql-privacy' className={c.fieldHint}>
                                {t('analysis.query.text2gql_privacy')}
                            </p>
                            <Space wrap>
                                <Button
                                    type='primary'
                                    loading={text2gql.pending && !isExecuting}
                                    disabled={text2gql.pending || !isGraphReady || !text2gql.question.trim()
                                    || !text2gql.config.baseUrl.trim() || !text2gql.config.model.trim()
                                    || (text2gql.config.provider !== 'ollama' && !text2gql.config.apiKey.trim())}
                                    onClick={text2gql.generate}
                                >
                                    {t(text2gql.pending && !isExecuting ? 'analysis.query.generating'
                                        : 'analysis.query.generate_query')}
                                </Button>
                                {text2gql.pending && !isExecuting && (
                                    <Button onClick={text2gql.cancel}>{t('analysis.query.cancel_generation')}</Button>
                                )}
                            </Space>
                        </div>
                    </section>
                    {text2gql.error && <Alert
                        showIcon
                        type={text2gql.errorType || 'error'}
                        message={text2gql.error}
                    />}
                    <section aria-labelledby='text2gql-gremlin-heading' className={c.generatedSection}>
                        <div className={c.sectionHeading}>
                            <div>
                                <h4 id='text2gql-gremlin-heading'>{t('analysis.query.generated_gremlin')}</h4>
                                <p className={c.fieldHint}>{t('analysis.query.generated_gremlin_hint')}</p>
                            </div>
                            <Space wrap>
                                <Button
                                    disabled={!isGraphReady || isEmptyQuery || text2gql.pending}
                                    loading={text2gql.pending && isExecuting}
                                    onClick={onExecution}
                                >
                                    {t(text2gql.pending && isExecuting ? 'analysis.query.querying'
                                        : 'analysis.query.run_edited_gremlin')}
                                </Button>
                                {text2gql.pending && isExecuting && (
                                    <Button onClick={text2gql.cancel}>
                                        {t('analysis.query.cancel_generation')}
                                    </Button>
                                )}
                            </Space>
                        </div>
                        {renderEditor('gremlin', TEXT2GQL)}
                    </section>
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
