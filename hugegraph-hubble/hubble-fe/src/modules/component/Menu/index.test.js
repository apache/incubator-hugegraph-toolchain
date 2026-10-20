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
import Menu from './index';
import {GraphContext} from '../Context';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
const fixture = target => {
    const container = document.createElement('div');
    container.getBoundingClientRect = () => ({left: 100, top: 40, width: 200, height: 150});
    return {controller: {pin: jest.fn(async () => ({})), hide: jest.fn(async () => ({}))},
        snapshot: {status: 'ready', data: {nodes: [{id: 'node:large', data: {fixed: false}}], edges: []}},
        contextTarget: target, setContextTarget: jest.fn(), containerRef: {current: container},
        run: operation => Promise.resolve().then(operation)};
};
const target = {kind: 'node', id: 'node:large', data: {raw: {id: 90071992547409930000n, label: 'person'}},
    client: {x: 999, y: 999}};

test('clamps instance menu and expands with original ID rather than rendering ID', async () => {
    const value = fixture(target);
    const expand = jest.fn();
    render(<GraphContext.Provider value={value}><Menu onClickExpand={expand} /></GraphContext.Provider>);
    expect(screen.getByRole('menu')).toHaveStyle({left: '200px', top: '150px'});
    expect(screen.queryByRole('menuitem', {name: 'analysis.canvas.context_menu.add_out_edge'})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.expand'}));
    await waitFor(() => expect(expand).toHaveBeenCalledWith({vertex_id: target.data.raw.id, vertex_label: 'person'}));
    expect(value.setContextTarget).toHaveBeenCalledWith(null);
});

test('uses controller view actions and closes on Escape or outside interaction', async () => {
    const value = fixture(target);
    const result = render(<GraphContext.Provider value={value}><Menu /></GraphContext.Provider>);
    fireEvent.click(screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.fix'}));
    await waitFor(() => expect(value.controller.pin).toHaveBeenCalledWith(['node:large'], true));
    fireEvent.keyDown(document, {key: 'Escape'});
    fireEvent.pointerDown(document.body);
    expect(value.setContextTarget).toHaveBeenCalledTimes(3);
    result.unmount();
    fireEvent.pointerDown(document.body);
    expect(value.setContextTarget).toHaveBeenCalledTimes(3);
});

test('uses raw vertex details for search and edge creation and omits unavailable canvas editing', async () => {
    const value = fixture(target);
    const search = jest.fn();
    const edge = jest.fn();
    const result = render(<GraphContext.Provider value={value}>
        <Menu onClickSearch={search} onClickAddEdge={edge} />
    </GraphContext.Provider>);
    fireEvent.click(screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.search'}));
    fireEvent.click(screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.add_in_edge'}));
    await waitFor(() => expect(edge).toHaveBeenCalledWith({vertexId: target.data.raw.id, vertexLabel: 'person'}, false));
    expect(search).toHaveBeenCalledWith({vertexId: target.data.raw.id, vertexLabel: 'person'});
    result.rerender(<GraphContext.Provider value={{...value, contextTarget: {kind: 'canvas', client: {x: 2, y: 3}}}}>
        <Menu />
    </GraphContext.Provider>);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
});
