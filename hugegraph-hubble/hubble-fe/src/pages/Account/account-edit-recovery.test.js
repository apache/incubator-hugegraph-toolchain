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
import EditLayer from './EditLayer';
import * as api from '../../api';

let mockAuthContext = null;

jest.mock('../../api', () => ({
    auth: {
        addUser: jest.fn(),
        updateUser: jest.fn(),
        updateAdminspace: jest.fn(),
        getUserInfo: jest.fn(),
    },
    manage: {
        getGraphSpaceList: jest.fn(),
    },
}));

jest.mock('../../auth/AuthContext', () => ({
    useAuthContext: () => ({context: mockAuthContext}),
}));

jest.mock('../../utils/rules', () => ({
    isName: {},
    isAccountName: {},
    required: () => ({required: true}),
}));

const mockTranslate = key => key;

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: mockTranslate}),
}));

const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return {promise, resolve, reject};
};

const props = {
    visible: true,
    onCancel: jest.fn(),
    refresh: jest.fn(),
};

beforeEach(() => {
    jest.clearAllMocks();
    mockAuthContext = null;
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
    api.manage.getGraphSpaceList.mockResolvedValue({status: 200, data: {records: []}});
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {user_name: 'alice', adminSpaces: []},
    });
});

test('creates a non-elevated account when preset API is unavailable',
    async () => {
        mockAuthContext = {capabilities: ['accounts_manage']};
        const onCreated = jest.fn();
        api.auth.addUser.mockResolvedValue({
            status: 200,
            data: {id: 'user-42'},
        });
        render(
            <EditLayer
                {...props}
                data={{}}
                op='create'
                onCreated={onCreated}
            />
        );
        await act(async () => undefined);

        expect(screen.queryByText('account.form.permission_preset'))
            .not.toBeInTheDocument();
        expect(api.manage.getGraphSpaceList).not.toHaveBeenCalled();

        fireEvent.change(screen.getByPlaceholderText(
            'account.form.id_placeholder'
        ), {target: {value: 'alice'}});
        fireEvent.change(screen.getByPlaceholderText(
            'account.form.default_password_placeholder'
        ), {target: {value: 'alice-password'}});
        await act(async () => {
            fireEvent.click(document.querySelector(
                '.ant-modal-footer .ant-btn-primary'
            ));
        });

        await waitFor(() => expect(api.auth.addUser).toHaveBeenCalled());
        const payload = api.auth.addUser.mock.calls[0][0];
        expect(payload).not.toHaveProperty('permission_preset');
        expect(payload).not.toHaveProperty('graphspace_permissions');
        expect(payload).not.toHaveProperty('adminSpaces');
        expect(payload).not.toHaveProperty('is_superadmin');
        expect(onCreated).toHaveBeenCalledWith({
            user_id: 'user-42',
            user_name: 'alice',
            is_superadmin: false,
        });
    });

test('keeps global super administrator identity in account creation', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.addUser.mockResolvedValue({status: 200});
    render(<EditLayer {...props} data={{}} op='create' />);

    fireEvent.change(screen.getByPlaceholderText(
        'account.form.id_placeholder'
    ), {target: {value: 'alice'}});
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    ), {target: {value: 'alice-password'}});
    fireEvent.click(screen.getByRole('switch'));
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    await waitFor(() => expect(api.auth.addUser).toHaveBeenCalledWith(
        expect.objectContaining({
            permission_preset: 'SUPER_ADMIN',
            is_superadmin: true,
            graphspace_permissions: [],
        }),
        expect.anything()
    ));
});

test('demotes a super administrator to an unassigned account', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            is_superadmin: true,
            graphspace_permissions: [],
        },
    });
    api.auth.updateUser.mockResolvedValue({status: 200});

    render(<EditLayer {...props} data={{id: 'user-id'}} op='edit' />);

    const toggle = await screen.findByRole('switch');
    await waitFor(() => expect(toggle).toBeChecked());
    fireEvent.click(toggle);
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    await waitFor(() => expect(api.auth.updateUser).toHaveBeenCalledWith(
        'user-id',
        expect.objectContaining({
            permission_preset: 'GS_READ_ONLY',
            is_superadmin: false,
            graphspace_permissions: [],
        }),
        expect.anything()
    ));
});

