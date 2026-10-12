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
    Alert,
    Button,
    Form,
    Input,
    message,
    Modal,
    Select,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import * as api from '../../api';
import TableHeader from '../../components/TableHeader';
import {useAuthContext} from '../../auth/AuthContext';
import {PERMISSION_PRESETS} from './permissionPresets';
import {loadAllPages, PAGE_ERROR_CONFIG} from './pagedRecords';

const responseRecords = response => response?.data?.records ?? [];
const errorDetail = value => {
    const response = value?.response ?? value;
    return response?.data?.message ?? response?.message;
};
const isMissingAccount = (value, t) => {
    const detail = errorDetail(value);
    if (typeof detail !== 'string') {
        return false;
    }
    const normalized = detail.toLowerCase();
    const subject = t('account.space_access.member.missing_response_subject')
        .toLowerCase();
    const predicate = t('account.space_access.member.missing_response_predicate')
        .toLowerCase();
    return normalized.includes('user or group is not exist')
        || normalized.includes('account does not exist')
        || (normalized.includes(subject) && normalized.includes(predicate));
};
const showMutationError = (error, t) => {
    const detail = errorDetail(error);
    if (isMissingAccount(error, t)) {
        message.error(t('account.space_access.member.account_not_found'));
        return;
    }
    message.error(detail
        ? `${t('common.msg.operation_failed')} (${detail})`
        : t('common.msg.operation_failed'));
};
const adminRole = {
    role_id: PERMISSION_PRESETS.GS_ADMIN,
    permission_preset: PERMISSION_PRESETS.GS_ADMIN,
};

const mergeMembersAndAdmins = (members, admins) => {
    const rows = new Map(members.map(member => [
        member.user_id,
        {...member, member_roles: member.roles ?? []},
    ]));
    admins.forEach(admin => {
        const userId = admin.id ?? admin.user_id;
        const existing = rows.get(userId) ?? {};
        rows.set(userId, {
            ...existing,
            user_id: userId,
            user_name: admin.name ?? admin.user_name ?? existing.user_name,
            member_roles: existing.roles ?? existing.member_roles ?? [],
            roles: [...(existing.roles ?? existing.member_roles ?? []), adminRole],
            is_space_admin: true,
        });
    });
    return Array.from(rows.values());
};

const rolePreset = role => {
    const explicit = role?.permission_preset ?? role?.permissionPreset;
    return Object.values(PERMISSION_PRESETS).includes(explicit)
        ? explicit
        : null;
};

const rolesPreset = roles => {
    const values = roles ?? [];
    const presets = values.map(rolePreset);
    if (presets.some(preset => preset === null)) {
        return null;
    }
    if (values.some(role => (role?.permission_preset ?? role?.permissionPreset) === PERMISSION_PRESETS.GS_ADMIN)) {
        return PERMISSION_PRESETS.GS_ADMIN;
    }
    return values.length > 0 && presets.every(Boolean) && new Set(presets).size === 1 ? presets[0] : null;
};

const roleLabel = (role, t) => {
    const preset = rolePreset(role);
    if (preset) {
        return t(`account.permission_preset.${preset}`);
    }
    const name = role?.role_name ?? role?.name;
    const legacy = t('account.permission_preset.legacy_custom');
    return name ? `${name} · ${legacy}` : legacy;
};

const RowAction = ({row, onAction, children}) => {
    const handleClick = useCallback(() => onAction(row), [onAction, row]);

    return <Button type="link" onClick={handleClick}>{children}</Button>;
};

const useScopedResource = (graphSpace, contextVersion, loader, selector) => {
    const request = useRef(null);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(false);
    const [revision, setRevision] = useState(0);

    const retry = useCallback(() => setRevision(value => value + 1), []);

    useEffect(() => {
        if (!graphSpace) {
            request.current = null;
            setData([]);
            setLoading(false);
            setError(false);
            return undefined;
        }
        const token = Symbol(graphSpace);
        request.current = token;
        setData([]);
        setLoading(true);
        setError(false);
        loader(graphSpace).then(response => {
            if (request.current !== token) {
                return;
            }
            if (response?.status !== 200) {
                setError(true);
                return;
            }
            setData(selector(response));
        }).catch(() => {
            if (request.current === token) {
                setError(true);
            }
        }).finally(() => {
            if (request.current === token) {
                setLoading(false);
            }
        });
        return () => {
            if (request.current === token) {
                request.current = null;
            }
        };
    }, [contextVersion, graphSpace, loader, revision, selector]);

    return {data, loading, error, retry};
};

