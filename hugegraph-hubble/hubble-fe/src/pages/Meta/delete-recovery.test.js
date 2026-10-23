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
import PropertyTable from './Property';
import VertexTable from './Vertex';
import EdgeTable from './Edge';
import * as api from '../../api';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('react-router-dom', () => ({
    useParams: () => ({graphspace: 'DEFAULT', graph: 'audit'}),
}));
jest.mock('./Property/EditLayer', () => ({EditPropertyLayer: () => null}));
jest.mock('./Vertex/EditLayer', () => ({EditVertexLayer: () => null}));
jest.mock('./Edge/EditLayer', () => ({EditEdgeLayer: () => null}));
jest.mock('./common/useMetaTable', () => () => ({
    data: [{name: 'audit_label', data_type: 'TEXT', cardinality: 'SINGLE',
        properties: [], primary_keys: [], property_indexes: [], sort_keys: []}],
    pagination: false,
}));
jest.mock('../../api', () => ({manage: {
    checkMetaProperty: jest.fn(), checkMetaVertex: jest.fn(),
    delMetaProperty: jest.fn(), delMetaVertex: jest.fn(), delMetaEdge: jest.fn(),
    getMetaPropertyList: jest.fn(), getMetaVertexList: jest.fn(), getMetaEdgeList: jest.fn(),
}}));

let existing;
beforeEach(() => {
    existing = {Property: [{name: 'audit_label'}], Vertex: [{name: 'audit_label'}], Edge: [{name: 'audit_label'}]};
    jest.clearAllMocks();
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
    }));
    api.manage.checkMetaProperty.mockResolvedValue({status: 200, data: {audit_label: false}});
    api.manage.checkMetaVertex.mockResolvedValue({status: 200, data: {audit_label: false}});
    for (const type of ['Property', 'Vertex', 'Edge']) {
        api.manage[`getMeta${type}List`].mockImplementation(async () => ({status: 200,
            data: {records: existing[type], total: existing[type].length}}));
    }
    jest.spyOn(Modal, 'confirm').mockImplementation(() => ({update: jest.fn()}));
    jest.spyOn(message, 'error').mockImplementation(() => {});
    jest.spyOn(message, 'success').mockImplementation(() => {});
});
afterEach(() => jest.restoreAllMocks());

const tables = [
    ['property', PropertyTable, 'delMetaProperty'],
    ['vertex', VertexTable, 'delMetaVertex'],
    ['edge', EdgeTable, 'delMetaEdge'],
];

test.each(tables)('%s deletion keeps confirmation retryable on business and transport failure',
    async (_name, Table, operation) => {
        api.manage[operation]
            .mockResolvedValueOnce({status: 500})
            .mockRejectedValueOnce(new Error('network unavailable'))
            .mockImplementationOnce(async () => {
                existing[operation.replace('delMeta', '')] = [];
                return {status: 200};
            });
        await act(async () => { render(<Table />); });
        fireEvent.click(screen.getByRole('button', {name: /^common\.(?:action\.)?delete$/}));
        await waitFor(() => expect(Modal.confirm).toHaveBeenCalledTimes(1));
        const {onOk} = Modal.confirm.mock.calls[0][0];

        const close = jest.fn();
        act(() => { onOk(close); onOk(close); });
        await waitFor(() => expect(message.error).toHaveBeenCalledTimes(1));
        expect(api.manage[operation]).toHaveBeenCalledTimes(1);
        expect(close).not.toHaveBeenCalled();
        expect(message.success).not.toHaveBeenCalled();
        await waitFor(() => expect(Modal.confirm.mock.results[0].value.update)
            .toHaveBeenLastCalledWith(expect.objectContaining({
                okButtonProps: {loading: false, disabled: false},
            })));
        const retry = () => {
            if (_name === 'property') act(() => onOk(close));
            else {
                const content = Modal.confirm.mock.results[0].value.update.mock.calls.at(-1)[0].content;
                const view = render(content);
                fireEvent.click(screen.getByRole('button', {name: 'schema.delete_retry'}));
                view.unmount();
            }
        };
        retry();
        await waitFor(() => expect(message.error).toHaveBeenCalledTimes(2));
        expect(close).not.toHaveBeenCalled();
        expect(message.success).not.toHaveBeenCalled();
        await waitFor(() => expect(Modal.confirm.mock.results[0].value.update)
            .toHaveBeenLastCalledWith(expect.objectContaining({okButtonProps: {loading: false, disabled: false}})));
        retry();
        await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
        expect(api.manage[operation]).toHaveBeenLastCalledWith('DEFAULT', 'audit',
            {names: ['audit_label'], skip_using: false}, {suppressBusinessErrorToast: true});
        expect(message.success).toHaveBeenCalledTimes(1);
    });
