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

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import Text2GqlSettings from './index';
import * as api from '../../api';

let mockIdentityEpoch = 0;
jest.mock('../../api', () => ({config: {
    getText2GqlSettings: jest.fn(), updateText2GqlSettings: jest.fn(),
}}));
jest.mock('../../auth/AuthContext', () => ({useAuthContext: () => ({identityEpoch: mockIdentityEpoch})}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
const initial = {enabled: true, connection_type: 'openai', base_url: 'https://model.example/v1',
    model: 'shared-model', has_api_key: true, source: 'managed'};
const response = values => ({status: 200, data: {...initial, ...values}});
const deferred = () => {
    let resolve;
    const promise = new Promise(done => {
        resolve = done;
    });
    return {promise, resolve};
};
const keyInput = () => screen.getByLabelText('analysis.model_settings.replace_key');
const saveButton = () => screen.getByRole('button', {name: 'analysis.model_settings.save'});

beforeAll(() => {
    window.matchMedia = window.matchMedia || (() => ({matches: false,
        addListener: jest.fn(), removeListener: jest.fn()}));
});
beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    mockIdentityEpoch = 0;
    api.config.getText2GqlSettings.mockResolvedValue(response());
    api.config.updateText2GqlSettings.mockResolvedValue(response());
});
const load = async () => {
    const view = render(<Text2GqlSettings />);
    await screen.findByLabelText('analysis.model_settings.replace_key');
    return view;
};

test('replaces a key without persisting or reading it back and clears the input after saving', async () => {
    await load();
    expect(keyInput()).toHaveValue('');
    fireEvent.change(keyInput(), {target: {value: 'replacement-secret'}});
    await act(async () => fireEvent.click(saveButton()));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('analysis.model_settings.saved'));
    expect(api.config.updateText2GqlSettings).toHaveBeenCalledWith({
        enabled: true, connection_type: 'openai', base_url: initial.base_url,
        model: 'shared-model', api_key: 'replacement-secret',
    }, expect.any(Object));
    expect(keyInput()).toHaveValue('');
    expect(JSON.stringify({...sessionStorage, ...localStorage})).not.toContain('replacement-secret');
});

test('keeps the saved key when only the model changes and the new key is blank', async () => {
    await load();
    fireEvent.change(screen.getByLabelText('analysis.query.model_name'), {target: {value: 'new-model'}});
    await act(async () => fireEvent.click(saveButton()));
    await waitFor(() => expect(api.config.updateText2GqlSettings).toHaveBeenCalledTimes(1));
    const params = api.config.updateText2GqlSettings.mock.calls[0][0];
    expect(params).toMatchObject({model: 'new-model', base_url: initial.base_url});
    expect(params).not.toHaveProperty('api_key');
    expect(params).not.toHaveProperty('clear_api_key');
});

test('requires a replacement key after changing the OpenAI-compatible endpoint', async () => {
    await load();
    fireEvent.change(screen.getByLabelText('Base URL'), {target: {value: 'https://different.example/v1'}});
    expect(saveButton()).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('analysis.model_settings.key_endpoint_changed');
    await act(async () => fireEvent.click(saveButton()));
    expect(api.config.updateText2GqlSettings).not.toHaveBeenCalled();
    fireEvent.change(keyInput(), {target: {value: 'new-endpoint-secret'}});
    expect(saveButton()).toBeEnabled();
});

test('makes clearing a configured key explicit and mutually exclusive with replacement', async () => {
    await load();
    fireEvent.click(screen.getByRole('switch'));
    fireEvent.click(screen.getByRole('checkbox', {name: 'analysis.model_settings.clear_key'}));
    expect(keyInput()).toBeDisabled();
    await act(async () => fireEvent.click(saveButton()));
    await waitFor(() => expect(api.config.updateText2GqlSettings).toHaveBeenCalledTimes(1));
    const params = api.config.updateText2GqlSettings.mock.calls[0][0];
    expect(params).toMatchObject({enabled: false, clear_api_key: true});
    expect(params).not.toHaveProperty('api_key');
});

test('does not forward a previously configured cloud key to a new Ollama endpoint', async () => {
    await load();
    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(screen.getByText('analysis.query.connection_ollama'));
    fireEvent.change(screen.getByLabelText('analysis.query.model_name'), {target: {value: 'local-model'}});
    expect(saveButton()).toBeEnabled();
    await act(async () => fireEvent.click(saveButton()));
    await waitFor(() => expect(api.config.updateText2GqlSettings).toHaveBeenCalledTimes(1));
    expect(api.config.updateText2GqlSettings.mock.calls[0][0]).toEqual({
        enabled: true, connection_type: 'ollama', base_url: 'http://localhost:11434/v1',
        model: 'local-model', clear_api_key: true,
    });
});

test('ignores an old-account save response and clears the pending key on identity change', async () => {
    const pending = deferred();
    api.config.updateText2GqlSettings.mockReturnValue(pending.promise);
    const view = await load();
    fireEvent.change(keyInput(), {target: {value: 'old-account-secret'}});
    await act(async () => fireEvent.click(saveButton()));
    const signal = api.config.updateText2GqlSettings.mock.calls[0][1].signal;
    mockIdentityEpoch = 1;
    api.config.getText2GqlSettings.mockResolvedValue(response({model: 'new-account-model'}));
    view.rerender(<Text2GqlSettings />);
    expect(signal.aborted).toBe(true);
    await screen.findByDisplayValue('new-account-model');
    expect(keyInput()).toHaveValue('');
    await act(async () => pending.resolve(response({model: 'old-account-model'})));
    expect(screen.queryByDisplayValue('old-account-model')).not.toBeInTheDocument();
    expect(screen.queryByText('analysis.model_settings.saved')).not.toBeInTheDocument();
});


test('does not retain the existing key across an endpoint change even when the model is disabled', async () => {
    await load();
    fireEvent.click(screen.getByRole('switch'));
    fireEvent.change(screen.getByLabelText('Base URL'), {target: {value: 'https://new-endpoint.example/v1'}});
    expect(saveButton()).toBeDisabled();
    fireEvent.click(screen.getByRole('checkbox', {name: 'analysis.model_settings.clear_key'}));
    expect(saveButton()).toBeEnabled();
    await act(async () => fireEvent.click(saveButton()));
    await waitFor(() => expect(api.config.updateText2GqlSettings).toHaveBeenCalledTimes(1));
    expect(api.config.updateText2GqlSettings.mock.calls[0][0]).toMatchObject({
        enabled: false, base_url: 'https://new-endpoint.example/v1', clear_api_key: true,
    });
});


test('marks enabled model fields as required and explains empty fields visually and accessibly', async () => {
    await load();
    const model = screen.getByLabelText('analysis.query.model_name');
    const url = screen.getByLabelText('Base URL');
    expect(model).toHaveAttribute('aria-required', 'true');
    expect(url).toHaveAttribute('aria-required', 'true');
    expect(model.labels[0]).toHaveClass('requiredLabel');
    fireEvent.change(model, {target: {value: ''}});
    expect(model).toHaveAttribute('aria-invalid', 'true');
    expect(model).toHaveClass('ant-input-status-error');
    expect(saveButton()).toBeDisabled();
    fireEvent.click(screen.getByRole('switch'));
    expect(model).toHaveAttribute('aria-required', 'false');
    expect(model).toHaveAttribute('aria-invalid', 'false');
});
