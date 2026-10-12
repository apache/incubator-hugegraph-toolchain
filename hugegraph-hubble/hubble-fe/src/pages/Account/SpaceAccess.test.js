/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from '@testing-library/react';
import SpaceAccess, {loadAllPages, rolesPreset} from './SpaceAccess';
import * as api from '../../api';

let mockAuthContext;

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: key => ({
            'account.space_access.member.missing_response_subject': '账号',
            'account.space_access.member.missing_response_predicate': '不存在',
        }[key] ?? key),
    }),
}));

jest.mock('../../auth/AuthContext', () => ({
    useAuthContext: () => mockAuthContext,
}));

jest.mock('../../api', () => ({
    auth: {
        getSpaceMembers: jest.fn(),
        getSpaceAccount: jest.fn(),
        getSpaceAdmins: jest.fn(),
        setSpaceAdmin: jest.fn(),
        removeSpaceAdmin: jest.fn(),
        setSpacePreset: jest.fn(),
        getSpaceRoles: jest.fn(),
        getSpaceTargets: jest.fn(),
        getSpaceAccesses: jest.fn(),
        addSpaceMember: jest.fn(),
        updateSpaceMember: jest.fn(),
        deleteSpaceMember: jest.fn(),
        addSpaceRole: jest.fn(),
        updateSpaceRole: jest.fn(),
        deleteSpaceRole: jest.fn(),
        addSpaceTarget: jest.fn(),
        updateSpaceTarget: jest.fn(),
        deleteSpaceTarget: jest.fn(),
        saveSpaceAccess: jest.fn(),
        deleteSpaceAccess: jest.fn(),
        getAllUserList: jest.fn(),
    },
    manage: {
        getGraphSpaceList: jest.fn(),
    },
}));

const deferred = () => {
    let resolve;
    const promise = new Promise(resolvePromise => {
        resolve = resolvePromise;
    });
    return {promise, resolve};
};

const page = records => ({status: 200, data: {records, total: records.length}});

const setResponses = ({members = [], roles = [], targets = [], accesses = []} = {}) => {
    api.auth.getSpaceMembers.mockResolvedValue(page(members));
    api.auth.getSpaceAdmins.mockResolvedValue(page([]));
    api.auth.getSpaceRoles.mockResolvedValue(page(roles));
    api.auth.getSpaceTargets.mockResolvedValue(page(targets));
    api.auth.getSpaceAccesses.mockResolvedValue({status: 200, data: accesses});
};

test('requires explicit preset for mixed or legacy member roles', () => {
    expect(rolesPreset([
        {role_name: 'observer'},
        {role_name: 'custom-role'},
    ])).toBeNull();
    expect(rolesPreset([
        {role_name: 'observer'},
        {role_name: 'analyst'},
    ])).toBeNull();
    expect(rolesPreset([
        {role_name: 'observer', permission_preset: 'GS_READ_ONLY'},
    ])).toBe('GS_READ_ONLY');
});

test('keeps graphspace administrator selected with member roles', () => {
    expect(rolesPreset([
        {role_name: 'analyst', permission_preset: 'GS_READ_WRITE'},
        {permission_preset: 'GS_ADMIN'},
    ])).toBe('GS_ADMIN');
    expect(rolesPreset([
        {role_name: 'analyst'},
        {role_name: 'custom-role', permissions: ['delete']},
    ])).toBeNull();
    expect(rolesPreset([
        {role_name: 'analyst', permissions: ['write', 'delete']},
    ])).toBeNull();
    expect(rolesPreset([
        {role_name: 'custom-role'},
        {permission_preset: 'GS_ADMIN'},
    ])).toBeNull();
});

test('loads every page without silently truncating member-management data', async () => {
    const firstPage = Array.from({length: 500}, (_, index) => ({id: index}));
    const request = jest.fn()
        .mockResolvedValueOnce({
            status: 200,
            data: {records: firstPage, total: '501'},
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {records: [{id: 500}], total: 501},
        });

    const response = await loadAllPages(request, {params: {query: ''}});

    expect(response.data.records).toHaveLength(501);
    expect(request).toHaveBeenNthCalledWith(1, {
        query: '',
        page_no: 1,
        page_size: 500,
    }, expect.any(Object));
    expect(request).toHaveBeenNthCalledWith(2, {
        query: '',
        page_no: 2,
        page_size: 500,
    }, expect.any(Object));
});