test('preserves permissions when editing a legacy account profile', async () => {
    mockAuthContext = {capabilities: ['accounts_manage']};
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            user_nickname: 'Alice',
            permission_preset: 'LEGACY_CUSTOM',
            graphspace_permissions: [{
                graphspace: 'analytics',
                permission_preset: 'GS_READ_ONLY',
            }],
        },
    });
    api.auth.updateUser.mockResolvedValue({status: 200});

    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    expect(await screen.findByDisplayValue('Alice')).toBeInTheDocument();
    expect(screen.queryByText('account.form.permission_preset'))
        .not.toBeInTheDocument();
    expect(api.manage.getGraphSpaceList).not.toHaveBeenCalled();

    fireEvent.change(screen.getByDisplayValue('Alice'), {
        target: {value: 'Alice Updated'},
    });
    await act(async () => {
        fireEvent.click(document.querySelector(
            '.ant-modal-footer .ant-btn-primary'
        ));
    });

    await waitFor(() => expect(api.auth.updateUser).toHaveBeenCalledWith(
        'alice',
        expect.objectContaining({
            user_name: 'alice',
            user_nickname: 'Alice Updated',
        }),
        expect.anything()
    ));
    await waitFor(() => expect(props.onCancel).toHaveBeenCalled());
    const payload = api.auth.updateUser.mock.calls[0][1];
    expect(payload).not.toHaveProperty('permission_preset');
    expect(payload).not.toHaveProperty('graphspace_permissions');
    expect(payload).not.toHaveProperty('adminSpaces');
    expect(payload).not.toHaveProperty('is_superadmin');
});

test('preserves mixed GraphSpace permissions on profile edit', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            user_nickname: 'Alice',
            graphspace_permissions: [{
                graphspace: 'read',
                permission_preset: 'GS_READ_ONLY',
            }, {
                graphspace: 'write',
                permission_preset: 'GS_READ_WRITE',
            }],
        },
    });
    api.auth.updateUser.mockResolvedValue({status: 200});

    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    expect(await screen.findByDisplayValue('Alice')).toBeInTheDocument();
    expect(screen.queryByText('account.form.permission_preset'))
        .not.toBeInTheDocument();
    fireEvent.change(screen.getByDisplayValue('Alice'), {
        target: {value: 'Alice Updated'},
    });
    await act(async () => {
        fireEvent.click(document.querySelector(
            '.ant-modal-footer .ant-btn-primary'
        ));
    });

    await waitFor(() => expect(api.auth.updateUser).toHaveBeenCalled());
    const payload = api.auth.updateUser.mock.calls[0][1];
    expect(payload.user_nickname).toBe('Alice Updated');
    expect(payload).not.toHaveProperty('permission_preset');
    expect(payload).not.toHaveProperty('graphspace_permissions');
    expect(payload).not.toHaveProperty('adminSpaces');
    expect(payload).not.toHaveProperty('is_superadmin');
});

test('updates a password without resubmitting unchanged permissions', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            permission_preset: 'GS_READ_ONLY',
            graphspace_permissions: [{
                graphspace: 'SPACE',
                permission_preset: 'GS_READ_ONLY',
            }],
        },
    });
    api.auth.updateUser.mockResolvedValue({status: 200});

    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    await screen.findByDisplayValue('alice');
    const password = screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    );
    fireEvent.change(password, {target: {value: 'new-password'}});
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    await waitFor(() => expect(api.auth.updateUser).toHaveBeenCalled());
    const payload = api.auth.updateUser.mock.calls[0][1];
    expect(payload.user_password).toBe('new-password');
    expect(payload).not.toHaveProperty('permission_preset');
    expect(payload).not.toHaveProperty('graphspace_permissions');
});

test('requires password and super administrator changes to be separate', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            is_superadmin: false,
            permission_preset: 'GS_READ_ONLY',
            graphspace_permissions: [],
        },
    });
    api.auth.updateUser.mockResolvedValue({status: 200});

    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    await screen.findByDisplayValue('alice');
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    ), {target: {value: 'new-password'}});
    fireEvent.click(screen.getByRole('switch'));
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    expect((await screen.findAllByText(
        'account.feedback.password_permission_separate'
    )).length).toBeGreaterThan(0);
    expect(api.auth.updateUser).not.toHaveBeenCalled();
});

test('rejects a combined profile and promotion before sending it', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            is_superadmin: false,
            permission_preset: 'GS_READ_ONLY',
            graphspace_permissions: [],
        },
    });
    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    await screen.findByDisplayValue('alice');
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    ), {target: {value: 'new-password'}});
    fireEvent.click(screen.getByRole('switch'));
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    expect((await screen.findAllByText(
        'account.feedback.password_permission_separate'
    )).length).toBeGreaterThan(0);
    expect(api.auth.updateUser).not.toHaveBeenCalled();
});

