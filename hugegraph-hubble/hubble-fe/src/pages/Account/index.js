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
    Alert,
    PageHeader,
    Button,
    Dropdown,
    Space,
    Table,
    message,
    Tooltip,
    Modal,
    Tag,
    Tabs,
} from 'antd';
import {MoreOutlined} from '@ant-design/icons';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import TableHeader from '../../components/TableHeader';
import EditLayer from './EditLayer';
import * as api from '../../api';
import {useAuthContext} from '../../auth/AuthContext';
import {
    getAccountPreset,
    getAccountPresetLabelKey,
    getPresetSpaces,
    PERMISSION_PRESETS,
} from './permissionPresets';
import SpaceAccess from './SpaceAccess';
import {accountErrorMessage} from './accountError';

const PAGE_ERROR_CONFIG = {suppressBusinessErrorToast: true};

const RowAction = ({onAction, row, children}) => {
    const handleClick = useCallback(() => onAction(row), [onAction, row]);

    return <Button type='link' onClick={handleClick}>{children}</Button>;
};

const GlobalAccounts = ({
    onAssignMember,
    onPendingCreateHandled,
    pendingCreate,
}) => {
    const {t} = useTranslation();
    const {context} = useAuthContext();
    const accountActions = context?.actions?.accounts ?? [];
    const authorizationActions = context?.actions?.authorizations ?? [];
    const canCreateAccount = accountActions.includes('create');
    const canUpdateAccount = accountActions.includes('update');
    const canDeleteAccount = accountActions.includes('delete');
    const canGrantAuthorization = authorizationActions.includes('grant');
    const permissionPresetsSupported = !context
        || context.capabilities?.includes('account_permission_presets');
    const standalone = context?.mode === 'NON_PD';
    const hasRowMutations = canUpdateAccount || canDeleteAccount || canGrantAuthorization;
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [creationContext, setCreationContext] = useState(null);
    const [op, setOp] = useState('detail');
    const [detail, setDetail] = useState({});
    const [data, setData] = useState([]);
    const [listLoading, setListLoading] = useState(true);
    const [listError, setListError] = useState(false);
    const listRequest = useRef(null);
    const [refresh, setRefresh] = useState(false);
    const [pagination, setPagination] = useState({toatal: 0, current: 1, pageSize: 10});

    const showDetail = useCallback(row => {
        setCreationContext(null);
        setDetail(row);
        setOp('detail');
        setEditLayerVisible(true);
    }, []);

    const showEdit = useCallback(row => {
        setCreationContext(null);
        setDetail(row);
        setOp('edit');
        setEditLayerVisible(true);
    }, []);

    const showMembership = useCallback(row => {
        const graphspaces = getPresetSpaces(row);
        onAssignMember?.({
            user_id: row.id,
            user_name: row.user_name,
            graphspace: graphspaces[0],
            graphspaces,
        });
    }, [onAssignMember]);

    const showAdd = useCallback(() => {
        setCreationContext(null);
        setDetail({});
        setOp('create');
        setEditLayerVisible(true);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(value => !value);
    }, []);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleCreated = useCallback(account => {
        if (creationContext) {
            setCreationContext(null);
            onAssignMember?.({
                ...account,
                graphspaces: creationContext.graphspaces,
                permission_preset: creationContext.permission_preset,
            });
            return;
        }
        if (!canGrantAuthorization || !onAssignMember || account.is_superadmin) {
            return;
        }
        Modal.confirm({
            title: t('account.created.title'),
            content: t('account.created.description', {
                name: account.user_name,
            }),
            okText: t('account.created.assign'),
            cancelText: t('account.created.done'),
            onOk: () => onAssignMember(account),
        });
    }, [canGrantAuthorization, creationContext, onAssignMember, t]);

    useEffect(() => {
        if (!pendingCreate || !canCreateAccount) {
            return;
        }
        setCreationContext(pendingCreate);
        setDetail({user_name: pendingCreate.user_name});
        setOp('create');
        setEditLayerVisible(true);
        onPendingCreateHandled?.();
    }, [canCreateAccount, onPendingCreateHandled, pendingCreate]);

    const handleDelete = useCallback(row => {
        Modal.confirm({
            title: t('account.delete_confirm', {name: row.user_name}),
            onOk: () => {
                return api.auth.delUser(row.id, PAGE_ERROR_CONFIG).then(res => {
                    if (res.status === 200) {
                        message.success(t('common.msg.delete_success'));
                        setRefresh(value => !value);
                        return;
                    }
                    throw res;
                }).catch(error => message.error(accountErrorMessage(
                    error, t('account.feedback.delete_retry')
                )));
            },
        });
    }, [t]);

    const handleTable = useCallback(page => {
        setPagination(value => ({...value, ...page}));
    }, []);

    const columns = [
        {
            title: t('account.col.id'),
            dataIndex: 'user_name',
            width: 150,
        },
        {
            title: t('account.col.name'),
            dataIndex: 'user_nickname',
            width: 150,
        },
        {
            title: t('account.col.remark'),
            dataIndex: 'user_description',
            width: 180,
            ellipsis: {showTitle: false},
            render: val => <Tooltip title={val} placement='bottomLeft'>{val}</Tooltip>,
        },
        {
            title: t('account.col.level'),
            width: 190,
            render: row => {
                const preset = getAccountPreset(row);
                const color = preset === PERMISSION_PRESETS.SUPER_ADMIN ? 'red'
                    : preset === PERMISSION_PRESETS.GS_ADMIN ? 'blue' : 'default';
                const labelKey = standalone
                                 && row.permission_preset
                                 === PERMISSION_PRESETS.GS_READ_WRITE
                    ? PERMISSION_PRESETS.GS_READ_WRITE
                    : getAccountPresetLabelKey(row, permissionPresetsSupported);
                return (
                    <Tag color={color}>
                        {t(`account.permission_preset.${labelKey}`)}
                    </Tag>
                );
            },
        },
        {
            key: 'graphspaces',
            title: t('account.col.resource'),
            width: 120,
            render: row => (
                getAccountPreset(row) === PERMISSION_PRESETS.SUPER_ADMIN
                    ? t('account.col.resource_all')
                    : getPresetSpaces(row).length
            ),
        },
        {
            title: t('account.col.create_time'),
            dataIndex: 'user_create',
            align: 'center',
            width: 200,
        },
        {
            title: t('common.operation'),
            width: hasRowMutations ? 170 : 100,
            align: 'center',
            render: row => {
                const menuItems = [
                    canUpdateAccount && {
                        key: 'edit',
                        label: t('common.action.edit'),
                    },
                    canGrantAuthorization
                    && onAssignMember
                    && getAccountPreset(row) !== PERMISSION_PRESETS.SUPER_ADMIN && {
                        key: 'membership',
                        label: t('account.action.manage_membership'),
                    },
                    canDeleteAccount
                    && row.user_name !== 'admin'
                    && row.user_name !== context?.username && {
                        key: 'delete',
                        danger: true,
                        label: t('common.action.delete'),
                    },
                ].filter(Boolean);
                const handleMenu = ({key}) => {
                    if (key === 'edit') {
                        showEdit(row);
                    }
                    else if (key === 'membership') {
                        showMembership(row);
                    }
                    else if (key === 'delete') {
                        handleDelete(row);
                    }
                };

                return (
                    <Space size={4}>
                        <RowAction onAction={showDetail} row={row}>
                            {t('common.action.detail')}
                        </RowAction>
                        {menuItems.length > 0 && (
                            <Dropdown
                                menu={{items: menuItems, onClick: handleMenu}}
                                trigger={['click']}
                            >
                                <Button
                                    type='link'
                                    aria-label={t('account.action.more')}
                                    icon={<MoreOutlined />}
                                >
                                    {t('account.action.more')}
                                </Button>
                            </Dropdown>
                        )}
                    </Space>
                );
            },
        },
    ].filter(column => !standalone || column.key !== 'graphspaces');

    const rowKey = useCallback(item => item.user_name, []);
    const {current, pageSize} = pagination;

    const loadAccounts = useCallback(async () => {
        const token = Symbol('account-list');
        listRequest.current = token;
        setListLoading(true);
        setListError(false);
        setData([]);
        try {
            const res = await api.auth.getAllUserList({
                query: '',
                page_no: current,
                page_size: pageSize,
            }, PAGE_ERROR_CONFIG);
            if (listRequest.current !== token) {
                return;
            }
            if (res.status === 200) {
                setData(res.data.records);
                setPagination(value => ({...value, total: res.data.total}));
                return;
            }
            setListError(true);
        }
        catch (error) {
            if (listRequest.current === token) {
                setListError(true);
            }
        }
        finally {
            if (listRequest.current === token) {
                setListLoading(false);
            }
        }
    }, [current, pageSize]);

    useEffect(() => {
        loadAccounts();
        return () => {
            listRequest.current = null;
        };
    }, [refresh, loadAccounts]);

    return (
        <>
            {listError && (
                <Alert
                    type='error'
                    showIcon
                    message={t('account.load.unavailable')}
                    action={(
                        <Button size='small' onClick={loadAccounts}>
                            {t('account.load.retry')}
                        </Button>
                    )}
                />
            )}
            <TableHeader>
                <Space>
                    {canCreateAccount && (
                        <Button onClick={showAdd} type='primary'>
                            {t('account.create')}
                        </Button>
                    )}
                </Space>
            </TableHeader>

            <Table
                columns={columns}
                dataSource={data}
                rowKey={rowKey}
                pagination={pagination}
                onChange={handleTable}
                loading={listLoading}
                scroll={{x: 1270}}
            />

            <EditLayer
                visible={editLayerVisible}
                op={op}
                data={detail}
                onCancel={handleHideLayer}
                refresh={handleRefresh}
                onCreated={handleCreated}
                allowedOperations={{
                    create: canCreateAccount,
                    edit: canUpdateAccount,
                }}
            />
        </>
    );
};

