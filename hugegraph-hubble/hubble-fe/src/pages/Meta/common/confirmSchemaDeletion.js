/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Button, Modal, message} from 'antd';

export const DELETE_REQUEST_CONFIG = {suppressBusinessErrorToast: true};

/**
 * Reconcile deletion against complete, fresh schema snapshots before retrying.
 * Only verified absence completes the operation; async retries remain explicit.
 */
export default function confirmSchemaDeletion({graphspace, graph, names, title, warning,
    list, checkUsage, remove, inUseKey, asyncDeletion = false, onComplete, onRefresh, t}) {
    const feedbackKey = `schema-delete:${graphspace}:${graph}:${names.join(',')}`;
    const showError = content => message.error({key: feedbackKey, content});
    let deleting = false;
    let submitted = false;
    let finished = false;
    let pendingNames = names;
    const remainingNames = async () => {
        const response = await list(graphspace, graph, {page_size: -1}, DELETE_REQUEST_CONFIG);
        const page = response?.data;
        if (response?.status !== 200 || !Array.isArray(page?.records)
            || !Number.isFinite(page.total) || page.total !== page.records.length) {
            throw new Error('Schema list is incomplete');
        }
        const recordNames = page.records.map(item => item?.name);
        const existing = new Set(recordNames);
        if (recordNames.some(name => typeof name !== 'string' || !name) || existing.size !== page.total) {
            throw new Error('Schema list identities are incomplete');
        }
        return names.filter(name => existing.has(name));
    };
    const content = (remaining, close) => {
        const remainingLabel = remaining?.join(', ');
        return <div style={{overflowWrap: 'anywhere'}}>
            <div><code>{graphspace}/{graph}</code></div>
            <div><code>{names.join(', ')}</code></div>
            {warning}
            {remaining && <>
                <p>{t('schema.delete_pending', {names: remainingLabel})}</p>
                <Button type='link' danger disabled={deleting}
                    onClick={() => execute(close, true)}>{t('schema.delete_retry')}</Button>
            </>}
        </div>;
    };
    const complete = close => {
        finished = true;
        try {
            onComplete();
        }
        catch {
            // Deletion is already confirmed; a UI callback must not strand the modal.
        }
        message.success({key: feedbackKey, content: t('common.msg.delete_success')});
        close();
    };
    // Static Modal.confirm rethrows rejected onOk promises. Own completion and
    // error feedback explicitly instead of returning a rejecting promise to it.
    const execute = (close, retry = false) => {
        if (deleting || finished) return;
        deleting = true;
        confirmation.update({okButtonProps: {loading: true, disabled: true},
            cancelButtonProps: {disabled: true}, keyboard: false,
            ...(submitted ? {content: content(pendingNames, close)} : {})});
        void (async () => {
            let remaining = await remainingNames();
            pendingNames = remaining;
            if (!remaining.length) return complete(close);
            if (submitted && !retry) {
                confirmation.update({content: content(remaining, close)});
                return;
            }
            if (checkUsage) {
                const usage = await checkUsage(graphspace, graph, {names: remaining}, DELETE_REQUEST_CONFIG);
                if (usage?.status !== 200 || remaining.some(name => typeof usage.data?.[name] !== 'boolean')) {
                    throw new Error('Schema usage is unavailable');
                }
                const inUse = remaining.filter(name => usage.data[name]);
                if (inUse.length) {
                    showError(t(inUseKey, {names: inUse.join(', ')}));
                    return;
                }
            }
            // Async tasks may be accepted before any response, including an error.
            submitted = asyncDeletion;
            let response;
            try {
                response = await remove(graphspace, graph,
                    {names: remaining, skip_using: false}, DELETE_REQUEST_CONFIG);
            }
            catch {
                // Reconcile even transport errors: the server may have applied them.
            }
            remaining = await remainingNames();
            pendingNames = remaining;
            if (!remaining.length) return complete(close);
            onRefresh();
            if (submitted) {
                if (response?.status !== 200) showError(t('schema.delete_failed'));
                confirmation.update({okText: t('schema.delete_check_status'),
                    content: content(remaining, close)});
            }
            else {
                showError(t('schema.delete_failed'));
            }
        })().catch(() => {
            showError(t('schema.delete_failed'));
        }).finally(() => {
            deleting = false;
            if (finished) return;
            confirmation.update({okButtonProps: {loading: false, disabled: false},
                cancelButtonProps: {disabled: false}, keyboard: true,
                okText: submitted ? t('schema.delete_check_status') : undefined,
                content: content(submitted ? pendingNames : null, close)});
        });
    };
    const confirmation = Modal.confirm({title, content: content(), onOk: close => execute(close)});
    return confirmation;
}
