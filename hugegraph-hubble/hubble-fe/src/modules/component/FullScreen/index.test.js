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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import FullScreen from './index';
import {GraphContext} from '../Context';
import screenfull from 'screenfull';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('screenfull', () => ({
    isEnabled: true,
    element: null,
    request: jest.fn(() => Promise.resolve()),
    exit: jest.fn(() => Promise.resolve()),
}));

const renderFullscreen = (container, onChange = jest.fn(), overrides = {}) => {
    const report = jest.fn();
    const run = operation => Promise.resolve().then(operation).catch(report);
    const view = render(
        <GraphContext.Provider value={{controller: {}, snapshot: {status: 'ready'},
            containerRef: {current: container}, run, ...overrides}}>
            <FullScreen onChange={onChange} />
        </GraphContext.Provider>
    );
    return {...view, report};
};

beforeEach(() => {
    jest.clearAllMocks();
    screenfull.element = null;
});

test('scopes F shortcut to its container, excludes editing, and removes listeners', async () => {
    const container = document.createElement('div');
    const input = document.createElement('input');
    const editable = document.createElement('div');
    editable.contentEditable = 'true';
    editable.setAttribute('contenteditable', 'true');
    container.append(input, editable);
    document.body.append(container);
    const view = renderFullscreen(container);
    expect(screen.getByRole('button', {
        name: 'analysis.canvas.toolbar.full_screen_shortcut',
    })).toBeEnabled();
    fireEvent.keyDown(document.body, {key: 'f'});
    fireEvent.keyDown(input, {key: 'f'});
    fireEvent.keyDown(editable, {key: 'f'});
    fireEvent.keyDown(container, {key: 'f', ctrlKey: true});
    fireEvent.keyDown(container, {key: 'f'});
    await waitFor(() => expect(screenfull.request).toHaveBeenCalledTimes(1));
    expect(screenfull.request).toHaveBeenCalledWith(container);
    view.unmount();
    fireEvent.keyDown(container, {key: 'f'});
    expect(screenfull.request).toHaveBeenCalledTimes(1);
    container.remove();
});

test('notifies only its own fullscreen transitions and cleans up on unmount', async () => {
    const container = document.createElement('div');
    const onChange = jest.fn();
    const view = renderFullscreen(container, onChange);
    screenfull.element = document.createElement('div');
    fireEvent(document, new Event('fullscreenchange'));
    expect(onChange).not.toHaveBeenCalled();
    screenfull.element = container;
    fireEvent(document, new Event('fullscreenchange'));
    fireEvent.click(screen.getByRole('button'));
    await waitFor(() => expect(screenfull.exit).toHaveBeenCalledTimes(1));
    screenfull.element = null;
    fireEvent(document, new Event('fullscreenchange'));
    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(2));
    view.unmount();
    screenfull.element = container;
    fireEvent(document, new Event('fullscreenchange'));
    expect(onChange).toHaveBeenCalledTimes(2);
});

test('handles browser rejection through the shared runner and disables before ready', async () => {
    screenfull.request.mockRejectedValueOnce(new Error('request denied'));
    const view = renderFullscreen(document.createElement('div'));
    fireEvent.click(screen.getByRole('button'));
    await waitFor(() => expect(view.report).toHaveBeenCalledWith(expect.any(Error)));
    view.unmount();
    renderFullscreen(document.createElement('div'), jest.fn(), {snapshot: {status: 'updating'}});
    expect(screen.getByRole('button')).toBeDisabled();
});