test('continues after short pages while declared records remain', async () => {
    const page = offset => Array.from({length: 200}, (_, index) => ({
        id: offset + index,
    }));
    const request = jest.fn()
        .mockResolvedValueOnce({
            status: 200,
            data: {records: page(0), total: 501},
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {records: page(200), total: 501},
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {records: page(400).slice(0, 101), total: 501},
        });

    const response = await loadAllPages(request);

    expect(response.data.records).toHaveLength(501);
    expect(request).toHaveBeenCalledTimes(3);
});

test('treats blank total as absent instead of zero', async () => {
    const firstPage = Array.from({length: 500}, (_, index) => ({id: index}));
    const request = jest.fn()
        .mockResolvedValueOnce({
            status: 200,
            data: {records: firstPage, total: '   '},
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {records: [{id: 500}], total: '   '},
        });

    const response = await loadAllPages(request);

    expect(response.data.records).toHaveLength(501);
    expect(request).toHaveBeenCalledTimes(2);
});

test('fails when a paged response ends before its declared total', async () => {
    const request = jest.fn()
        .mockResolvedValueOnce({
            status: 200,
            data: {records: [{id: 0}], total: 2},
        })
        .mockResolvedValueOnce({
            status: 200,
            data: {records: [], total: 2},
        });

    await expect(loadAllPages(request))
        .rejects.toThrow('Record list ended before its declared total');
});

test('shows legacy role names without treating them as presets', async () => {
    setResponses({
        members: [{
            user_id: 'alice',
            user_name: 'alice',
            roles: [{role_id: 'custom', role_name: 'analyst'}],
        }],
    });

    render(<SpaceAccess />);

    expect(await screen.findByText(
        'analyst · account.permission_preset.legacy_custom'
    )).toBeInTheDocument();
});

beforeEach(() => {
    jest.clearAllMocks();
    api.auth.getSpaceAccount.mockResolvedValue({
        status: 200,
        data: {user_id: 'account-id', user_name: 'bob'},
    });
    mockAuthContext = {
        context: {
            context_version: 'v1',
            actions: {
                members: ['read', 'add', 'remove'],
                roles: ['read', 'create', 'update', 'delete'],
                authorizations: ['read', 'grant', 'revoke'],
            },
            scopes: {
                all_graphspaces: false,
                admin_graphspaces: ['SPACE_A'],
            },
        },
    };
    setResponses({
        members: [{user_id: 'alice', user_name: 'alice', roles: []}],
        roles: [{id: 'reader-id', role_name: 'reader'}],
    });
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

test('uses only path-scoped APIs for a space administrator', async () => {
    render(<SpaceAccess />);

    expect((await screen.findAllByText('alice')).length).toBeGreaterThan(0);
    expect(api.auth.getSpaceMembers).toHaveBeenCalledWith(
        'SPACE_A', expect.any(Object), expect.any(Object)
    );
    expect(api.auth.getSpaceRoles).not.toHaveBeenCalled();
    expect(api.auth.getSpaceAdmins).toHaveBeenCalledWith(
        'SPACE_A', expect.any(Object), expect.any(Object)
    );
    expect(api.auth.getSpaceTargets).not.toHaveBeenCalled();
    expect(api.auth.getSpaceAccesses).not.toHaveBeenCalled();
    expect(api.auth.getAllUserList).not.toHaveBeenCalled();
    expect(api.manage.getGraphSpaceList).not.toHaveBeenCalled();
});

test('drops late scoped responses after the authorization context changes', async () => {
    const requestA = deferred();
    api.auth.getSpaceMembers.mockReturnValueOnce(requestA.promise);
    const view = render(<SpaceAccess />);
    await waitFor(() => expect(api.auth.getSpaceMembers).toHaveBeenCalledWith(
        'SPACE_A', expect.any(Object), expect.any(Object)
    ));

    mockAuthContext = {
        context: {
            ...mockAuthContext.context,
            context_version: 'v2',
            scopes: {
                all_graphspaces: false,
                admin_graphspaces: ['SPACE_B'],
            },
        },
    };
    api.auth.getSpaceMembers.mockResolvedValueOnce(page([
        {user_id: 'bob', user_name: 'bob', roles: []},
    ]));
    view.rerender(<SpaceAccess />);

    expect((await screen.findAllByText('bob')).length).toBeGreaterThan(0);
    await act(async () => requestA.resolve(page([
        {user_id: 'alice', user_name: 'alice', roles: []},
    ])));
    expect(screen.queryByText('alice')).not.toBeInTheDocument();
    expect(screen.getAllByText('bob').length).toBeGreaterThan(0);
});

test('does not infer mutations when the server grants read-only actions', async () => {
    mockAuthContext = {
        context: {
            ...mockAuthContext.context,
            actions: {
                members: ['read'],
                roles: ['read'],
                authorizations: ['read'],
            },
        },
    };

    render(<SpaceAccess />);

    expect((await screen.findAllByText('alice')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', {
        name: 'account.space_access.member.add',
    })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {
        name: 'common.action.delete',
    })).not.toBeInTheDocument();
});