test('keeps GraphSpace membership out of the account profile form', async () => {
    mockAuthContext = {
        capabilities: ['accounts_manage', 'account_permission_presets'],
    };
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'alice',
            permission_preset: 'GS_READ_ONLY',
            graphspace_permissions: [{
                graphspace: 'SPACE',
                permission_preset: 'GS_READ_ONLY',
            }],
        },
    });

    render(<EditLayer {...props} data={{id: 'alice'}} op='edit' />);

    await screen.findByDisplayValue('alice');
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByText('account.form.permission_preset'))
        .not.toBeInTheDocument();
    expect(screen.queryByText('account.form.graphspaces'))
        .not.toBeInTheDocument();
});

test('ignores a second account mutation while the first submit is pending', async () => {
    const mutation = deferred();
    api.auth.addUser.mockReturnValue(mutation.promise);
    render(<EditLayer {...props} data={{}} op='create' />);

    const submit = document.querySelector('.ant-modal-footer .ant-btn-primary');
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.id_placeholder'
    ), {target: {value: 'alice'}});
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    ), {target: {value: 'alice-password'}});
    fireEvent.click(submit);
    fireEvent.click(submit);

    await waitFor(() => expect(api.auth.addUser).toHaveBeenCalledTimes(1));
    await act(async () => mutation.resolve({status: 200}));
});

test('keeps a late account detail response from replacing the current user', async () => {
    const requestA = deferred();
    const requestB = deferred();
    api.auth.getUserInfo.mockImplementation(id => (
        id === 'A' ? requestA.promise : requestB.promise
    ));
    const view = render(<EditLayer {...props} data={{id: 'A'}} op='detail' />);

    view.rerender(<EditLayer {...props} data={{id: 'B'}} op='detail' />);
    await act(async () => requestB.resolve({
        status: 200,
        data: {user_name: 'bob', user_nickname: 'Bob'},
    }));
    expect(await screen.findByText('bob')).toBeInTheDocument();

    await act(async () => requestA.resolve({
        status: 200,
        data: {user_name: 'alice', user_nickname: 'Alice'},
    }));
    expect(screen.queryByText('alice')).not.toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
});

test('clears the previous account when the next detail request fails', async () => {
    const requestB = deferred();
    api.auth.getUserInfo.mockImplementation(id => (
        id === 'A' ? Promise.resolve({
            status: 200,
            data: {user_name: 'alice', user_nickname: 'Alice'},
        }) : requestB.promise
    ));
    const view = render(<EditLayer {...props} data={{id: 'A'}} op='detail' />);
    expect(await screen.findByText('alice')).toBeInTheDocument();

    view.rerender(<EditLayer {...props} data={{id: 'B'}} op='detail' />);
    await waitFor(() => expect(api.auth.getUserInfo).toHaveBeenCalledTimes(2));
    await act(async () => requestB.reject(new Error('offline')));
    await waitFor(() => expect(screen.queryByText('alice')).not.toBeInTheDocument());
});

test('requires an explicit password when creating an account', async () => {
    render(<EditLayer {...props} data={{}} op='create' />);
    await act(async () => undefined);

    expect(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    )).toBeRequired();
});

test('prefills a guided account creation with the requested account ID', () => {
    render(
        <EditLayer
            {...props}
            data={{user_name: 'guided_user'}}
            op='create'
        />
    );

    expect(screen.getByPlaceholderText('account.form.id_placeholder'))
        .toHaveValue('guided_user');
});

test('keeps a specific account creation error visible in the form', async () => {
    api.auth.addUser.mockResolvedValue({
        status: 400,
        message: 'Refresh the GraphSpace list and select an existing one.',
    });
    render(<EditLayer {...props} data={{}} op='create' />);
    await act(async () => undefined);

    fireEvent.change(screen.getByPlaceholderText(
        'account.form.id_placeholder'
    ), {target: {value: 'alice'}});
    fireEvent.change(screen.getByPlaceholderText(
        'account.form.default_password_placeholder'
    ), {target: {value: 'secret'}});
    fireEvent.click(document.querySelector(
        '.ant-modal-footer .ant-btn-primary'
    ));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('account.feedback.save_failed');
    expect(alert).toHaveTextContent(
        'Refresh the GraphSpace list and select an existing one.'
    );
    expect(props.onCancel).not.toHaveBeenCalled();
});

test('shows the derived space administrator level in account details', async () => {
    api.auth.getUserInfo.mockResolvedValue({
        status: 200,
        data: {
            user_name: 'space-admin',
            is_superadmin: false,
            adminSpaces: [{name: 'SPACE'}],
        },
    });

    render(<EditLayer {...props} data={{id: 'space-admin'}} op='detail' />);

    expect(await screen.findByText(
        'account.permission_preset.GS_ADMIN'
    )).toBeInTheDocument();
});
