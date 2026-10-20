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

import {createRef} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import LayoutConfigPanel from './index';
import {GraphContext} from '../../Context';
import {G6Runtime} from '../../../../visualization/runtime/G6Runtime';
import {normalizeLayoutSettings} from '../../../../utils/analysisGraphPresentation';
import {hubbleRenderId} from '../../../../visualization/hubble/adapter';

let mockNodes = [];
const mockEngine = {on: jest.fn(), off: jest.fn(), destroy: jest.fn(), stopLayout: jest.fn(),
    setData: jest.fn(value => {mockNodes = value.nodes;}), getNodeData: jest.fn(() => mockNodes),
    getPluginInstance: jest.fn(), draw: jest.fn(async () => {}), render: jest.fn(async () => {}),
    layout: jest.fn(async () => {}), setLayout: jest.fn()};
jest.mock('@antv/g6', () => ({Graph: jest.fn(() => mockEngine), GraphEvent: {AFTER_TRANSFORM: 'aftertransform'}}));
jest.mock('../../../../visualization/runtime/extensions', () => ({registerExtensions: jest.fn()}));
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
    fireEvent.click(await screen.findByText(label(name), {
        selector: '.ant-select-item-option-content .changeLayoutTypeSelectName',
    }));
};

test('does not inspect graph data while closed', () => {
    render(<LayoutConfigPanel open={false} data={{get nodes() { throw new Error('closed'); }}} />);
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
});

test('flushes the latest edit for an explicit panel close and never replays it after unmount', async () => {
    const onChange = jest.fn();
    const ref = createRef();
    const view = render(panel(onChange, {ref, layout: {type: 'concentric', options: {startAngle: 0}}}));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('start_angle')}), {target: {value: '1'}});
    expect(onChange).not.toHaveBeenCalled();
    await act(async () => { ref.current.flush(); });
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({type: 'concentric',
        options: expect.objectContaining({startAngle: 1})}));
    view.unmount();
    await new Promise(resolve => setTimeout(resolve, 150));
    expect(onChange).toHaveBeenCalledTimes(1);
});

test('direct unmount cancels the pending edit even through a retained imperative handle', async () => {
    const onChange = jest.fn();
    const ref = createRef();
    const view = render(panel(onChange, {ref, layout: {type: 'concentric', options: {startAngle: 0}}}));
    const handle = ref.current;
    fireEvent.change(screen.getByRole('spinbutton', {name: label('start_angle')}), {target: {value: '1'}});
    view.unmount();
    await act(async () => {
        handle.flush();
        await new Promise(resolve => setTimeout(resolve, 150));
    });
    expect(onChange).not.toHaveBeenCalled();
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

test('shows decorative layout images in the selected value and all six menu choices', async () => {
    const {container} = render(panel(jest.fn()));
    const selected = () => container.querySelector('.assetsItem');
    expect(selected()).toHaveTextContent(label('force'));
    expect(selected().querySelector('.assetsTitle[aria-hidden="true"] img[alt=""]')).not.toBeNull();
    fireEvent.mouseDown(screen.getByRole('combobox', {name: label('layout_type')}));
    const menuNames = ['force', 'circular', 'concentric', 'dagre', 'grid', 'radial'];
    for (const name of menuNames) {
        const text = await screen.findByText(label(name), {
            selector: '.ant-select-item-option-content .changeLayoutTypeSelectName',
        });
        expect(text.parentElement.querySelector('.assetsTitle[aria-hidden="true"] img[alt=""]')).not.toBeNull();
    }
    fireEvent.click(screen.getByText(label('radial'), {
        selector: '.ant-select-item-option-content .changeLayoutTypeSelectName',
    }));
    expect(selected()).toHaveTextContent(label('radial'));
    expect(selected().querySelector('.assetsTitle[aria-hidden="true"] img[alt=""]')).not.toBeNull();
});

test('submits editable force options and cancels pending edits on direct close', async () => {
    const onChange = jest.fn();
    const result = render(panel(onChange, {layout: {type: 'd3-force', options: {velocityDecay: 0.2}}}));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({
        type: 'd3-force', options: {linkDistance: 225, velocityDecay: 0.2},
    })));
    const count = onChange.mock.calls.length;
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '300'}});
    result.rerender(panel(onChange, {open: false}));
    await new Promise(resolve => setTimeout(resolve, 150));
    expect(onChange).toHaveBeenCalledTimes(count);
});

