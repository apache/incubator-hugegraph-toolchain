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

import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Account from './index';
import * as api from '../../api';

let mockCurrentUser;
let mockAuthContext;

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

jest.mock('../../api', () => ({
    auth: {
        getAllUserList: jest.fn(),
        delUser: jest.fn(),
    },
}));

jest.mock('../../utils/user', () => ({getUser: () => mockCurrentUser}));
jest.mock('../../auth/AuthContext', () => ({
    useAuthContext: () => mockAuthContext,
}));
jest.mock('./EditLayer', () => props => (
    props.visible ? (
        <button
            onClick={() => props.onCreated?.({
                user_id: 'created-id',
                user_name: props.data.user_name,
            })}
        >
            guided account {props.data.user_name}
        </button>
    ) : null
));
jest.mock('./SpaceAccess', () => props => (
    <div>
        space access
        {props.onCreateAccount && (
            <button
                onClick={() => props.onCreateAccount({
                    graphspaces: ['SPACE_A', 'SPACE_B'],
                    permission_preset: 'GS_READ_ONLY',
                    user_name: 'guided_user',
                })}
            >
                request guided account
            </button>
        )}
        {props.pendingAccount && (
            <span data-testid="pending-account">
                pending {props.pendingAccount.user_name}
                {' '}
                {props.pendingAccount.graphspaces.join(',')}
                {' '}
                {props.pendingAccount.permission_preset}
            </span>
        )}
    </div>
));

beforeEach(() => {
    jest.clearAllMocks();
    mockCurrentUser = {id: 'admin', is_superadmin: true};
    mockAuthContext = {
        refresh: jest.fn(),
        context: {
            capabilities: ['account_permission_presets'],
            actions: {
                accounts: ['create', 'read', 'update', 'delete'],
                authorizations: ['read', 'grant', 'revoke'],
            },
        },
    };
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

test('shows an explicit recoverable state when no account read action remains', async () => {
    mockAuthContext = {
        refresh: jest.fn().mockResolvedValue(undefined),
        context: {actions: {accounts: [], members: [], roles: [], authorizations: []}},
    };

    render(<Account />);

    expect(screen.getByRole('alert')).toHaveTextContent('account.permission_changed');
    expect(api.auth.getAllUserList).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', {name: 'account.refresh_permissions'}));
    expect(mockAuthContext.refresh).toHaveBeenCalledTimes(1);
});

test('does not present a failed account request as an empty user table', async () => {
    api.auth.getAllUserList
        .mockRejectedValueOnce(new Error('down'))
        .mockResolvedValueOnce({
            status: 200,
            data: {records: [{user_name: 'analyst'}], total: 1},
        });

    render(<Account />);

    expect(await screen.findByText('account.load.unavailable')).toBeInTheDocument();
    expect(screen.queryByText('analyst')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'account.load.retry'}));

    expect(await screen.findByText('analyst')).toBeInTheDocument();
    expect(screen.queryByText('account.load.unavailable')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'common.action.detail'})).toBeInTheDocument();
});

test('labels administrators, space administrators, and unassigned users', async () => {
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {
            records: [
                {user_name: 'root', is_superadmin: true, adminSpaces: []},
                {user_name: 'space-admin', is_superadmin: false, adminSpaces: ['SPACE']},
                {user_name: 'analyst', is_superadmin: false, adminSpaces: []},
            ],
            total: 3,
        },
    });

    render(<Account />);

    expect(await screen.findByText('account.permission_preset.SUPER_ADMIN'))
        .toBeInTheDocument();
    expect(screen.getByText('account.permission_preset.GS_ADMIN'))
        .toBeInTheDocument();
    expect(screen.getByText('account.permission_preset.unassigned'))
        .toBeInTheDocument();
});

test('does not label an unassigned legacy account as GraphSpace read-only', async () => {
    mockAuthContext.context.capabilities = ['accounts_manage'];
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {
            records: [{user_name: 'analyst', adminSpaces: []}],
            total: 1,
        },
    });

    render(<Account />);

    expect(await screen.findByText('account.permission_preset.unassigned'))
        .toBeInTheDocument();
    expect(screen.queryByText('account.permission_preset.GS_READ_ONLY'))
        .not.toBeInTheDocument();
});