const ErrorAlert = ({error, retry, t}) => (error ? (
    <Alert
        type="error"
        showIcon
        message={t('account.space_access.load_error')}
        action={(
            <Button size="small" onClick={retry}>
                {t('common.action.retry')}
            </Button>
        )}
    />
) : null);

const SpaceAccess = ({
    onCreateAccount,
    onPendingAccountHandled,
    pendingAccount,
}) => {
    const {t} = useTranslation();
    const {context} = useAuthContext();
    const contextVersion = context?.context_version;
    const scopes = context?.scopes ?? {};
    const memberActions = context?.actions?.members ?? [];
    const canAddMember = memberActions.includes('add');
    const canRemoveMember = memberActions.includes('remove');
    const [selectedSpace, setSelectedSpace] = useState('');
    const [allSpaces, setAllSpaces] = useState([]);
    const [spacesLoading, setSpacesLoading] = useState(false);
    const [spacesError, setSpacesError] = useState(false);
    const [spacesRevision, setSpacesRevision] = useState(0);
    const spacesRequest = useRef(null);
    const accountValidation = useRef(null);
    const [memberDialog, setMemberDialog] = useState(null);
    const [missingAccountId, setMissingAccountId] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [memberForm] = Form.useForm();
    const accountNotFoundMessage = useCallback(() => t(
        onCreateAccount
            ? 'account.space_access.member.account_not_found'
            : 'account.space_access.member.account_not_found_contact_admin'
    ), [onCreateAccount, t]);

    const scopedSpaces = useMemo(
        () => scopes.admin_graphspaces ?? [],
        [scopes.admin_graphspaces]
    );

    useEffect(() => {
        if (!scopes.all_graphspaces) {
            spacesRequest.current = null;
            setAllSpaces([]);
            setSpacesLoading(false);
            setSpacesError(false);
            return undefined;
        }
        const token = Symbol('graphspaces');
        spacesRequest.current = token;
        setSpacesLoading(true);
        setSpacesError(false);
        loadAllPages(api.manage.getGraphSpaceList, {params: {query: ''}})
            .then(response => {
                if (spacesRequest.current !== token) {
                    return;
                }
                if (response?.status !== 200) {
                    setSpacesError(true);
                    return;
                }
                setAllSpaces(responseRecords(response).map(space => space.name));
            })
            .catch(() => {
                if (spacesRequest.current === token) {
                    setSpacesError(true);
                }
            })
            .finally(() => {
                if (spacesRequest.current === token) {
                    setSpacesLoading(false);
                }
            });
        return () => {
            if (spacesRequest.current === token) {
                spacesRequest.current = null;
            }
        };
    }, [contextVersion, scopes.all_graphspaces, spacesRevision]);

    const spaces = scopes.all_graphspaces ? allSpaces : scopedSpaces;
    const graphSpace = spaces.includes(selectedSpace) ? selectedSpace : spaces[0];
    const loadMembers = useCallback(space => loadAllPages(
        (params, config) => api.auth.getSpaceMembers(space, params, config),
        {params: {query: ''}}
    ), []);
    const loadAdmins = useCallback(space => loadAllPages(
        (params, config) => api.auth.getSpaceAdmins(space, params, config),
        {params: {query: ''}}
    ), []);
    const members = useScopedResource(
        graphSpace, contextVersion, loadMembers, responseRecords
    );
    const admins = useScopedResource(graphSpace, contextVersion, loadAdmins, responseRecords
    );
    const visibleMembers = {
        data: mergeMembersAndAdmins(members.data, admins.data),
        loading: members.loading || admins.loading,
        error: members.error || admins.error,
        retry: () => {
            members.retry();
            admins.retry();
        },
    };

    const refreshAll = useCallback(() => {
        members.retry();
        admins.retry();
    }, [admins, members]);

    const runMutation = useCallback(async (operation, close) => {
        if (submitting) {
            return;
        }
        setSubmitting(true);
        try {
            const response = await operation();
            if (response?.status !== 200) {
                showMutationError(response, t);
                return;
            }
            message.success(t('common.msg.success'));
            close();
            refreshAll();
        }
        catch (error) {
            showMutationError(error, t);
        }
        finally {
            setSubmitting(false);
        }
    }, [refreshAll, submitting, t]);

    const openMember = useCallback((row, graphSpaceSelectable = false) => {
        const memberGraphSpace = graphSpaceSelectable
            && spaces.includes(row?.graphspace)
            ? row.graphspace
            : graphSpace;
        const requestedSpaces = graphSpaceSelectable
            && Array.isArray(row?.graphspaces)
            ? row.graphspaces.filter(space => spaces.includes(space))
            : [memberGraphSpace].filter(Boolean);
        memberForm.setFieldsValue({
            user_id: row?.user_id,
            account_id: row?.user_name,
            permission_preset: row?.permission_preset
                ?? rolesPreset(row?.roles),
            graphspace: graphSpaceSelectable
                ? requestedSpaces
                : memberGraphSpace,
        });
        setMissingAccountId(null);
        setMemberDialog({
            ...(row ?? {}),
            graphSpaceSelectable,
        });
    }, [graphSpace, memberForm, spaces]);
    useEffect(() => {
        if (!pendingAccount || !graphSpace) {
            return;
        }
        openMember(pendingAccount, true);
        onPendingAccountHandled?.();
    }, [
        graphSpace,
        onPendingAccountHandled,
        openMember,
        pendingAccount,
        spaces,
    ]);
    const closeMember = useCallback(() => {
        accountValidation.current = null;
        setMemberDialog(null);
        setMissingAccountId(null);
        memberForm.resetFields();
    }, [memberForm]);
    const validateExistingAccount = useCallback(async (_, value) => {
        const validation = Symbol('account-validation');
        accountValidation.current = validation;
        setMissingAccountId(null);
        memberForm.setFieldValue('user_id', undefined);
        if (!value) {
            return;
        }
        const graphSpaceValue = memberForm.getFieldValue('graphspace');
        const targetSpace = Array.isArray(graphSpaceValue)
            ? graphSpaceValue[0]
            : graphSpaceValue;
        if (!targetSpace) {
            return;
        }
        const isCurrent = () => accountValidation.current === validation
            && memberForm.getFieldValue('account_id') === value;
        let response;
        try {
            response = await api.auth.getSpaceAccount(
                targetSpace, value, PAGE_ERROR_CONFIG
            );
        }
        catch (error) {
            if (!isCurrent()) {
                return;
            }
            if (isMissingAccount(error, t)) {
                setMissingAccountId(value);
                throw new Error(accountNotFoundMessage());
            }
            throw new Error(
                t('account.space_access.member.account_check_failed')
            );
        }
        if (!isCurrent()) {
            return;
        }
        const account = response?.data;
        if (response?.status !== 200) {
            if (isMissingAccount(response, t)) {
                setMissingAccountId(value);
                throw new Error(accountNotFoundMessage());
            }
            throw new Error(
                t('account.space_access.member.account_check_failed')
            );
        }
        if (!(account?.user_id ?? account?.id)) {
            setMissingAccountId(value);
            throw new Error(accountNotFoundMessage());
        }
        memberForm.setFieldValue('user_id', account.user_id ?? account.id);
    }, [accountNotFoundMessage, memberForm, t]);
    const handleAccountIdChange = useCallback(() => {
        accountValidation.current = null;
        setMissingAccountId(null);
        memberForm.setFieldValue('user_id', undefined);
    }, [memberForm]);
    const startAccountCreation = useCallback(() => {
        if (!missingAccountId || !onCreateAccount) {
            return;
        }
        const graphSpaceValue = memberForm.getFieldValue('graphspace');
        const graphspaces = Array.isArray(graphSpaceValue)
            ? graphSpaceValue
            : [graphSpaceValue].filter(Boolean);
        const request = {
            graphspaces,
            permission_preset: memberForm.getFieldValue('permission_preset'),
            user_name: missingAccountId,
        };
        closeMember();
        onCreateAccount(request);
    }, [closeMember, memberForm, missingAccountId, onCreateAccount]);
    const submitMember = useCallback(values => {
        const graphSpaces = Array.isArray(values.graphspace)
            ? values.graphspace
            : [values.graphspace];
        runMutation(
            async () => {
                const results = await Promise.allSettled(
                    graphSpaces.map(space => api.auth.setSpacePreset(
                        space,
                        values.user_id ?? values.account_id,
                        values.account_id,
                        values.permission_preset,
                        PAGE_ERROR_CONFIG
                    ))
                );
                const failedSpaces = graphSpaces.filter((_, index) => {
                    const result = results[index];
                    return result.status === 'rejected'
                           || result.value?.status !== 200;
                });
                if (failedSpaces.length > 0) {
                    refreshAll();
                    const options = {
                        success: graphSpaces.length - failedSpaces.length,
                        total: graphSpaces.length,
                        spaces: failedSpaces.join(', '),
                    };
                    throw new Error(t('account.space_access.member.batch_failed', options));
                }
                return {status: 200};
            },
            closeMember
        );
    }, [closeMember, refreshAll, runMutation, t]);

    const confirmDelete = useCallback((title, operation) => {
        Modal.confirm({
            title,
            onOk: () => runMutation(operation, () => undefined),
        });
    }, [runMutation]);

    const editMember = useCallback(row => openMember(row), [openMember]);
    const deleteMember = useCallback(row => confirmDelete(
        t('account.space_access.member.remove_confirm'),
        () => api.auth.deleteSpaceMember(
            graphSpace, row.user_id, PAGE_ERROR_CONFIG
        )
    ), [confirmDelete, graphSpace, t]);
    const addMember = useCallback(() => openMember(), [openMember]);
    const canManageMember = row => scopes.all_graphspaces
        || !row.is_space_admin;
    const retrySpaces = useCallback(
        () => setSpacesRevision(value => value + 1), []
    );
    const submitMemberForm = useCallback(() => memberForm.submit(), [memberForm]);
    const replacingCustomAccess = Boolean(
        memberDialog?.user_id
        && (memberDialog.roles ?? []).length > 0
        && rolesPreset(memberDialog.roles) === null
    );

    const memberColumns = [
        {title: t('account.space_access.member.id'), dataIndex: 'user_name'},
        {
            title: t('account.space_access.member.roles'),
            dataIndex: 'roles',
            render: value => value?.map(role => (
                <Tag key={role.role_id}>{roleLabel(role, t)}</Tag>
            )),
        },
        ...((canAddMember || canRemoveMember) ? [{
            title: t('common.operation'),
            render: row => (
                <Space>
                    {canAddMember && canManageMember(row) && (
                        <RowAction row={row} onAction={editMember}>
                            {t('common.action.edit')}
                        </RowAction>
                    )}
                    {canRemoveMember && canManageMember(row) && (
                        <RowAction row={row} onAction={deleteMember}>
                            {t('common.action.delete')}
                        </RowAction>
                    )}
                </Space>
            ),
        }] : []),
    ];

    const table = (resource, columns, rowKey, addLabel, onAdd, canAdd) => (
        <>
            <ErrorAlert error={resource.error} retry={resource.retry} t={t} />
            <TableHeader>
                {canAdd && (
                    <Button type="primary" onClick={onAdd}>{addLabel}</Button>
                )}
            </TableHeader>
            <Table
                columns={columns}
                dataSource={resource.data}
                rowKey={rowKey}
                loading={resource.loading}
                pagination={{pageSize: 10, showSizeChanger: false}}
            />
        </>
    );

    if (spacesError) {
        return (
            <ErrorAlert
                error
                retry={retrySpaces}
                t={t}
            />
        );
    }

    if (!spacesLoading && spaces.length === 0) {
        return (
            <Alert
                type="info"
                showIcon
                message={t('account.space_access.no_spaces')}
            />
        );
    }

    return (
        <>
            <Typography.Paragraph type="secondary">
                {t('account.space_access.description')}
            </Typography.Paragraph>
            <Space align="center" wrap>
                <Typography.Text strong>
                    {t('account.space_access.graphspace')}
                </Typography.Text>
                <Select
                    aria-label={t('account.space_access.graphspace')}
                    value={graphSpace}
                    onChange={setSelectedSpace}
                    loading={spacesLoading}
                    options={spaces.map(space => ({label: space, value: space}))}
                    style={{minWidth: 240}}
                />
            </Space>
            {table(
                visibleMembers, memberColumns, 'user_id',
                t('account.space_access.member.add'),
                addMember, canAddMember
            )}

            <Modal
                open={memberDialog !== null}
                title={t('account.space_access.member.dialog')}
                onCancel={closeMember}
                onOk={submitMemberForm}
                confirmLoading={submitting}
                okText={replacingCustomAccess
                    ? t('account.space_access.member.replace')
                    : t('common.action.save')}
                destroyOnClose
            >
                {replacingCustomAccess && (
                    <Alert
                        type="warning"
                        showIcon
                        message={t('account.space_access.member.custom_title')}
                        description={t(
                            'account.space_access.member.custom_description'
                        )}
                    />
                )}
                <Form form={memberForm} layout="vertical" onFinish={submitMember}>
                    <Form.Item
                        name="graphspace"
                        label={t('account.space_access.graphspace')}
                        rules={[{required: true}]}
                    >
                        <Select
                            aria-label={t('account.space_access.graphspace')}
                            disabled={!memberDialog?.graphSpaceSelectable}
                            mode={memberDialog?.graphSpaceSelectable
                                ? 'multiple'
                                : undefined}
                            options={spaces.map(space => ({
                                label: space,
                                value: space,
                            }))}
                        />
                    </Form.Item>
                    {memberDialog?.user_id ? (
                        <>
                            <Form.Item
                                name="account_id"
                                label={t('account.space_access.member.id')}
                                rules={[{required: true}]}
                            >
                                <Input disabled />
                            </Form.Item>
                            <Form.Item name="user_id" hidden>
                                <Input onChange={handleAccountIdChange} />
                            </Form.Item>
                        </>
                    ) : (
                        <>
                            <Form.Item
                                name="account_id"
                                label={t(
                                    'account.space_access.member.existing_account'
                                )}
                                dependencies={['graphspace']}
                                extra={missingAccountId && onCreateAccount ? (
                                    <Button
                                        type="link"
                                        size="small"
                                        onClick={startAccountCreation}
                                    >
                                        {t(
                                            'account.space_access.member.create_account'
                                        )}
                                    </Button>
                                ) : null}
                                validateTrigger="onBlur"
                                rules={[
                                    {required: true},
                                    {validator: validateExistingAccount},
                                ]}
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item name="user_id" hidden>
                                <Input />
                            </Form.Item>
                        </>
                    )}
                    <Form.Item
                        name="permission_preset"
                        label={t('account.space_access.member.roles')}
                        rules={[{required: true}]}
                    >
                        <Select
                            options={Object.values(PERMISSION_PRESETS)
                                .filter(value => value !== PERMISSION_PRESETS.SUPER_ADMIN)
                                .filter(value => scopes.all_graphspaces
                                                 || value !== PERMISSION_PRESETS.GS_ADMIN)
                                .map(value => ({
                                    value,
                                    label: t(`account.permission_preset.${value}`),
                                }))}
                        />
                    </Form.Item>
                </Form>
            </Modal>

        </>
    );
};

export {loadAllPages, rolesPreset};
export default SpaceAccess;
