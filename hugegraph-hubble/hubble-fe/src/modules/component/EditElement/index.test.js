/*
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

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import EditElement from './index';
import {GraphContext} from '../Context';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api/index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../api/index', () => ({analysis: {
    getVertexProperties: jest.fn(), getEdgeProperties: jest.fn(),
    updateVertexProperties: jest.fn(), updateEdgeProperties: jest.fn(),
}}));
const initial = {kind: 'node', renderingId: 'node:large', id: 90071992547409930000n,
    itemType: 'person', properties: {name: 'Alice', code: 'K1', count: 3}, raw: {}};
const fixture = () => ({controller: {}, report: jest.fn()});
const content = (value, props, context = {graphSpace: 'DEFAULT', graph: 'g'}) => (
    <GraphContext.Provider value={{...value, run: operation => Promise.resolve().then(operation).catch(value.report)}}>
        <GraphAnalysisContext.Provider value={context}><EditElement show drawerInfo={initial} {...props} /></GraphAnalysisContext.Provider>
    </GraphContext.Provider>
);
const edit = () => fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'}));
const save = () => fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.save'}));
beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query,
        addListener: jest.fn(), removeListener: jest.fn(), addEventListener: jest.fn(),
        removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
beforeEach(() => {
    jest.resetAllMocks();
    api.analysis.getVertexProperties.mockResolvedValue({status: 200,
        data: {nullableProps: ['name'], nonNullableProps: ['code'], primaryKeys: ['code']}});
    api.analysis.updateVertexProperties.mockResolvedValue({status: 200});
});

test('saves using raw ID and reports only one raw patch with preserved primary properties', async () => {
    const value = fixture();
    const onChange = jest.fn().mockResolvedValue({status: 'applied'});
    const cancel = jest.fn();
    render(content(value, {onChange, cancel}));
    edit();
    const input = await screen.findByRole('textbox', {name: 'name'});
    fireEvent.change(input, {target: {value: 'Bob'}});
    save();
    await waitFor(() => expect(onChange).toHaveBeenCalledWith('nodes', 'node:large', {
        id: initial.id, label: 'person', properties: {name: 'Bob', code: 'K1', count: 3},
    }));
    expect(api.analysis.updateVertexProperties).toHaveBeenCalledWith('DEFAULT', 'g', {
        id: initial.id, label: 'person', properties: {name: 'Bob'},
    });
    expect(cancel).toHaveBeenCalledTimes(1);
});

test('ignores late metadata response after changing graph or element', async () => {
    const value = fixture();
    let resolve;
    api.analysis.getVertexProperties.mockImplementationOnce(() => new Promise(done => { resolve = done; }));
    const result = render(content(value, {onChange: jest.fn()}));
    edit();
    await waitFor(() => expect(api.analysis.getVertexProperties).toHaveBeenCalled());
    result.rerender(content(value, {onChange: jest.fn(), drawerInfo: {...initial, renderingId: 'node:new', id: 'new'}},
        {graphSpace: 'OTHER', graph: 'other'}));
    await act(async () => resolve({status: 200, data: {nullableProps: ['name']}}));
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'})).toBeEnabled();
});

test('ignores late save response after changing graph and catches rejected requests', async () => {
    const value = fixture();
    const onChange = jest.fn();
    let resolve;
    api.analysis.updateVertexProperties.mockImplementationOnce(() => new Promise(done => { resolve = done; }));
    const result = render(content(value, {onChange}));
    edit();
    await screen.findByRole('textbox', {name: 'name'});
    save();
    await waitFor(() => expect(api.analysis.updateVertexProperties).toHaveBeenCalled());
    result.rerender(content(value, {onChange}, {graphSpace: 'OTHER', graph: 'other'}));
    await act(async () => resolve({status: 200}));
    expect(onChange).not.toHaveBeenCalled();
    api.analysis.getVertexProperties.mockRejectedValueOnce(new Error('network'));
    edit();
    await waitFor(() => expect(value.report).toHaveBeenCalledWith(expect.any(Error)));
    expect(screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'})).toBeEnabled();
});

test('edge editing preserves original endpoints and uses rendering ID only for callback', async () => {
    const value = fixture();
    api.analysis.getEdgeProperties.mockResolvedValue({status: 200, data: {nullableProps: ['weight']}});
    api.analysis.updateEdgeProperties.mockResolvedValue({status: 200});
    const onChange = jest.fn().mockResolvedValue({status: 'applied'});
    render(content(value, {onChange, drawerInfo: {kind: 'edge', renderingId: 'edge:render', id: 'business-edge',
        itemType: 'knows', source: initial.id, target: 'peer', properties: {weight: '1'}}}));
    edit();
    const input = await screen.findByRole('textbox', {name: 'weight'});
    fireEvent.change(input, {target: {value: '2'}});
    save();
    await waitFor(() => expect(onChange).toHaveBeenCalledWith('edges', 'edge:render', {
        id: 'business-edge', label: 'knows', source: initial.id, target: 'peer', properties: {weight: '2'},
    }));
});