test('reopens the submitted force size and spacing without replacing them on the next edit', async () => {
    const onChange = jest.fn();
    const layout = {type: 'd3-force', options: {nodeSize: 70, nodeSpacing: 40, preventOverlap: false}};
    const view = render(panel(onChange, {layout}));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    const submitted = onChange.mock.lastCall[0];
    view.rerender(panel(onChange, {layout: submitted, open: false}));
    view.rerender(panel(onChange, {layout: submitted}));
    expect(screen.getByRole('spinbutton', {name: label('node_size')})).toHaveValue('70');
    expect(screen.getByRole('spinbutton', {name: label('node_spacing')})).toHaveValue('40');
    const overlap = screen.getByRole('switch', {name: label('prevent_overlap')});
    expect(overlap).not.toBeChecked();
    fireEvent.click(overlap);
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force',
        options: expect.objectContaining({nodeSize: 70, nodeSpacing: 40, preventOverlap: true, linkDistance: 225})}));
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

test.each([
    {name: 'default relaxed rings', options: {}, strict: false},
    {name: 'saved strict rings', options: {strictRadial: true}, strict: true},
])('radial keeps $name when editing another option', async ({options, strict}) => {
    const onChange = jest.fn();
    render(panel(onChange, {layout: {type: 'radial', options}}));
    expect(screen.getByRole('switch', {name: label('strict_radial')})).toHaveAttribute('aria-checked', String(strict));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance_short')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'radial',
        options: {...options, linkDistance: 225}}));
});

// Exercise the actual form -> normalization -> runtime path, mocking only the native renderer.
test('editing only force link distance leaves adaptive strength and collision in runtime ownership', async () => {
    const graph = {nodes: [{id: 'a', data: {raw: {id: 'a'}}, style: {size: 30}}], edges: []};
    const runtime = new G6Runtime(document.createElement('div'));
    const onChange = jest.fn(async value => runtime.layout(normalizeLayoutSettings(value)));
    try {
        await runtime.replace(graph, null);
        render(panel(onChange, {data: graph}));
        fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
        await waitFor(() => expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({
            link: {distance: 225}, manyBody: {strength: -800}, collide: {radius: 40},
        })));
        expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force', options: {linkDistance: 225}});
    }
    finally { runtime.stop(); runtime.destroy(); }
});

test('switching from radial back to force restores automatic force defaults', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const onChange = jest.fn(async value => runtime.layout(normalizeLayoutSettings(value)));
    try {
        await runtime.replace(data, null);
        render(panel(onChange, {layout: {type: 'radial'}}));
        await selectLayout('force');
        await waitFor(() => expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({
            link: {distance: 180}, manyBody: {strength: -800}, collide: {radius: 40},
        })));
    }
    finally { runtime.stop(); runtime.destroy(); }
});

test('switching to radial and editing its link keeps collision and ring distance safe for actual 80px nodes', async () => {
    const graph = {nodes: [{id: 'a', data: {raw: {id: 'a'}}, style: {size: 80}}], edges: []};
    const runtime = new G6Runtime(document.createElement('div'));
    const onChange = jest.fn(async value => runtime.layout(normalizeLayoutSettings(value)));
    try {
        await runtime.replace(graph, null);
        render(panel(onChange, {data: graph}));
        await selectLayout('radial');
        await waitFor(() => expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({
            nodeSize: 80, unitRadius: 104, strictRadial: false,
        })));
        fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance_short')}), {target: {value: '225'}});
        await waitFor(() => expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({
            nodeSize: 80, unitRadius: 104, linkDistance: 225,
        })));
    }
    finally { runtime.stop(); runtime.destroy(); }
});

