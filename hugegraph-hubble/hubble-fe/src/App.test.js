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

import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import App from './App';
import * as api from './api';

jest.mock('./api', () => ({
    config: {
        getConfig: jest.fn(),
    },
}));

jest.mock('./routes', () => ({element}) => (
    <div
        data-testid="app-route"
        data-auth-enabled={String(
            jest.requireActual('./utils/config').isAuthEnabled()
        )}
    >
        {element}
        <input aria-label="unsaved draft" />
    </div>
));

jest.mock('./layout.ant', () => () => <div>Hubble layout</div>);
jest.mock('./auth/AuthContext', () => ({
    AuthContextProvider: ({children}) => children,
}));

test('wires the Hubble layout into the application router', async () => {
    sessionStorage.clear();
    api.config.getConfig.mockResolvedValue({
        status: 200,
        data: {pd_enabled: false, auth_enabled: false},
    });
    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );
    expect(await screen.findByTestId('app-route')).toBeInTheDocument();
    expect(screen.getByText('Hubble layout')).toBeInTheDocument();
    expect(JSON.parse(sessionStorage.getItem('hubble_config_'))).toEqual({pd_enabled: false, auth_enabled: false});
});

test('shows a retry surface when configuration bootstrap fails', async () => {
    api.config.getConfig.mockRejectedValue(new Error('offline'));

    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(
        'Unable to load Hubble configuration.'
    );
    expect(screen.getByRole('button', {name: 'Retry'})).toBeInTheDocument();
});

test('revalidates fail-closed server capabilities until verified', async () => {
    jest.useFakeTimers();
    sessionStorage.setItem('user_', JSON.stringify({user_name: 'admin'}));
    localStorage.setItem('user', 'admin');
    api.config.getConfig
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: false,
                auth_enabled: true,
                server_capabilities_verified: false,
            },
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: false,
                auth_enabled: false,
                server_capabilities_verified: true,
            },
        });

    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );
    expect(await screen.findByTestId('app-route')).toBeInTheDocument();
    expect(screen.getByTestId('app-route'))
        .toHaveAttribute('data-auth-enabled', 'true');

    await act(async () => {
        jest.advanceTimersByTime(2000);
    });
    await waitFor(() => expect(api.config.getConfig).toHaveBeenCalledTimes(2));
    expect(JSON.parse(sessionStorage.getItem('hubble_config_')))
        .toMatchObject({
            auth_enabled: false,
            server_capabilities_verified: true,
        });
    expect(screen.getByTestId('app-route'))
        .toHaveAttribute('data-auth-enabled', 'false');
    expect(sessionStorage.getItem('user_')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    jest.useRealTimers();
});

test('continues revalidation after a transient retry failure', async () => {
    jest.useFakeTimers();
    api.config.getConfig
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: true,
                auth_enabled: true,
                server_capabilities_verified: false,
            },
        })
        .mockRejectedValueOnce(new Error('temporarily unavailable'))
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: true,
                auth_enabled: false,
                server_capabilities_verified: true,
            },
        });

    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );
    expect(await screen.findByTestId('app-route')).toHaveAttribute(
        'data-auth-enabled', 'true'
    );

    await act(async () => {
        jest.advanceTimersByTime(2000);
    });
    await waitFor(() => expect(api.config.getConfig).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByTestId('app-route')).toHaveAttribute(
        'data-auth-enabled', 'true'
    );

    await act(async () => {
        jest.advanceTimersByTime(2000);
    });
    await waitFor(() => expect(api.config.getConfig).toHaveBeenCalledTimes(3));
    expect(screen.getByTestId('app-route')).toHaveAttribute(
        'data-auth-enabled', 'false'
    );
    jest.useRealTimers();
});

test('periodically revalidates a verified authentication mode', async () => {
    jest.useFakeTimers();
    api.config.getConfig
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: true,
                auth_enabled: false,
                server_capabilities_verified: true,
            },
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {
                pd_enabled: true,
                auth_enabled: true,
                server_capabilities_verified: true,
            },
        });

    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );
    expect(await screen.findByTestId('app-route')).toHaveAttribute(
        'data-auth-enabled', 'false'
    );

    await act(async () => {
        jest.advanceTimersByTime(30_000);
    });
    await waitFor(() => expect(api.config.getConfig).toHaveBeenCalledTimes(2));
    expect(screen.getByTestId('app-route')).toHaveAttribute(
        'data-auth-enabled', 'true'
    );
    jest.useRealTimers();
});

test('preserves route state when periodic configuration is unchanged', async () => {
    jest.useFakeTimers();
    const config = {
        pd_enabled: true,
        auth_enabled: true,
        graph_create_enabled: true,
        cypher_enabled: true,
        server_capabilities_verified: true,
    };
    api.config.getConfig
        .mockResolvedValueOnce({status: 200, data: config})
        .mockResolvedValueOnce({status: 200, data: {...config}});

    render(
        <MemoryRouter
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App />
        </MemoryRouter>
    );
    const draft = await screen.findByRole('textbox', {
        name: 'unsaved draft',
    });
    fireEvent.change(draft, {target: {value: 'keep me'}});

    await act(async () => {
        jest.advanceTimersByTime(30_000);
    });
    await waitFor(() => expect(api.config.getConfig).toHaveBeenCalledTimes(2));
    expect(screen.getByRole('textbox', {name: 'unsaved draft'}))
        .toHaveValue('keep me');
    jest.useRealTimers();
});
