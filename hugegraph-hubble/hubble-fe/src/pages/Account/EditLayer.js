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

import {Alert, Modal, Input, Form, message, Spin, Switch} from 'antd';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import * as api from '../../api';
import * as rules from '../../utils/rules';
import style from './index.module.scss';
import FormHelpLabel from '../../components/FormHelpLabel';
import {accountErrorMessage} from './accountError';
import {useAuthContext} from '../../auth/AuthContext';
import {
    getAccountPresetLabelKey,
    getPresetSpaces,
    PERMISSION_PRESETS,
    toPermissionPayload,
} from './permissionPresets';
import {PAGE_ERROR_CONFIG} from './pagedRecords';

const DEFAULT_ALLOWED_OPERATIONS = {create: true, edit: true};
const toProfilePayload = values => ({
    user_name: values.user_name,
    user_nickname: values.user_nickname,
    user_password: values.user_password,
    user_description: values.user_description,
});
const HelpLabel = ({t, labelKey}) => (
    <FormHelpLabel
        label={t(labelKey)}
        help={t(`${labelKey}_help`)}
    />
);

const EditLayer = ({
    visible,
    onCancel,
    data,
    op,
    refresh,
    onCreated,
    allowedOperations = DEFAULT_ALLOWED_OPERATIONS,
}) => {
    const {t} = useTranslation();
    const {context} = useAuthContext();
    const [form] = Form.useForm();
    const [detail, setDetail] = useState({});
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [mutationError, setMutationError] = useState(null);
    const submitPending = useRef(false);
    const detailRequest = useRef(0);
    const permissionPresetsSupported = !context
        || context.capabilities?.includes('account_permission_presets');
    const standalone = context?.mode === 'NON_PD';

    const title = {
        'detail': t('account.form.title_detail'),
        'edit': t('account.form.title_edit'),
        'create': t('account.form.title_create'),
    };

    const createUser = useCallback(values => {
        const profile = toProfilePayload(values);
        const payload = values.is_superadmin
            ? toPermissionPayload({
                ...profile,
                permission_preset: PERMISSION_PRESETS.SUPER_ADMIN,
            })
            : profile;
        return api.auth.addUser(payload, PAGE_ERROR_CONFIG).then(res => {
            if (res.status === 200) {
                message.success(t('common.msg.create_success'));
                onCancel();
                refresh();
                onCreated?.({
                    user_id: res.data?.id,
                    user_name: values.user_name,
                    is_superadmin: Boolean(values.is_superadmin),
                });
                return;
            }
            throw res;
        });
    }, [onCancel, onCreated, refresh, t]);
    const updateUser = useCallback(async values => {
        const profile = toProfilePayload(values);
        const superAdminChanged = permissionPresetsSupported
            && Boolean(values.is_superadmin) !== Boolean(detail.is_superadmin);
        const payload = superAdminChanged
            ? toPermissionPayload({
                ...profile,
                permission_preset: values.is_superadmin
                    ? PERMISSION_PRESETS.SUPER_ADMIN
                    : PERMISSION_PRESETS.GS_READ_ONLY,
            })
            : profile;
        const requestUpdate = async update => {
            const response = await api.auth.updateUser(
                data.id, update, PAGE_ERROR_CONFIG
            );
            if (response.status !== 200) {
                throw response;
            }
        };
        if (superAdminChanged && profile.user_password) {
            throw new Error(t('account.feedback.password_permission_separate'));
        }
        await requestUpdate(payload);
        message.success(t('common.msg.update_success'));
        onCancel();
        refresh();
    }, [
        data.id,
        detail.is_superadmin,
        onCancel,
        permissionPresetsSupported,
        refresh,
        t,
    ]);

    const onFinish = useCallback(async () => {
        if (submitPending.current || loading) {
            return;
        }

        submitPending.current = true;
        setSubmitting(true);
        setMutationError(null);
        try {
            const values = await form.validateFields();
            if (op === 'create') {
                await createUser(values);
            }

            if (op === 'edit') {
                await updateUser(values);
            }

        }
        catch (error) {
            if (!error || !error.errorFields) {
                const detail = accountErrorMessage(
                    error, t('account.feedback.save_retry')
                );
                setMutationError(detail);
                message.error(detail);
            }
        }
        finally {
            submitPending.current = false;
            setSubmitting(false);
        }
    }, [createUser, form, loading, op, t, updateUser]);

    useEffect(() => {
        if (!visible) {
            detailRequest.current += 1;
            setDetail({});
            setMutationError(null);
            form.resetFields();
            setLoading(false);
            return;
        }

        const request = detailRequest.current + 1;
        detailRequest.current = request;
        if (data.id) {
            setLoading(true);
            setDetail({});
            form.resetFields();
            api.auth.getUserInfo(data.id, PAGE_ERROR_CONFIG).then(res => {
                if (detailRequest.current !== request) {
                    return;
                }

                if (res.status === 200) {
                    if (op !== 'detail') {
                        form.setFieldsValue(res.data);
                    }
                    setDetail(res.data);
                    return;
                }

                form.resetFields();
                setDetail({});
                message.error(t('common.msg.load_failed'));
            }).catch(() => {
                if (detailRequest.current !== request) {
                    return;
                }

                form.resetFields();
                setDetail({});
                message.error(t('common.msg.load_failed'));
            }).finally(() => {
                if (detailRequest.current === request) {
                    setLoading(false);
                }
            });
        }
        else {
            setDetail({});
            form.resetFields();
            form.setFieldsValue({user_name: data.user_name});
            setLoading(false);
        }
    }, [visible, data.id, data.user_name, form, op, t]);

    if (op !== 'detail' && !allowedOperations[op]) {
        return null;
    }

    return (
        op === 'detail'
            ? (
                <Modal
                    title={t('account.form.title_detail')}
                    onCancel={onCancel}
                    open={visible}
                    footer={null}
                    width={600}
                    maskClosable={false}
                >
                    <Spin spinning={loading}>
                        <Form
                            labelCol={{span: 6}}
                            form={form}
                            preserve={false}
                        >
                            <Form.Item label={t('account.form.id')} className={style.item}>
                                {detail.user_name}
                            </Form.Item>
                            <Form.Item label={t('account.form.name')} className={style.item}>
                                {detail.user_nickname}
                            </Form.Item>
                            <Form.Item
                                label={t('account.form.permission_preset')}
                                className={style.item}
                            >
                                {t(`account.permission_preset.${
                                    standalone
                                    && detail.permission_preset
                                    === PERMISSION_PRESETS.GS_READ_WRITE
                                        ? PERMISSION_PRESETS.GS_READ_WRITE
                                        : getAccountPresetLabelKey(
                                            detail,
                                            permissionPresetsSupported
                                        )
                                }`)}
                            </Form.Item>
                            <Form.Item label={t('account.form.remark')} className={style.item}>
                                {detail.user_description}
                            </Form.Item>
                            {!standalone && (
                                <Form.Item
                                    label={t('account.form.graphspaces')}
                                    className={style.item}
                                >
                                    {getPresetSpaces(detail).join(', ')}
                                </Form.Item>
                            )}
                            <Form.Item label={t('account.col.create_time')} className={style.item}>
                                {detail.user_create}
                            </Form.Item>
                        </Form>
                    </Spin>
                </Modal>
            )
            : (
                <Modal
                    title={title[op] ?? t('account.form.title_create')}
                    onCancel={onCancel}
                    open={visible}
                    onOk={onFinish}
                    confirmLoading={submitting}
                    okButtonProps={{disabled: loading}}
                    width={600}
                >
                    <Spin spinning={loading}>
                        {mutationError && (
                            <Alert
                                type='error'
                                showIcon
                                message={t('account.feedback.save_failed')}
                                description={mutationError}
                            />
                        )}
                        <Form
                            labelCol={{span: 6}}
                            // initialValues={data}
                            form={form}
                            preserve={false}
                        >
                            {(op === 'create' || op === 'edit') && (
                                <>
                                    <Form.Item
                                        label={<HelpLabel t={t} labelKey='account.form.id' />}
                                        name="user_name"
                                        validateFirst
                                        rules={[{type: 'string', min: 5, max: 16}, rules.isName, rules.required()]}
                                    >
                                        <Input
                                            placeholder={t('account.form.id_placeholder')}
                                            disabled={op === 'edit'}
                                        />
                                    </Form.Item>
                                    <Form.Item
                                        label={<HelpLabel t={t} labelKey='account.form.name' />}
                                        name="user_nickname"
                                        rules={[rules.isAccountName]}
                                        validateFirst
                                    >
                                        <Input placeholder={t('account.form.name_placeholder')} />
                                    </Form.Item>
                                    <Form.Item
                                        label={<HelpLabel t={t} labelKey='account.form.default_password' />}
                                        name="user_password"
                                        rules={op === 'create'
                                            ? [rules.required(), {type: 'string', min: 5, max: 16}]
                                            : [{type: 'string', min: 5, max: 16}]}
                                    >
                                        <Input.Password
                                            placeholder={t('account.form.default_password_placeholder')}
                                            autoComplete="new-password"
                                        />
                                    </Form.Item>
                                    <Form.Item
                                        label={<HelpLabel t={t} labelKey='account.form.remark' />}
                                        name="user_description"
                                    >
                                        <Input placeholder={t('account.form.remark_placeholder')} />
                                    </Form.Item>
                                    {permissionPresetsSupported && (
                                        <Form.Item
                                            label={(
                                                <HelpLabel
                                                    t={t}
                                                    labelKey='account.form.is_superadmin'
                                                />
                                            )}
                                            name="is_superadmin"
                                            valuePropName="checked"
                                            initialValue={false}
                                        >
                                            <Switch />
                                        </Form.Item>
                                    )}
                                </>
                            )}
                        </Form>
                    </Spin>
                </Modal>
            )
    );
};

export default EditLayer;
