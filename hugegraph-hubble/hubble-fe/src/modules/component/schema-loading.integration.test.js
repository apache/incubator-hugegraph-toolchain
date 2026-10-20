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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import DynamicAddNode from './DynamicAddNode';
import Search from './Search';
import GraphAnalysisContext from '../Context';
import * as api from '../../api';

jest.mock('react-i18next', () => {
    const t = key => key;
    return {...jest.requireActual('react-i18next'), useTranslation: () => ({t})};
});
jest.mock('../../api', () => ({manage: {getMetaVertexLink: jest.fn(), getMetaEdge: jest.fn()}}));

beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});

test.each([true, false])('keeps mounted graph drawers safe while new graph Schema loads (open: %s)', async open => {
    const viewFor = (graph, schema) => (
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
            <DynamicAddNode open={open} drawerInfo={schema} />
        </GraphAnalysisContext.Provider>
    );
    const schemaFor = name => [{name, id_strategy: 'AUTOMATIC', properties: []}];
    const view = render(viewFor('graph-a', schemaFor('old-person')));
    // Home immediately hides the previous graph's Schema, before graph B responds.
    view.rerender(viewFor('graph-b', undefined));
    view.rerender(viewFor('graph-b', []));
    view.rerender(viewFor('graph-b', schemaFor('new-person')));
    if (open) {
        fireEvent.mouseDown(screen.getByRole('combobox', {name: /dynamic_add.vertex_type/}));
        await waitFor(() => expect(screen.getByText('new-person', {
            selector: '.ant-select-item-option-content',
        })).toBeVisible());
        expect(screen.queryByText('old-person', {selector: '.ant-select-item-option-content'})).not.toBeInTheDocument();
    }
});

test.each([true, false])('keeps mounted expansion search safe while property keys load (open: %s)', open => {
    const viewFor = (graph, keys) => (
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
            <Search open={open} propertykeys={keys} />
        </GraphAnalysisContext.Provider>
    );
    const view = render(viewFor('graph-a', [{name: 'old-name', data_type: 'TEXT'}]));
    view.rerender(viewFor('graph-b', undefined));
    view.rerender(viewFor('graph-b', []));
    view.rerender(viewFor('graph-b', [{name: 'new-age', data_type: 'INT'}]));
});

test('enables a selected search property when its pending Schema type arrives', async () => {
    api.manage.getMetaVertexLink.mockResolvedValue({status: 200, data: ['knows']});
    api.manage.getMetaEdge.mockResolvedValue({status: 200, data: {properties: [{name: 'age'}]}});
    const viewFor = keys => (
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'graph-b'}}>
            <Search open vertexLabel='person' propertykeys={keys} />
        </GraphAnalysisContext.Provider>
    );
    const view = render(viewFor(undefined));
    const choose = async (name, value) => {
        fireEvent.mouseDown(screen.getByRole('combobox', {name: `analysis.canvas.filter_drawer.${name}`}));
        fireEvent.click(await screen.findByText(value, {selector: '.ant-select-item-option-content'}));
    };
    await choose('edge_label', 'knows');
    const add = screen.getByRole('button', {name: 'analysis.canvas.filter_drawer.add_property'});
    await waitFor(() => expect(add).toBeEnabled());
    fireEvent.click(add);
    await choose('property', 'age');
    expect(screen.getByPlaceholderText('analysis.canvas.filter_drawer.placeholder')).toBeDisabled();
    view.rerender(viewFor([{name: 'age', data_type: 'INT'}]));
    expect(screen.getByRole('spinbutton', {name: 'analysis.canvas.filter_drawer.value'})).toBeEnabled();
});
