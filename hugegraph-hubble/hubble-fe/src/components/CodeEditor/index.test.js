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

import {fireEvent, render} from '@testing-library/react';
import CodeEditor from './index';

jest.mock('@codemirror/autocomplete', () => ({
    autocompletion: () => ({}),
    closeBrackets: () => ({}),
}));
jest.mock('@codemirror/language', () => ({
    HighlightStyle: {define: () => ({})},
    syntaxHighlighting: () => ({}),
}));
jest.mock('@codemirror/view', () => ({
    placeholder: () => ({}),
}));
jest.mock('@lezer/highlight', () => ({tags: {}}));
jest.mock('./syntax', () => ({default: {hint: [], language: {}}}));
jest.mock('codemirror', () => {
    class MockEditorView {
        static domEventHandlers(handlers) {
            return {handlers};
        }

        static editable = {of: () => ({})};
        static contentAttributes = {of: () => ({})};
        static updateListener = {of: listener => ({updateListener: listener})};
        static theme() {
            return {};
        }

        constructor({doc, extensions, parent}) {
            this.state = {
                doc: {
                    length: doc.length,
                    toString: () => doc,
                },
            };
            this.dom = global.document.createElement('div');
            this.dom.className = 'cm-editor';
            this.content = global.document.createElement('div');
            this.content.className = 'cm-content';
            this.content.textContent = doc;
            this.updateListeners = extensions.flat().filter(extension => extension.updateListener)
                .map(extension => extension.updateListener);
            this.content.addEventListener('focus', () => this.updateListeners.forEach(listener => {
                listener({docChanged: false, state: this.state});
            }));
            this.content.tabIndex = 0;
            this.dom.appendChild(this.content);
            parent.appendChild(this.dom);
            extensions.flat().filter(extension => extension.handlers)
                .forEach(extension => {
                    this.content.addEventListener('keydown', event => {
                        extension.handlers.keydown(event, this);
                    });
                });
        }

        dispatch({changes}) {
            const doc = changes.insert;
            this.state.doc = {
                length: doc.length,
                toString: () => doc,
            };
            this.content.textContent = doc;
            this.updateListeners.forEach(listener => listener({docChanged: true, state: this.state}));
        }

        destroy() {
            this.dom.remove();
        }
    }

    return {basicSetup: {}, EditorView: MockEditorView};
});
jest.mock('react-i18next', () => {
    const translate = key => key;
    return {useTranslation: () => ({t: translate})};
});

it('keeps focus when controlled callbacks change after input', () => {
    const firstExecution = jest.fn();
    const nextExecution = jest.fn();
    const firstChange = jest.fn();
    const nextChange = jest.fn();
    const {container, rerender} = render(
        <CodeEditor
            value=''
            onChange={firstChange}
            onExecutionShortcut={firstExecution}
        />
    );
    const originalEditor = container.querySelector('.cm-editor');
    const content = container.querySelector('.cm-content');
    content.focus();

    rerender(
        <CodeEditor
            value='x'
            onChange={nextChange}
            onExecutionShortcut={nextExecution}
        />
    );

    expect(container.querySelector('.cm-editor')).toBe(originalEditor);
    expect(document.activeElement).toBe(content);

    fireEvent.keyDown(content, {key: 'Enter', ctrlKey: true});
    expect(firstExecution).not.toHaveBeenCalled();
    expect(nextExecution).toHaveBeenCalledTimes(1);
});


it('preserves the generated controlled value through pending read-only transitions', () => {
    const onChange = jest.fn();
    const {container, rerender} = render(<CodeEditor value='' onChange={onChange} />);
    const gremlin = 'g.V().hasLabel("person").limit(3)';
    rerender(<CodeEditor value={gremlin} readOnly onChange={onChange} />);
    expect(container.querySelector('.cm-content')).toHaveTextContent(gremlin);
    rerender(<CodeEditor value={gremlin} readOnly={false} onChange={onChange} />);
    expect(container.querySelector('.cm-content')).toHaveTextContent(gremlin);
    fireEvent.focus(container.querySelector('.cm-content'));
    expect(onChange).not.toHaveBeenCalled();
});

it('does not turn focus and layout updates into document changes', () => {
    const onChange = jest.fn();
    const {container} = render(<CodeEditor value='g.V()' onChange={onChange} />);
    fireEvent.focus(container.querySelector('.cm-content'));
    expect(onChange).not.toHaveBeenCalled();
});
