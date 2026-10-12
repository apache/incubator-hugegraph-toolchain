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

import {useCallback} from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import useMetaTable from './useMetaTable';

const Harness = ({request, identityKey = 'graph-a', refreshKey = ''}) => {
    const fetchPage = useCallback(params => request(params), [request]);
    const {data, loading, error, retry, handleTable, pagination} = useMetaTable(
        fetchPage, {identityKey, refreshKey}
    );
    const goToPageThree = useCallback(() => handleTable({current: 3}), [handleTable]);
    return (
        <>
            <div>{loading ? 'loading' : 'settled'}</div>
            <div>{error ? 'failed' : 'available'}</div>
            <div>{data.map(item => item.name).join(',')}</div>
            <div>page-{pagination.current}</div>
            <button onClick={retry}>retry</button>
            <button onClick={goToPageThree}>page 3</button>
        </>
    );
};

test('separates request failure from empty success and recovers on retry', async () => {
    const request = jest.fn()
        .mockRejectedValueOnce(new Error('offline'))
        .mockResolvedValueOnce({status: 200, data: {records: [], total: 0}});

    render(<Harness request={request} />);

    expect(await screen.findByText('failed')).toBeInTheDocument();
    expect(screen.getByText('settled')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'retry'}));
    await waitFor(() => expect(request).toHaveBeenCalledTimes(2));
    expect(await screen.findByText('available')).toBeInTheDocument();
    expect(await screen.findByText('settled')).toBeInTheDocument();
});

test('ignores a late response from the previous table identity', async () => {
    let resolveOld;
    const oldRequest = jest.fn(() => new Promise(resolve => {
        resolveOld = resolve;
    }));
    const currentRequest = jest.fn().mockResolvedValue({
        status: 200,
        data: {records: [{name: 'current'}], total: 1},
    });
    const {rerender} = render(<Harness request={oldRequest} />);
    rerender(<Harness request={currentRequest} />);

    expect(await screen.findByText('current')).toBeInTheDocument();
    resolveOld({status: 200, data: {records: [{name: 'old'}], total: 1}});
    await waitFor(() => expect(screen.queryByText('old')).not.toBeInTheDocument());
});

test('resets page three to page one when the graph identity changes', async () => {
    const firstRequest = jest.fn().mockResolvedValue({
        status: 200, data: {records: [{name: 'old'}], total: 30},
    });
    const nextRequest = jest.fn().mockResolvedValue({
        status: 200, data: {records: [{name: 'new'}], total: 1},
    });
    const {rerender} = render(
        <Harness request={firstRequest} identityKey='graph-a' />
    );
    await screen.findByText('old');
    fireEvent.click(screen.getByRole('button', {name: 'page 3'}));
    await waitFor(() => expect(firstRequest).toHaveBeenLastCalledWith({page_no: 3}));

    rerender(<Harness request={nextRequest} identityKey='graph-b' />);
    expect(await screen.findByText('new')).toBeInTheDocument();
    expect(nextRequest).toHaveBeenCalledWith({page_no: 1});
    expect(nextRequest).toHaveBeenCalledTimes(1);
    expect(screen.getByText('page-1')).toBeInTheDocument();
});

test('falls back to the last valid page after deleting the final page', async () => {
    const request = jest.fn(params => Promise.resolve(params.page_no === 3
        ? {status: 200, data: {records: [], total: 11}}
        : {status: 200, data: {records: [{name: 'last'}], total: 11}}));
    render(<Harness request={request} />);
    await screen.findByText('last');
    fireEvent.click(screen.getByRole('button', {name: 'page 3'}));

    await waitFor(() => expect(request).toHaveBeenCalledWith({page_no: 2}));
    expect(await screen.findByText('page-2')).toBeInTheDocument();
    expect(await screen.findByText('last')).toBeInTheDocument();
});