test('rapid edits accumulate only explicit values and survive reopening', async () => {
    const ref = createRef();
    const onChange = jest.fn();
    const view = render(panel(onChange, {ref}));
    expect(screen.getByRole('spinbutton', {name: label('node_strength')})).toHaveValue('800');
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    fireEvent.change(screen.getByRole('spinbutton', {name: label('node_strength')}), {target: {value: '900'}});
    await act(async () => {ref.current.flush();});
    expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force', options: {linkDistance: 225, nodeStrength: 900}});
    const submitted = onChange.mock.lastCall[0];
    view.rerender(panel(onChange, {layout: submitted, open: false}));
    view.rerender(panel(onChange, {layout: submitted}));
    expect(screen.getByRole('spinbutton', {name: label('node_strength')})).toHaveValue('900');
    expect(screen.getByRole('spinbutton', {name: label('link_distance')})).toHaveValue('225');
});

test('canonical saved collision and strength survive a link-only edit', async () => {
    const onChange = jest.fn();
    const options = {manyBody: {strength: -950}, collide: {radius: 55, iterations: 2}};
    render(panel(onChange, {layout: {type: 'd3-force', options}}));
    expect(screen.getByRole('spinbutton', {name: label('node_strength')})).toHaveValue('950');
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force',
        options: {...options, linkDistance: 225}}));
    expect(normalizeLayoutSettings(onChange.mock.lastCall[0]).options).toEqual({
        ...options, link: {distance: 225},
    });
});

test('a saved partial force collision pair displays its effective diameter without rewriting it', async () => {
    const onChange = jest.fn();
    render(panel(onChange, {layout: {type: 'd3-force', options: {nodeSpacing: 40}}}));
    expect(screen.getByRole('spinbutton', {name: label('node_size')})).toHaveValue('10');
    expect(screen.getByRole('spinbutton', {name: label('node_spacing')})).toHaveValue('40');
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force',
        options: {nodeSpacing: 40, linkDistance: 225}}));
    expect(normalizeLayoutSettings(onChange.mock.lastCall[0]).options.collide).toEqual({radius: 25});
});

test('a partial native collision object displays the native fallback without adding a radius on a link edit', async () => {
    const onChange = jest.fn();
    const options = {collide: {iterations: 2}};
    render(panel(onChange, {layout: {type: 'd3-force', options}}));
    expect(screen.getByRole('spinbutton', {name: label('node_size')})).toHaveValue('10');
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance')}), {target: {value: '225'}});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({type: 'd3-force',
        options: {...options, linkDistance: 225}}));
});

test('same-type external layout updates keep the new focus and pending user edits', async () => {
    const context = fixture();
    const onChange = jest.fn();
    const ref = createRef();
    const withFocus = focusNode => <GraphContext.Provider value={context}>
        <LayoutConfigPanel open ref={ref} data={data} onChange={onChange}
            layout={{type: 'radial', options: {focusNode}}} />
    </GraphContext.Provider>;
    const view = render(withFocus('node:old'));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance_short')}), {target: {value: '225'}});
    view.rerender(withFocus('node:new'));
    await act(async () => {ref.current.flush();});
    expect(onChange).toHaveBeenLastCalledWith({type: 'radial', options: {focusNode: 'node:new', linkDistance: 225}});
    // A completed edit is no longer a pending patch that can override a later external update.
    view.rerender(withFocus('node:newer'));
    fireEvent.change(screen.getByRole('spinbutton', {name: label('link_distance_short')}), {target: {value: '250'}});
    await act(async () => {ref.current.flush();});
    expect(onChange).toHaveBeenLastCalledWith({type: 'radial', options: {focusNode: 'node:newer', linkDistance: 250}});
});
