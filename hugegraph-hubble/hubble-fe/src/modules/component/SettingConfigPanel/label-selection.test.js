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
import SettingConfigPanel from './index';
import {GraphContext} from '../Context';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query,
        addListener: jest.fn(), removeListener: jest.fn(), addEventListener: jest.fn(),
        removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});

test.each([['node', 'closure'], ['edge', 'closure'], ['node', 'renderer-unmount'], ['edge', 'renderer-unmount']])('preserves selected %s fields across %s and resets them for a new dataset', async (kind, transition) => {
    const value = {controller: {}, snapshot: {status: 'ready'}, run: operation => Promise.resolve().then(operation)};
    const elementLabel = kind === 'node' ? 'person' : 'knows';
    const raw = {id: 'alice-id', label: elementLabel, properties: {name: 'Alice'}};
    const id = `${kind}:string:alice-id`;
    const group = kind === 'node' ? 'nodes' : 'edges';
    const originalText = kind === 'node' ? raw.id : raw.label;
    let data = {nodes: [], edges: [], [group]: [{id, data: {raw}, style: {labelText: originalText}}]};
    const onChange = jest.fn(patches => {
        data = {...data, [group]: data[group].map(element => ({...element,
            style: {...element.style, ...patches.find(patch => patch.id === element.id)?.style}}))};
    });
    const fields = new Map();
    const content = (open, datasetKey = 1) => {
        if (!fields.has(datasetKey)) fields.set(datasetKey, {current: {node: new Map(), edge: new Map()}});
        return <GraphContext.Provider value={value}>
        {(open || transition === 'closure') && <SettingConfigPanel key={datasetKey} open={open} data={data}
            selectedFields={fields.get(datasetKey)}
            metaData={{[kind === 'node' ? 'vertexMeta' : 'edgeMeta']:
                [{name: elementLabel, style: {display_fields: ['~id']}}]}} onChange={onChange} />}
    </GraphContext.Provider>;
    };
    const view = render(content(true));
    const fieldLabel = key => `analysis.canvas.setting_panel.${key}`;
    fireEvent.mouseDown(screen.getByRole('combobox', {name: fieldLabel(`${kind}_label_property`)}));
    fireEvent.click(await screen.findByText(elementLabel, {selector: '.ant-cascader-menu-item-content'}));
    fireEvent.click(await screen.findByText('name', {selector: '.ant-cascader-menu-item-content'}));
    fireEvent.click(await screen.findByText(fieldLabel(kind === 'node' ? 'node_id' : 'edge_type'),
        {selector: '.ant-cascader-menu-item-content'}));
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id, kind, style: expect.objectContaining({labelText: 'Alice'})},
    ]));
    view.rerender(content(false));
    // Other presentation values must come from the current snapshot, not a stale form draft.
    data = {...data, [group]: data[group].map(element => ({...element,
        style: {...element.style, labelFontSize: 24}}))};
    view.rerender(content(true));
    expect(screen.getByRole('slider', {name: fieldLabel(`${kind}_label_size`)})).toHaveAttribute('aria-valuenow', '24');
    fireEvent.click(screen.getByRole('switch', {name: fieldLabel(`${kind}_label`)}));
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id, kind, style: expect.objectContaining({labelText: ''})},
    ]));
    fireEvent.click(screen.getByRole('switch', {name: fieldLabel(`${kind}_label`)}));
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id, kind, style: expect.objectContaining({labelText: 'Alice'})},
    ]));
    view.rerender(content(true, 2));
    fireEvent.click(screen.getByRole('switch', {name: fieldLabel(`${kind}_label`)}));
    fireEvent.click(screen.getByRole('switch', {name: fieldLabel(`${kind}_label`)}));
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id, kind, style: expect.objectContaining({labelText: originalText})},
    ]));
});