test('submits only the selected preset when adding a member', async () => {
    api.auth.setSpacePreset.mockResolvedValue({status: 200});
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {
        target: {value: 'bob'},
    });
    fireEvent.blur(account);
    await waitFor(() => expect(api.auth.getSpaceAccount).toHaveBeenCalledWith(
        'SPACE_A', 'bob', expect.any(Object)
    ));
    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[comboboxes.length - 1]);
    fireEvent.click(screen.getByText('account.permission_preset.GS_READ_WRITE'));
    fireEvent.click(screen.getByRole('button', {name: 'common.action.save'}));

    await waitFor(() => expect(api.auth.setSpacePreset).toHaveBeenCalledWith(
        'SPACE_A', 'account-id', 'bob', 'GS_READ_WRITE', expect.any(Object)));
});

test('uses the preset API for GS admin', async () => {
    mockAuthContext.context.scopes = {
        all_graphspaces: true,
        admin_graphspaces: [],
    };
    api.manage.getGraphSpaceList.mockResolvedValue(page([
        {name: 'SPACE_A'},
    ]));
    api.auth.setSpacePreset.mockResolvedValue({status: 200});
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {
        target: {value: 'bob'},
    });
    fireEvent.blur(account);
    await waitFor(() => expect(api.auth.getSpaceAccount).toHaveBeenCalledWith(
        'SPACE_A', 'bob', expect.any(Object)
    ));
    const comboboxes = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(comboboxes[comboboxes.length - 1]);
    fireEvent.click(screen.getByText('account.permission_preset.GS_ADMIN'));
    fireEvent.click(screen.getByRole('button', {name: 'common.action.save'}));

    await waitFor(() => expect(api.auth.setSpacePreset).toHaveBeenCalledWith(
        'SPACE_A', 'account-id', 'bob', 'GS_ADMIN', expect.any(Object)));
    expect(api.auth.addSpaceMember).not.toHaveBeenCalled();
});

test('does not let a space administrator manage other space administrators',
    async () => {
        api.auth.getSpaceAdmins.mockResolvedValueOnce(page([{
            id: 'admin-id',
            name: 'space-admin',
        }]));

        render(<SpaceAccess />);

        const row = (await screen.findByText('space-admin')).closest('tr');
        expect(within(row).queryByRole('button', {
            name: 'common.action.edit',
        })).not.toBeInTheDocument();
        expect(within(row).queryByRole('button', {
            name: 'common.action.delete',
        })).not.toBeInTheDocument();
    });

test('shows GraphSpace administrators through the preset model', async () => {
    api.auth.getSpaceAdmins.mockResolvedValueOnce(page([{
        id: 'admin-id',
        name: 'space-admin',
    }]));

    render(<SpaceAccess />);

    expect(await screen.findByText('space-admin')).toBeInTheDocument();
    expect(screen.getByText('account.permission_preset.GS_ADMIN'))
        .toBeInTheDocument();
});

