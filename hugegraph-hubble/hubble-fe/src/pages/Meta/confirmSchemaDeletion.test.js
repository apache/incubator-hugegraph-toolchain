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

import {act, waitFor} from '@testing-library/react';
import {Modal, message} from 'antd';
import confirmSchemaDeletion from './common/confirmSchemaDeletion';

beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(message, 'error').mockImplementation(() => {});
    jest.spyOn(message, 'success').mockImplementation(() => {});
});

afterEach(() => jest.restoreAllMocks());

test('confirmed deletion still closes when the completion callback throws', async () => {
    const update = jest.fn();
    jest.spyOn(Modal, 'confirm').mockImplementation(() => ({update}));
    const list = jest.fn()
        .mockResolvedValueOnce({status: 200, data: {
            records: [{name: 'audit_label'}], total: 1,
        }})
        .mockResolvedValueOnce({status: 200, data: {records: [], total: 0}});
    const remove = jest.fn().mockResolvedValue({status: 200});
    const close = jest.fn();

    confirmSchemaDeletion({
        graphspace: 'DEFAULT',
        graph: 'audit',
        names: ['audit_label'],
        title: 'Delete schema',
        warning: null,
        list,
        remove,
        onComplete: () => {
            throw new Error('render callback failed');
        },
        onRefresh: jest.fn(),
        t: key => key,
    });

    act(() => Modal.confirm.mock.calls[0][0].onOk(close));

    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(remove).toHaveBeenCalledTimes(1);
    expect(message.success).toHaveBeenCalledTimes(1);
    expect(message.error).not.toHaveBeenCalled();
    expect(update).toHaveBeenCalledWith(expect.objectContaining({
        okButtonProps: {loading: true, disabled: true},
    }));
});
