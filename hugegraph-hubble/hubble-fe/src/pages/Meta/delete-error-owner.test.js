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

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {Modal, message} from 'antd';
import confirmSchemaDeletion from './common/confirmSchemaDeletion';

let items;
let options;
let close;
let update;
let onOk;
beforeEach(() => {
    items = ['a', 'b'];
    close = jest.fn();
    update = jest.fn();
    jest.spyOn(Modal, 'confirm').mockImplementation(value => {
        onOk = value.onOk;
        return {update};
    });
    jest.spyOn(message, 'error').mockImplementation(() => {});
    jest.spyOn(message, 'success').mockImplementation(() => {});
    options = {graphspace: 'DEFAULT', graph: 'audit', names: ['a', 'b'], t: key => key,
        title: 'Delete', warning: null, inUseKey: 'in-use',
        list: jest.fn(async () => ({status: 200, data: {records: items.map(name => ({name})), total: items.length}})),
        checkUsage: jest.fn(async (_space, _graph, {names}) => ({status: 200,
            data: Object.fromEntries(names.map(name => [name, false]))})),
        remove: jest.fn(), onComplete: jest.fn(), onRefresh: jest.fn()};
});
afterEach(() => jest.restoreAllMocks());
const finishAttempt = async () => {
    await waitFor(() => expect(update).toHaveBeenLastCalledWith(expect.objectContaining({
        okButtonProps: {loading: false, disabled: false},
    })));
};

test('lost DELETE response reconciles absence as completion without another DELETE', async () => {
    options.remove.mockImplementation(async () => { items = []; throw new Error('response lost'); });
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(options.remove).toHaveBeenCalledTimes(1);
    expect(options.onRefresh).not.toHaveBeenCalled();
    expect(message.error).not.toHaveBeenCalled();
});

test('partially applied batch retries only remaining names with fresh usage check', async () => {
    options.remove.mockImplementationOnce(async () => {items = ['b']; throw new Error('lost');})
        .mockImplementationOnce(async () => {items = []; return {status: 200};});
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await finishAttempt();
    expect(close).not.toHaveBeenCalled();
    act(() => onOk(close));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(options.checkUsage.mock.calls[1][2]).toEqual({names: ['b']});
    expect(options.remove.mock.calls[1][2]).toEqual({names: ['b'], skip_using: false});
});

test('usage changes after failure block retry before any further deletion', async () => {
    options.remove.mockResolvedValue({status: 500});
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await finishAttempt();
    options.checkUsage.mockResolvedValue({status: 200, data: {a: false, b: true}});
    act(() => onOk(close));
    await waitFor(() => expect(message.error).toHaveBeenLastCalledWith(expect.objectContaining({content: 'in-use'})));
    expect(options.remove).toHaveBeenCalledTimes(1);
    expect(close).not.toHaveBeenCalled();
});

test('a synchronous 200 with skipped targets is not reported as complete', async () => {
    options.remove.mockResolvedValue({status: 200});
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await finishAttempt();
    expect(close).not.toHaveBeenCalled();
    expect(message.success).not.toHaveBeenCalled();
    expect(message.error).toHaveBeenCalledWith(expect.objectContaining({content: 'schema.delete_failed'}));
});

test.each(['accepted', 'lost', 'partial failure'])('async %s deletion checks status until explicit retry, which rechecks usage', async mode => {
    options.asyncDeletion = true;
    if (mode === 'lost') options.remove.mockRejectedValue(new Error('response lost'));
    else options.remove.mockResolvedValue({status: mode === 'accepted' ? 200 : 500});
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await finishAttempt();
    expect(update.mock.calls.at(-1)[0].okText).toBe('schema.delete_check_status');
    act(() => onOk(close));
    await finishAttempt();
    expect(options.remove).toHaveBeenCalledTimes(1);
    expect(message.success).not.toHaveBeenCalled();
    render(update.mock.calls.at(-1)[0].content);
    options.remove.mockImplementationOnce(async () => {items = []; return {status: 200};});
    fireEvent.click(screen.getByRole('button', {name: 'schema.delete_retry'}));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(options.checkUsage).toHaveBeenCalledTimes(2);
    expect(options.remove).toHaveBeenCalledTimes(2);
});

test.each([
    ['failed', {status: 500}],
    ['truncated', {status: 200, data: {records: [], total: 10}}],
    ['missing total', {status: 200, data: {records: []}}],
    ['duplicate names', {status: 200, data: {records: [{name: 'other'}, {name: 'other'}], total: 2}}],
    ['missing name', {status: 200, data: {records: [{}], total: 1}}],
    ['invalid record', {status: 200, data: {records: [null], total: 1}}],
])('untrusted %s listing cannot imply absence or permit writes', async (_mode, response) => {
    options.list.mockResolvedValue(response);
    confirmSchemaDeletion(options);
    act(() => onOk(close));
    await finishAttempt();
    expect(options.remove).not.toHaveBeenCalled();
    expect(close).not.toHaveBeenCalled();
    expect(message.success).not.toHaveBeenCalled();
});