test('opens the scoped member flow with account and GraphSpace prefilled',
    async () => {
        const onHandled = jest.fn();
        render(
            <SpaceAccess
                pendingAccount={{user_name: 'bob'}}
                onPendingAccountHandled={onHandled}
            />
        );

        expect(await screen.findByDisplayValue('bob')).toBeInTheDocument();
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByTitle('SPACE_A')).toBeInTheDocument();
        expect(within(dialog).getByRole('combobox', {
            name: 'account.space_access.graphspace',
        })).toBeEnabled();
        expect(onHandled).toHaveBeenCalledTimes(1);
    });

test('assigns an account to multiple GraphSpaces selected in the dialog', async () => {
    mockAuthContext.context.scopes = {
        all_graphspaces: true,
        admin_graphspaces: [],
    };
    api.manage.getGraphSpaceList.mockResolvedValue({
        status: 200,
        data: {
            records: [{name: 'SPACE_A'}, {name: 'SPACE_B'}],
            total: 2,
        },
    });
    api.auth.setSpacePreset.mockResolvedValue({status: 200});
    const onHandled = jest.fn();
    render(
        <SpaceAccess
            pendingAccount={{
                user_id: 'bob-id',
                user_name: 'bob',
                graphspace: 'SPACE_B',
            }}
            onPendingAccountHandled={onHandled}
        />
    );

    expect(await screen.findByDisplayValue('bob')).toBeInTheDocument();
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByTitle('SPACE_B')).toBeInTheDocument();
    const graphSpaceSelect = within(dialog).getByRole('combobox', {
        name: 'account.space_access.graphspace',
    });
    fireEvent.mouseDown(graphSpaceSelect);
    const graphSpaceOptions = await screen.findAllByText('SPACE_A');
    fireEvent.click(graphSpaceOptions.find(option =>
        option.classList.contains('ant-select-item-option-content')
    ));
    const roleSelects = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(roleSelects[roleSelects.length - 1]);
    fireEvent.click(screen.getByText('account.permission_preset.GS_READ_ONLY'));
    fireEvent.click(within(dialog).getByRole('button', {
        name: 'common.action.save',
    }));

    await waitFor(() => expect(api.auth.setSpacePreset).toHaveBeenCalledTimes(2));
    expect(api.auth.setSpacePreset).toHaveBeenCalledWith(
        'SPACE_A', 'bob-id', 'bob', 'GS_READ_ONLY', expect.any(Object)
    );
    expect(api.auth.setSpacePreset).toHaveBeenCalledWith(
        'SPACE_B', 'bob-id', 'bob', 'GS_READ_ONLY', expect.any(Object)
    );
    expect(onHandled).toHaveBeenCalledTimes(1);
});

test('keeps the current GraphSpace fixed for member-list operations', async () => {
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByTitle('SPACE_A')).toBeInTheDocument();
    expect(within(dialog).getByRole('combobox', {
        name: 'account.space_access.graphspace',
    })).toBeDisabled();
});

test('explains that an unknown account must be created first', async () => {
    const onCreateAccount = jest.fn();
    api.auth.getSpaceAccount.mockResolvedValue({
        status: 400,
        message: 'The user or group is not exist',
    });
    render(<SpaceAccess onCreateAccount={onCreateAccount} />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {target: {value: 'missing-user'}});
    fireEvent.blur(account);

    expect(await within(dialog).findByText(
        'account.space_access.member.account_not_found'
    )).toBeInTheDocument();
    expect(api.auth.getSpaceAccount).toHaveBeenCalledWith(
        'SPACE_A', 'missing-user', expect.any(Object)
    );
    expect(api.auth.setSpacePreset).not.toHaveBeenCalled();

    const roleSelects = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(roleSelects[roleSelects.length - 1]);
    fireEvent.click(screen.getByText('account.permission_preset.GS_READ_ONLY'));
    fireEvent.click(within(dialog).getByRole('button', {
        name: 'account.space_access.member.create_account',
    }));
    expect(onCreateAccount).toHaveBeenCalledWith({
        graphspaces: ['SPACE_A'],
        permission_preset: 'GS_READ_ONLY',
        user_name: 'missing-user',
    });
});