test('shows standalone accounts as read-write without GraphSpace resources',
    async () => {
        mockAuthContext.context = {
            mode: 'NON_PD',
            capabilities: [],
            actions: {
                accounts: ['create', 'read', 'update', 'delete'],
                authorizations: [],
            },
        };
        api.auth.getAllUserList.mockResolvedValue({
            status: 200,
            data: {
                records: [{
                    user_name: 'writer',
                    permission_preset: 'GS_READ_WRITE',
                }, {
                    user_name: 'legacy',
                }],
                total: 2,
            },
        });

        render(<Account />);

        expect(await screen.findByText(
            'account.permission_preset.GS_READ_WRITE'
        )).toBeInTheDocument();
        expect(screen.getByText('account.permission_preset.unassigned'))
            .toBeInTheDocument();
        expect(screen.queryByText('account.col.resource'))
            .not.toBeInTheDocument();
        expect(screen.queryByText('account.space_access.scoped_tab'))
            .not.toBeInTheDocument();
    });

test('space administrators use scoped management without loading global accounts', async () => {
    mockCurrentUser = {
        id: 'space-admin',
        is_superadmin: true,
        adminSpaces: ['UNTRUSTED-CACHED-SPACE'],
    };
    mockAuthContext = {
        context: {
            actions: {
                accounts: [],
                members: ['read', 'add', 'remove'],
                roles: ['read', 'create', 'update', 'delete'],
                authorizations: ['read', 'grant', 'revoke'],
            },
            scopes: {admin_graphspaces: ['SPACE']},
        },
    };
    render(<Account />);

    expect(await screen.findByText('space access')).toBeInTheDocument();
    expect(api.auth.getAllUserList).not.toHaveBeenCalled();
});

test('ignores cached superadmin fields when the server denies account mutations', async () => {
    mockCurrentUser = {id: 'cached-admin', is_superadmin: true};
    mockAuthContext = {
        context: {
            actions: {accounts: ['read'], authorizations: ['read']},
        },
    };
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {
            records: [{user_name: 'analyst', is_superadmin: false, adminSpaces: []}],
            total: 1,
        },
    });

    render(<Account />);

    expect(await screen.findByText('analyst')).toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'account.create'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'common.action.edit'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {
        name: 'common.action.assign_permission',
    })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'common.action.delete'})).not.toBeInTheDocument();
});

test('super administrators retain all account management actions', async () => {
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {
            records: [{user_name: 'analyst', is_superadmin: false, adminSpaces: []}],
            total: 1,
        },
    });

    render(<Account />);

    expect(await screen.findByText('analyst')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'account.create'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'common.action.detail'})).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {name: 'account.action.more'}));
    expect(await screen.findByText('common.action.edit')).toBeInTheDocument();
    expect(screen.getByText('account.action.manage_membership')).toBeInTheDocument();
    expect(screen.getByText('common.action.delete')).toBeInTheDocument();
});

test('preserves every GraphSpace when managing an account membership', async () => {
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {
            records: [{
                id: 'analyst-id',
                user_name: 'analyst',
                graphspace_permissions: [
                    {graphspace: 'SPACE_A', permission_preset: 'GS_READ_ONLY'},
                    {graphspace: 'SPACE_B', permission_preset: 'GS_READ_ONLY'},
                ],
            }],
            total: 1,
        },
    });

    render(<Account />);

    expect(await screen.findByText('analyst')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {name: 'account.action.more'}));
    await userEvent.click(screen.getByText('account.action.manage_membership'));

    expect(await screen.findByTestId('pending-account'))
        .toHaveTextContent('pending analyst SPACE_A,SPACE_B');
});

test('returns a guided account creation to the selected GraphSpaces', async () => {
    mockAuthContext.context.actions.members = ['read', 'add', 'remove'];
    api.auth.getAllUserList.mockResolvedValue({
        status: 200,
        data: {records: [], total: 0},
    });
    render(<Account />);

    await userEvent.click(screen.getByRole('tab', {
        name: 'account.space_access.scoped_tab',
    }));
    await userEvent.click(screen.getByRole('button', {
        name: 'request guided account',
    }));

    expect(screen.getByRole('button', {
        name: 'guided account guided_user',
    })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {
        name: 'guided account guided_user',
    }));
    expect(await screen.findByText(
        'pending guided_user SPACE_A,SPACE_B GS_READ_ONLY'
    ))
        .toBeInTheDocument();
});
