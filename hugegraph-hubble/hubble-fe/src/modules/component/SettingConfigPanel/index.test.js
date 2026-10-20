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

import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';
import SettingConfigPanel from './index';
import Legend from '../Legend';
import {GraphContext} from '../Context';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
const fixture = () => ({controller: {highlight: jest.fn(async () => ({})), setMinimap: jest.fn(async () => ({status: 'applied'}))},
    snapshot: {status: 'ready'}, showLegend: true, showGrid: true, showMinimap: true,
    setShowLegend: jest.fn(), setShowGrid: jest.fn(), setShowMinimap: jest.fn(),
    run: operation => Promise.resolve().then(operation)});
const raw = Object.freeze({id: 90071992547409930000n, label: 'person', properties: Object.freeze({name: 'Alice'})});
const data = {nodes: [Object.freeze({id: 'node:large', data: Object.freeze({raw}),
    style: Object.freeze({labelText: 'Alice', labelFontSize: 12, labelPlacement: 'bottom', x: 20})})], edges: []};
const metaData = {vertexMeta: [{name: 'person', style: {display_fields: ['name']}}]};
const setting = (value, props) => <GraphContext.Provider value={value}>
    <SettingConfigPanel data={data} metaData={metaData} open {...props} />
</GraphContext.Provider>;
const label = key => `analysis.canvas.setting_panel.${key}`;

test('does not inspect graph data or create controls while closed', () => {
    const unread = {get nodes() { throw new Error('closed panel must not inspect data'); }};
    render(<SettingConfigPanel open={false} data={unread} />);
    expect(screen.queryByRole('switch')).not.toBeInTheDocument();
});

test('changes labels with style patches, preserves raw data and retains draft across position snapshots', async () => {
    const value = fixture();
    const onChange = jest.fn();
    const result = render(setting(value, {onChange}));
    const toggle = screen.getByRole('switch', {name: label('node_label')});
    fireEvent.click(toggle);
    await waitFor(() => expect(onChange).toHaveBeenCalledWith([
        {id: 'node:large', kind: 'node', style: {labelText: ''}},
    ]));
    fireEvent.click(toggle);
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id: 'node:large', kind: 'node', style: {labelText: 'Alice'}},
    ]));
    const font = screen.getByRole('slider', {name: label('node_label_size')});
    fireEvent.keyDown(font, {key: 'End', keyCode: 35});
    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith([
        {id: 'node:large', kind: 'node', style: {labelFontSize: 48}},
    ]));
    result.rerender(setting(value, {onChange, data: {...data, nodes: [{...data.nodes[0],
        style: {...data.nodes[0].style, x: 888}}]}}));
    expect(screen.getByRole('slider', {name: label('node_label_size')})).toHaveAttribute('aria-valuenow', '48');
    expect(data.nodes[0].data.raw).toBe(raw);
    expect(data.nodes[0].style.labelText).toBe('Alice');
    expect(raw.properties.name).toBe('Alice');
});

test('scopes module toggles to context and keeps minimap state when reopening', async () => {
    const first = fixture();
    const second = fixture();
    const renderPanels = open => <>
        <section aria-label="first">{setting(first, {open})}</section>
        <section aria-label="second">{setting(second, {})}</section>
    </>;
    const result = render(renderPanels(true));
    const region = within(screen.getByRole('region', {name: 'first'}));
    fireEvent.click(region.getByRole('switch', {name: label('enable_minimap')}));
    fireEvent.click(region.getByRole('switch', {name: label('enable_grid')}));
    fireEvent.click(region.getByRole('switch', {name: label('enable_legend')}));
    await waitFor(() => expect(first.setShowMinimap).toHaveBeenCalledWith(false));
    expect(first.controller.setMinimap).toHaveBeenCalledWith(false);
    expect(first.setShowGrid).toHaveBeenCalledWith(false, expect.anything());
    expect(first.setShowLegend).toHaveBeenCalledWith(false, expect.anything());
    expect(second.controller.setMinimap).not.toHaveBeenCalled();
    expect(second.setShowGrid).not.toHaveBeenCalled();
    first.showMinimap = false;
    result.rerender(renderPanels(false));
    result.rerender(renderPanels(true));
    expect(within(screen.getByRole('region', {name: 'first'})).getByRole('switch', {
        name: label('enable_minimap'),
    })).toHaveAttribute('aria-checked', 'false');
});

test('legend respects the owning context visibility', () => {
    const value = {...fixture(), showLegend: false, snapshot: {status: 'ready', data: {
        nodes: [{...data.nodes[0], data: {raw, legend: 'person'}}], edges: []}}};
    const result = render(<GraphContext.Provider value={value}><Legend /></GraphContext.Provider>);
    expect(screen.queryByRole('button', {name: 'person'})).not.toBeInTheDocument();
    result.rerender(<GraphContext.Provider value={{...value, showLegend: true}}><Legend /></GraphContext.Provider>);
    expect(screen.getByRole('button', {name: 'person'})).toBeInTheDocument();
});

test('keeps experimental rendering inside a collapsed settings section', async () => {
    const user = userEvent.setup();
    const onRenderModeChange = jest.fn();
    render(setting(fixture(), {renderMode: GRAPH_RENDER_MODE.CANVAS2D, onRenderModeChange}));
    const summary = screen.getByText('analysis.canvas.render_mode', {selector: 'summary'});
    expect(summary.parentElement).not.toHaveAttribute('open');
    expect(screen.getByRole('combobox', {name: 'analysis.canvas.render_mode'})).not.toBeVisible();
    await user.click(summary);
    await user.click(screen.getByRole('combobox', {name: 'analysis.canvas.render_mode'}));
    await user.click(await screen.findByText('analysis.canvas.mode_3d_experimental',
        {selector: '.ant-select-item-option-content'}));
    expect(onRenderModeChange).toHaveBeenCalledWith(GRAPH_RENDER_MODE.CANVAS3D);
});

test('offers a working return to 2D in 3D settings without mounting 2D-only controls', async () => {
    const user = userEvent.setup();
    const onRenderModeChange = jest.fn();
    const unread = {get nodes() { throw new Error('3D settings must not inspect 2D data'); }};
    render(<SettingConfigPanel open data={unread} renderMode={GRAPH_RENDER_MODE.CANVAS3D}
        onRenderModeChange={onRenderModeChange} />);
    expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    expect(screen.queryByRole('slider')).not.toBeInTheDocument();
    await user.click(screen.getByText('analysis.canvas.render_mode', {selector: 'summary'}));
    await user.click(screen.getByRole('combobox', {name: 'analysis.canvas.render_mode'}));
    await user.click(await screen.findByText('analysis.canvas.mode_2d', {selector: '.ant-select-item-option-content'}));
    expect(onRenderModeChange).toHaveBeenCalledWith(GRAPH_RENDER_MODE.CANVAS2D);
});