test('tells a GraphSpace administrator to contact a global administrator', async () => {
    api.auth.getSpaceAccount.mockResolvedValue({
        status: 400,
        message: 'The user or group is not exist',
    });
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {target: {value: 'missing-user'}});
    fireEvent.blur(account);

    expect(await within(dialog).findByText(
        'account.space_access.member.account_not_found_contact_admin'
    )).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', {
        name: 'account.space_access.member.create_account',
    })).not.toBeInTheDocument();
});

test('recognizes a localized missing-account response', async () => {
    api.auth.getSpaceAccount.mockResolvedValue({
        status: 400,
        message: '账号 missing-user 不存在，请先创建账号再分配 GraphSpace 权限。',
    });
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {target: {value: 'missing-user'}});
    fireEvent.blur(account);

    expect(await within(dialog).findByText(
        'account.space_access.member.account_not_found_contact_admin'
    )).toBeInTheDocument();
});

test('does not describe an unrelated bad request as a missing account', async () => {
    api.auth.getSpaceAccount.mockResolvedValue({
        status: 400,
        message: 'Invalid permission preset',
    });
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {target: {value: 'alice'}});
    fireEvent.blur(account);

    expect(await within(dialog).findByText(
        'account.space_access.member.account_check_failed'
    )).toBeInTheDocument();
    expect(within(dialog).queryByText(
        'account.space_access.member.account_not_found_contact_admin'
    )).not.toBeInTheDocument();
});

test('ignores an older account lookup after the ID changes', async () => {
    const older = deferred();
    const current = deferred();
    api.auth.getSpaceAccount.mockImplementation((_space, accountId) => (
        accountId === 'older' ? older.promise : current.promise
    ));
    api.auth.setSpacePreset.mockResolvedValue({status: 200});
    render(<SpaceAccess />);

    await screen.findAllByText('alice');
    fireEvent.click(screen.getByRole('button', {
        name: 'account.space_access.member.add',
    }));
    const dialog = screen.getByRole('dialog');
    const account = within(dialog).getByLabelText(
        'account.space_access.member.existing_account'
    );
    fireEvent.change(account, {target: {value: 'older'}});
    fireEvent.blur(account);
    fireEvent.change(account, {target: {value: 'current'}});
    fireEvent.blur(account);

    await act(async () => current.resolve({
        status: 200,
        data: {user_id: 'current-id', user_name: 'current'},
    }));
    await act(async () => older.resolve({
        status: 200,
        data: {user_id: 'older-id', user_name: 'older'},
    }));

    const roleSelects = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(roleSelects[roleSelects.length - 1]);
    fireEvent.click(screen.getByText('account.permission_preset.GS_READ_ONLY'));
    fireEvent.click(within(dialog).getByRole('button', {
        name: 'common.action.save',
    }));

    await waitFor(() => expect(api.auth.setSpacePreset).toHaveBeenCalledWith(
        'SPACE_A',
        'current-id',
        'current',
        'GS_READ_ONLY',
        expect.any(Object)
    ));
    expect(api.auth.setSpacePreset).not.toHaveBeenCalledWith(
        'SPACE_A',
        'older-id',
        expect.anything(),
        expect.anything(),
        expect.any(Object)
    );
});

test('requires an explicit replacement for legacy custom access', async () => {
    setResponses({
        members: [{
            user_id: 'alice',
            user_name: 'alice',
            roles: [{role_id: 'custom', role_name: 'analyst'}],
        }],
    });

    render(<SpaceAccess />);

    const row = (await screen.findAllByText('alice'))[0].closest('tr');
    fireEvent.click(within(row).getByRole('button', {
        name: 'common.action.edit',
    }));

    expect(screen.getByRole('alert')).toHaveTextContent(
        'account.space_access.member.custom_title'
    );
    expect(screen.getByRole('button', {
        name: 'account.space_access.member.replace',
    })).toBeInTheDocument();
});
