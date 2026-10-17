/*
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
import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QueryBar from './index';

jest.mock('../../../../components/CodeEditor', () => props => (
    <div
        className='cm-editor'
        data-testid={`editor-${props.lang}`}
        data-placeholder={props.placeholder}
        data-read-only={String(Boolean(props.readOnly))}
        data-aria-label={props.ariaLabel}
        data-value={props.value}
        onInput={event => props.onChange(event.target.textContent)}
        data-meta-enter-newline={props.metaEnterNewline ? 'true' : 'false'}
        data-execution-shortcut={props.onExecutionShortcut ? 'true' : 'false'}
        onKeyDown={event => {
            if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
                props.onExecutionShortcut?.();
            }
        }}
    >
        editor
    </div>
));
jest.mock('../ContentCommon', () => ({
    SecondaryActions: ({
        favoriteCardVisible,
        setFavoriteCardVisible,
        isEmptyQuery,
        shortcutHint,
    }) => (
        <div data-testid='secondary-query-actions'>
            <button>Clear</button>
            <button onClick={() => setFavoriteCardVisible(true)}>Favorite</button>
            <span>{favoriteCardVisible ? 'favorite open' : 'favorite closed'}</span>
            <span>{isEmptyQuery ? 'query empty' : 'query ready'}</span>
            <span>{shortcutHint}</span>
        </div>
    ),
    PrimaryActions: ({onExecute, activeTab}) => (
        <button onClick={() => onExecute(activeTab)}>Run Query</button>
    ),
}));
jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => ({
        'analysis.query.gremlin_tab': 'Gremlin',
        'analysis.query.connection_type': 'Connection type',
        'analysis.query.connection_openai': 'OpenAI compatible',
        'analysis.query.connection_ollama': 'Ollama (local)',
        'analysis.query.gremlin_placeholder': 'For example: g.V().limit(10)',
        'analysis.query.cypher_tab': 'Cypher',
        'analysis.query.cypher_placeholder': 'For example: MATCH (n) RETURN n LIMIT 10',
        'analysis.query.shortcut_hint': 'Ctrl / Command + Enter to run',
        'analysis.query.text2gql_tab': 'Natural language',
        'analysis.query.text2gql_title': 'Natural-language graph query',
        'analysis.query.text2gql_description': 'This preview is not connected.',
        'analysis.query.text2gql_placeholder': 'e.g. Show 10 vertices and the relationships between them',
        'analysis.query.text2gql_badge': 'Coming soon',
        'analysis.query.text2gql_privacy': 'Nothing is sent or executed.',
        'analysis.query.collapse': 'Collapse',
        'analysis.query.expand': 'Expand',
    })[key] || key}),
}));

beforeAll(() => {
    window.matchMedia = window.matchMedia || (() => ({
        matches: false,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

it('ignores execution shortcuts until the graph context is ready', () => {
    const onExecute = jest.fn();
    const props = {
        activeTab: 'Gremlin',
        codeEditorContent: 'g.V().limit(10)',
        onExecute,
        onTabsChange: jest.fn(),
        setCodeEditorContent: jest.fn(),
    };
    const {rerender} = render(<QueryBar {...props} isGraphReady={false} />);
    fireEvent.keyDown(screen.getByTestId('editor-gremlin'), {key: 'Enter', ctrlKey: true});
    expect(onExecute).not.toHaveBeenCalled();

    rerender(<QueryBar {...props} isGraphReady />);
    fireEvent.keyDown(screen.getByTestId('editor-gremlin'), {key: 'Enter', ctrlKey: true});
    expect(onExecute).toHaveBeenCalledWith('Gremlin');
});

it('enables Text2GQL input and generates with the current model settings', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: 'Find people', updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), cancel: jest.fn(), pending: false,
    };
    render(<QueryBar
        activeTab='Text2GQL'
        codeEditorContent=''
        text2gql={text2gql}
        onTabsChange={jest.fn()}
        setCodeEditorContent={jest.fn()}
    />);
    expect(screen.getByRole('textbox', {name: 'analysis.query.question_label'})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: 'analysis.query.generate_query'}));
    expect(text2gql.generate).toHaveBeenCalledTimes(1);
});

it('hides Cypher when capability is absent', () => {
    render(
        <QueryBar
            activeTab='Gremlin'
            onTabsChange={jest.fn()}
            codeEditorContent=''
            setCodeEditorContent={jest.fn()}
        />
    );

    expect(screen.queryByRole('tab', {name: 'Cypher'})).not.toBeInTheDocument();
});

it('does not transfer an open favorite popover when query tabs change', () => {
    const ControlledQueryBar = () => {
        const [activeTab, setActiveTab] = useState('Gremlin');
        return (
            <QueryBar
                activeTab={activeTab}
                cypherEnabled
                onTabsChange={setActiveTab}
                codeEditorContent='g.V()'
                setCodeEditorContent={jest.fn()}
            />
        );
    };

    render(<ControlledQueryBar />);
    expect(screen.getByText('query ready')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Favorite'}));
    expect(screen.getByText('favorite open')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', {name: 'Cypher'}));
    expect(screen.getByText('favorite closed')).toBeInTheDocument();
});

it('matches the editor placeholder to the active query language without promotional help', () => {
    const ControlledQueryBar = () => {
        const [activeTab, setActiveTab] = useState('Gremlin');
        return (
            <QueryBar
                activeTab={activeTab}
                cypherEnabled
                onTabsChange={setActiveTab}
                codeEditorContent=''
                setCodeEditorContent={jest.fn()}
            />
        );
    };

    render(<ControlledQueryBar />);
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute(
        'data-placeholder',
        'For example: g.V().limit(10)'
    );
    expect(screen.queryByText(/safe example/i)).not.toBeInTheDocument();
    expect(screen.getByText('Ctrl / Command + Enter to run')).toBeInTheDocument();
    expect(screen.getByText('Ctrl / Command + Enter to run').parentElement)
        .toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute(
        'data-meta-enter-newline', 'false'
    );
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute(
        'data-execution-shortcut', 'true'
    );

    fireEvent.click(screen.getByRole('tab', {name: 'Cypher'}));
    expect(screen.getByTestId('editor-cypher')).toHaveAttribute(
        'data-placeholder',
        'For example: MATCH (n) RETURN n LIMIT 10'
    );
    expect(screen.queryByText(/safe example/i)).not.toBeInTheDocument();
});

it('shows one combined Ctrl and Command shortcut hint on every platform', () => {
    render(
        <QueryBar
            activeTab='Gremlin'
            onTabsChange={jest.fn()}
            onExecute={jest.fn()}
            codeEditorContent='g.V()'
            setCodeEditorContent={jest.fn()}
        />
    );

    expect(screen.getByText('Ctrl / Command + Enter to run')).toBeInTheDocument();
});

it('keeps query actions, language tabs, and Text2GQL in one navigation row', () => {
    render(
        <QueryBar
            activeTab='Gremlin'
            onTabsChange={jest.fn()}
            onExecute={jest.fn()}
            codeEditorContent='g.V()'
            setCodeEditorContent={jest.fn()}
        />
    );

    const navigation = screen.getByRole('tablist').closest('.ant-tabs-nav');
    expect(navigation).toContainElement(screen.getByTestId('secondary-query-actions'));
    expect(navigation).toContainElement(screen.getByRole('tab', {
        name: /Natural language/,
    }));
    expect(navigation).toContainElement(screen.getByRole('button', {name: 'Run Query'}));

    const text2gqlTab = screen.getByRole('tab', {name: /Natural language/});
    const clearButton = screen.getByRole('button', {name: 'Clear'});
    const favoriteButton = screen.getByRole('button', {name: 'Favorite'});
    expect(text2gqlTab.compareDocumentPosition(clearButton))
        .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(clearButton.compareDocumentPosition(favoriteButton))
        .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
});

it('keeps collapse as a keyboard-accessible editor control', () => {
    render(
        <QueryBar
            activeTab='Gremlin'
            onTabsChange={jest.fn()}
            onExecute={jest.fn()}
            codeEditorContent='g.V()'
            setCodeEditorContent={jest.fn()}
        />
    );

    const collapse = screen.getByRole('button', {name: /Collapse/});
    expect(collapse).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(collapse);
    expect(screen.queryByTestId('editor-gremlin')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: /Expand/})).toHaveAttribute(
        'aria-expanded', 'false'
    );
});

it('executes with Ctrl+Enter and Command+Enter inside the editor', () => {
    const onExecute = jest.fn();
    render(
        <QueryBar
            activeTab='Gremlin'
            onTabsChange={jest.fn()}
            onExecute={onExecute}
            codeEditorContent='g.V()'
            setCodeEditorContent={jest.fn()}
        />
    );

    const editor = screen.getByTestId('editor-gremlin');
    fireEvent.keyDown(editor, {key: 'Enter', metaKey: true});
    expect(onExecute).toHaveBeenCalledWith('Gremlin');
    fireEvent.keyDown(editor, {key: 'Enter', ctrlKey: true});
    expect(onExecute).toHaveBeenCalledTimes(2);
});


it('keeps generated Gremlin read-only while generation or execution is pending', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: 'Find people', pending: true, updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), cancel: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent='g.V()'
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute('data-read-only', 'true');
});


it('keeps the Text2GQL editor available when a user clears the Gremlin to rewrite it', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: '', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
    };
    const ControlledQueryBar = () => {
        const [content, setContent] = useState('g.V()');
        return (
            <QueryBar
                activeTab='Text2GQL'
                codeEditorContent={content}
                text2gql={text2gql}
                onTabsChange={jest.fn()}
                setCodeEditorContent={setContent}
            />
        );
    };
    render(<ControlledQueryBar />);
    fireEvent.input(screen.getByTestId('editor-gremlin'), {target: {textContent: ''}});
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute('data-value', '');
    fireEvent.input(screen.getByTestId('editor-gremlin'), {target: {textContent: 'g.V().count()'}});
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute('data-value', 'g.V().count()');
});


it('keeps model field labels visible and ties Gremlin actions to the editor section', () => {
    const text2gql = {
        config: {provider: 'openai', baseUrl: 'https://api.openai.com/v1', model: 'test', apiKey: 'private'},
        question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent='g.V()'
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    expect(screen.getByLabelText(/Base URL/)).toHaveAttribute('aria-required', 'true');
    expect(screen.getByLabelText('API Key')).toHaveAttribute('aria-required', 'true');
    const generated = screen.getByRole('region', {name: 'analysis.query.generated_gremlin'});
    expect(generated).toContainElement(screen.getByRole('button', {name: 'analysis.query.run_edited_query'}));
    expect(screen.getByTestId('editor-gremlin')).toHaveAttribute(
        'data-aria-label', 'analysis.query.generated_gremlin'
    );
});

it.each([false, true])('distinguishes model generation and graph execution loading (%s)', isExecuting => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: 'Find people', pending: true, updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), cancel: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent='g.V()'
            isExecuting={isExecuting}
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    const active = screen.getByRole('button', {
        name: new RegExp(isExecuting ? 'analysis.query.querying' : 'analysis.query.generating'),
    });
    expect(active).toHaveClass('ant-btn-loading');
    expect(screen.getByRole('textbox', {name: 'analysis.query.question_label'})).toBeDisabled();
    expect(screen.getAllByRole('button', {name: 'analysis.query.cancel_generation'})).toHaveLength(1);
});

it('shows cancellation as neutral information and uses distinct IDs for both Gremlin editors', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: '', pending: false, error: 'Cancelled', errorType: 'info',
        updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
    };
    const ControlledQueryBar = () => {
        const [activeTab, setActiveTab] = useState('Gremlin');
        return (
            <QueryBar
                activeTab={activeTab}
                codeEditorContent='g.V()'
                text2gql={text2gql}
                onTabsChange={setActiveTab}
                setCodeEditorContent={jest.fn()}
            />
        );
    };
    const {container} = render(<ControlledQueryBar />);
    fireEvent.click(screen.getByRole('tab', {name: /Natural language/}));
    expect(screen.getByRole('alert')).toHaveClass('ant-alert-info');
    const editorIds = [...container.querySelectorAll('[id^="query-editor-"]')].map(node => node.id);
    expect(editorIds).toHaveLength(2);
    expect(new Set(editorIds).size).toBe(2);
});


it('offers only OpenAI-compatible and local Ollama connection types', () => {
    const text2gql = {
        config: {provider: 'openai', baseUrl: 'https://api.deepseek.com/v1', model: 'test', apiKey: ''},
        question: '', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    fireEvent.click(screen.getByText('analysis.query.model_settings'));
    expect(screen.getByText('Connection type')).toBeInTheDocument();
    expect(screen.getByText('analysis.query.connection_openai_hint')).toBeInTheDocument();
    fireEvent.mouseDown(document.getElementById('text2gql-provider'));
    expect(screen.getAllByRole('option')).toHaveLength(2);
    expect(screen.getByRole('option', {name: 'OpenAI compatible'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Ollama (local)'})).toBeInTheDocument();
    expect(screen.getByLabelText(/Base URL/)).toHaveValue('https://api.deepseek.com/v1');
});


it('shows only safe shared model information and allows generation without personal credentials', () => {
    const text2gql = {
        config: {provider: 'openai', baseUrl: '', model: '', apiKey: ''},
        source: 'shared', sharedConfig: {available: true, loading: false, model: 'deployment-model'},
        question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), changeSource: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    expect(screen.queryByLabelText(/Base URL/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText('API Key')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('deployment-model');
    expect(screen.getByRole('button', {name: 'analysis.query.generate_query'})).toBeEnabled();
});


it('hides custom credentials and disables generation when neither model source is available', () => {
    const text2gql = {
        config: {provider: 'openai', baseUrl: 'https://private.example/v1', model: 'test', apiKey: 'private'},
        source: 'custom', sharedConfig: {available: false, customAvailable: false, loading: false},
        question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), changeSource: jest.fn(),
    };
    render(
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    expect(screen.queryByLabelText(/Base URL/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText('API Key')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('analysis.query.custom_unavailable');
    expect(screen.getByRole('button', {name: /analysis.query.model_source:/})).toBeDisabled();
    const generate = screen.getByRole('button', {name: 'analysis.query.generate_query'});
    expect(generate).toBeDisabled();
    fireEvent.click(generate);
    expect(text2gql.generate).not.toHaveBeenCalled();
});


it('defaults generation to Gremlin and offers a working Cypher editor through the same panel', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(),
        generate: jest.fn(), changeLanguage: jest.fn(),
    };
    const panel = language => (
        <QueryBar
            activeTab='Text2GQL'
            cypherEnabled
            codeEditorContent='MATCH (n) RETURN n LIMIT 3'
            text2gql={{...text2gql, language}}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    const {rerender} = render(panel(undefined));
    const languageButton = screen.getByRole('button', {name: 'analysis.query.generation_language: Gremlin'});
    userEvent.click(languageButton);
    expect(text2gql.changeLanguage).toHaveBeenCalledWith('cypher');
    rerender(panel('cypher'));
    expect(languageButton).toHaveAccessibleName('analysis.query.generation_language: Cypher');
    expect(screen.getByRole('region', {name: 'analysis.query.generated_cypher'}))
        .toContainElement(screen.getByTestId('editor-cypher'));
    expect(screen.getByTestId('editor-cypher')).toHaveAttribute('data-aria-label', 'analysis.query.generated_cypher');
    fireEvent.click(screen.getByRole('button', {name: 'analysis.query.generate_query'}));
    expect(text2gql.generate).toHaveBeenCalledTimes(1);
    userEvent.click(languageButton);
    expect(text2gql.changeLanguage).toHaveBeenLastCalledWith('gremlin');
});


it('disables Cypher natural-language generation with a clear server capability explanation', () => {
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: 'Find people', pending: false, changeLanguage: jest.fn(), updateConfig: jest.fn(),
        setQuestion: jest.fn(), generate: jest.fn(),
    };
    const panel = cypherEnabled => (
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent='g.V()'
            cypherEnabled={cypherEnabled}
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    const {rerender} = render(panel(false));
    const languageButton = screen.getByRole('button', {name: 'analysis.query.generation_language: Gremlin'});
    expect(languageButton).toBeDisabled();
    expect(screen.getByText('analysis.query.cypher_unavailable')).toBeInTheDocument();
    fireEvent.click(languageButton);
    expect(text2gql.changeLanguage).not.toHaveBeenCalled();
    rerender(panel(true));
    const enabledLanguage = screen.getByRole('button', {name: 'analysis.query.generation_language: Gremlin'});
    expect(enabledLanguage).toBeEnabled();
    expect(screen.queryByText('analysis.query.cypher_unavailable')).not.toBeInTheDocument();
    userEvent.click(enabledLanguage);
    expect(text2gql.changeLanguage).toHaveBeenCalledWith('cypher');
});


it('keeps model settings collapsed until missing configuration needs attention, without sending a request', () => {
    const text2gql = {
        config: {provider: 'openai', baseUrl: 'https://api.openai.com/v1', model: '', apiKey: ''},
        question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
    };
    const panel = config => (
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={{...text2gql, config}}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    const {rerender} = render(panel(text2gql.config));
    const settings = screen.getByText('analysis.query.model_settings').closest('details');
    expect(settings).not.toHaveAttribute('open');
    const generate = screen.getByRole('button', {name: 'analysis.query.generate_query'});
    expect(generate).toBeEnabled();
    fireEvent.click(generate);
    expect(settings).toHaveAttribute('open');
    expect(screen.getByRole('alert')).toHaveTextContent('analysis.query.complete_model_config');
    expect(screen.getByLabelText('analysis.query.model_name')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('analysis.query.model_name')).toHaveFocus();
    expect(text2gql.generate).not.toHaveBeenCalled();
    rerender(panel({...text2gql.config, model: 'test-model', apiKey: 'page-secret'}));
    fireEvent.click(generate);
    expect(text2gql.generate).toHaveBeenCalledTimes(1);
    expect(settings).not.toHaveAttribute('open');
});

it.each(['generation', 'execution', 'cancelled'])(
    'keeps failures collapsed and offers model inspection only for generation errors (%s)', errorStage => {
        const text2gql = {
            config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
            question: 'Find people', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(),
            generate: jest.fn(),
        };
        const panel = error => (
            <QueryBar
                activeTab='Text2GQL'
                codeEditorContent=''
                text2gql={{...text2gql, error, errorStage, errorType: errorStage === 'cancelled' ? 'info' : 'error'}}
                onTabsChange={jest.fn()}
                setCodeEditorContent={jest.fn()}
            />
        );
        const {rerender} = render(panel(''));
        const settings = screen.getByText('analysis.query.model_settings').closest('details');
        expect(settings).not.toHaveAttribute('open');
        rerender(panel(errorStage === 'cancelled' ? 'Cancelled' : 'Request failed'));
        expect(settings).not.toHaveAttribute('open');
        expect(screen.getByLabelText(/Base URL/)).not.toHaveFocus();
        const inspect = screen.queryByRole('button', {name: 'analysis.query.inspect_model_settings'});
        if (errorStage === 'generation') {
            expect(inspect).toBeInTheDocument();
            fireEvent.click(inspect);
            expect(settings).toHaveAttribute('open');
            expect(screen.getByLabelText(/Base URL/)).toHaveFocus();
        }
        else {
            expect(inspect).not.toBeInTheDocument();
        }
    }
);

it.each([['Enter', '{Enter}'], ['Space', ' ']])(
    'switches language and model source in both directions with native %s activation', (_, key) => {
        const changeLanguage = jest.fn();
        const changeSource = jest.fn();
        const ControlledQueryBar = () => {
            const [language, setLanguage] = useState('gremlin');
            const [source, setSource] = useState('shared');
            const text2gql = {
                config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
                question: '', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
                sharedConfig: {available: true, customAvailable: true, loading: false},
                language, source,
                changeLanguage: value => {
                    changeLanguage(value);
                    setLanguage(value);
                },
                changeSource: value => {
                    changeSource(value);
                    setSource(value);
                },
            };
            return (
                <QueryBar
                    activeTab='Text2GQL'
                    cypherEnabled
                    codeEditorContent=''
                    text2gql={text2gql}
                    onTabsChange={jest.fn()}
                    setCodeEditorContent={jest.fn()}
                />
            );
        };
        render(<ControlledQueryBar />);
        const language = screen.getByRole('button', {name: 'analysis.query.generation_language: Gremlin'});
        const source = screen.getByRole('button', {name: 'analysis.query.model_source: analysis.query.source_shared'});
        expect(language.closest('.queryToolbar')).toContainElement(source);
        act(() => language.focus());
        userEvent.keyboard(key);
        expect(changeLanguage).toHaveBeenLastCalledWith('cypher');
        expect(language).toHaveAccessibleName('analysis.query.generation_language: Cypher');
        userEvent.keyboard(key);
        expect(changeLanguage.mock.calls).toEqual([['cypher'], ['gremlin']]);
        act(() => source.focus());
        userEvent.keyboard(key);
        expect(changeSource).toHaveBeenLastCalledWith('custom');
        expect(source).toHaveAccessibleName('analysis.query.model_source: analysis.query.source_custom');
        userEvent.keyboard(key);
        expect(changeSource.mock.calls).toEqual([['custom'], ['shared']]);
    }
);

it('switches model sources directly with one click and retains the current source label', () => {
    const changeSource = jest.fn();
    const text2gql = {
        config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
        question: '', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
        sharedConfig: {available: true, customAvailable: true, loading: false}, changeSource,
    };
    const panel = source => (
        <QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={{...text2gql, source}}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />
    );
    const {rerender} = render(panel('custom'));
    const source = screen.getByRole('button', {name: 'analysis.query.model_source: analysis.query.source_custom'});
    userEvent.click(source);
    expect(changeSource).toHaveBeenLastCalledWith('shared');
    rerender(panel('shared'));
    expect(source).toHaveAccessibleName('analysis.query.model_source: analysis.query.source_shared');
    userEvent.click(source);
    expect(changeSource.mock.calls).toEqual([['shared'], ['custom']]);
});

it.each([
    ['custom', {available: false, customAvailable: true, loading: false, reason: 'not_configured'},
        'analysis.query.shared_not_configured'],
    ['shared', {available: true, customAvailable: false, loading: false}, 'analysis.query.custom_unavailable'],
    ['custom', {available: true, customAvailable: true, loading: true}, 'analysis.query.shared_loading'],
])('keeps the current source when the alternative is unavailable or loading (%s, %j)',
    async (source, sharedConfig, hint) => {
        const changeSource = jest.fn();
        const text2gql = {
            config: {provider: 'ollama', baseUrl: 'http://localhost:11434/v1', model: 'local', apiKey: ''},
            question: '', pending: false, updateConfig: jest.fn(), setQuestion: jest.fn(), generate: jest.fn(),
            source, sharedConfig, changeSource,
        };
        render(<QueryBar
            activeTab='Text2GQL'
            codeEditorContent=''
            text2gql={text2gql}
            onTabsChange={jest.fn()}
            setCodeEditorContent={jest.fn()}
        />);
        const button = screen.getByRole('button', {name: new RegExp('analysis.query.model_source:')});
        expect(button).toBeDisabled();
        fireEvent.click(button);
        expect(changeSource).not.toHaveBeenCalled();
        userEvent.hover(button.parentElement);
        expect(await screen.findByRole('tooltip')).toHaveTextContent(hint);
    }
);
