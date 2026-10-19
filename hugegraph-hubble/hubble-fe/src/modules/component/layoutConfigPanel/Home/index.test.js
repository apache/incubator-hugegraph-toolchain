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
import LayoutConfigPanel from './index';
import {GraphContext} from '../../Context';
import {hubbleRenderId} from '../../../../visualization/hubble/adapter';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
const data = {nodes: [{id: 'node:a', data: {raw: {id: 90071992547409930000n}}}], edges: []};
const fixture = () => ({controller: {}, snapshot: {status: 'ready'},
    run: operation => Promise.resolve().then(operation)});
const panel = (onChange, props = {}) => <GraphContext.Provider value={fixture()}>
    <LayoutConfigPanel layout={{type: 'd3-force'}} data={data} open onChange={onChange} {...props} />
</GraphContext.Provider>;
const label = key => `analysis.canvas.layout_panel.${key}`;
const selectLayout = async name => {
    fireEvent.mouseDown(screen.getByRole('combobox', {name: label('layout_type')}));
    fireEvent.click(await screen.findByText(label(name), {selector: '.ant-select-item-option-content'}));
};

test('does not inspect graph data while closed', () => {
    render(<LayoutConfigPanel open={false} data={{get nodes() { throw new Error('closed'); }}} />);
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
});

test('offers six canonical layout choices and submits the selected layout', async () => {
    const onChange = jest.fn();
    render(panel(onChange));
    for (const type of ['circular', 'concentric', 'dagre', 'grid', 'radial', 'force']) {
        await selectLayout(type);
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({
            type: type === 'force' ? 'd3-force' : type,
        })));
    }
    expect(onChange.mock.calls.every(([value]) => value.options && !Object.hasOwn(value, 'pipes'))).toBe(true);
});

test('normalizes force options and cancels pending layout edits on close', async () => {
    const onChange = jest.fn();
    const result = render(panel(onChange, {layout: {type: 'd3-force', options: {velocityDecay: 0.2}}}));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({
        type: 'd3-force', options: expect.objectContaining({link: {distance: 225},
            manyBody: {strength: -300}, velocityDecay: 0.2}),
    })));
    const count = onChange.mock.calls.length;
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '300'}});
    result.rerender(panel(onChange, {open: false}));
    await new Promise(resolve => setTimeout(resolve, 150));
    expect(onChange).toHaveBeenCalledTimes(count);
});

test('keeps positive grid rows and converts radial raw IDs once at its selector', async () => {
    const onChange = jest.fn();
    render(panel(onChange));
    await selectLayout('grid');
    fireEvent.change(screen.getByRole('spinbutton', {name: label('rows')}), {target: {value: '3'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'grid', options: {rows: 3}}));
    await selectLayout('radial');
    fireEvent.mouseDown(screen.getByRole('combobox', {name: label('focus_node')}));
    fireEvent.click(await screen.findByText('90071992547409930000', {selector: '.ant-select-item-option-content'}));
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({
        type: 'radial', options: expect.objectContaining({focusNode: hubbleRenderId('node', 90071992547409930000n)}),
    })));
});