const Account = () => {
    const {t} = useTranslation();
    const {context, refresh: refreshPermissions} = useAuthContext();
    const actions = context?.actions ?? {};
    const canReadGlobalAccounts = (actions.accounts ?? []).includes('read');
    const canCreateGlobalAccount = (actions.accounts ?? []).includes('create');
    const canReadScopedAccess = [
        ...(actions.members ?? []),
        ...(actions.roles ?? []),
        ...(actions.authorizations ?? []),
    ].includes('read');
    const [activeTab, setActiveTab] = useState('global');
    const [pendingMember, setPendingMember] = useState(null);
    const [pendingCreate, setPendingCreate] = useState(null);
    const assignMember = useCallback(account => {
        setPendingMember(account);
        setActiveTab('scoped');
    }, []);
    const clearPendingMember = useCallback(() => setPendingMember(null), []);
    const createAccount = useCallback(request => {
        setPendingCreate(request);
        setActiveTab('global');
    }, []);
    const clearPendingCreate = useCallback(() => setPendingCreate(null), []);
    const refreshPermissionContext = useCallback(
        () => Promise.resolve(refreshPermissions?.()).catch(() => undefined),
        [refreshPermissions]
    );

    let content = null;
    if (canReadGlobalAccounts && canReadScopedAccess) {
        content = (
            <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                    {
                        key: 'global',
                        label: t('account.space_access.global_tab'),
                        children: (
                            <GlobalAccounts
                                onAssignMember={assignMember}
                                pendingCreate={pendingCreate}
                                onPendingCreateHandled={clearPendingCreate}
                            />
                        ),
                    },
                    {
                        key: 'scoped',
                        label: t('account.space_access.scoped_tab'),
                        children: (
                            <SpaceAccess
                                pendingAccount={pendingMember}
                                onPendingAccountHandled={clearPendingMember}
                                onCreateAccount={canCreateGlobalAccount
                                    ? createAccount
                                    : undefined}
                            />
                        ),
                    },
                ]}
            />
        );
    }
    else if (canReadGlobalAccounts) {
        content = <GlobalAccounts />;
    }
    else if (canReadScopedAccess) {
        content = <SpaceAccess />;
    }
    else {
        content = (
            <Alert
                type='warning'
                showIcon
                message={t('account.permission_changed')}
                description={t('account.permission_changed_description')}
                action={(
                    <Button
                        size='small'
                        onClick={refreshPermissionContext}
                    >
                        {t('account.refresh_permissions')}
                    </Button>
                )}
            />
        );
    }

    return (
        <>
            <PageHeader ghost={false} onBack={false} title={t('account.title')} />
            <div className='container'>{content}</div>
        </>
    );
};

export default Account;
