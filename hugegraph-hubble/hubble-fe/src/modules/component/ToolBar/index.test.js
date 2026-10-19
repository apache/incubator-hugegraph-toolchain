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

import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {GraphContext} from '../Context';
import ToolBar from './index';
import RedoUndo from '../RedoUndo';
import FixNode from '../FixNode';
import ZoomGraph from '../ZoomGraph';
import FitCenter from '../FitCenter';
import RefreshGraph from '../RefreshGraph';
import ClearGraph from '../ClearGraph';
import ZeroDegreeNode from '../ZeroDegreeNode';
import MiniMap from '../MiniMap';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

const makeValue = () => {
    const controller = Object.fromEntries(['undo', 'redo', 'pin', 'hide', 'zoomBy', 'fitView',
        'setLayout', 'replaceData', 'setMinimap'].map(name => [name, jest.fn(async () => ({status: 'applied'}))]));
    const report = jest.fn();
    return {
        controller, report, run: operation => Promise.resolve().then(operation).catch(report),
        layout: {type: 'grid'},
        snapshot: {status: 'ready', canUndo: true, canRedo: false, selectedIds: ['a', 'b', 'edge'],
            data: {nodes: [{id: 'a', data: {fixed: true}}, {id: 'b', data: {fixed: false}},
                {id: 'alone', data: {}}], edges: [{id: 'edge', source: 'a', target: 'b'}]}},
    };
};
const button = name => screen.getByRole('button', {name: `analysis.canvas.toolbar.${name}`});
const view = (value, children) => <GraphContext.Provider value={value}>{children}</GraphContext.Provider>;

test('uses controller history availability and never broadcasts a second data update', async () => {
    const value = makeValue();
    const onChange = jest.fn();
    const result = render(view(value, <RedoUndo onChange={onChange} />));
    expect(button('undo')).toBeEnabled();
    expect(button('redo')).toBeDisabled();
    fireEvent.click(button('undo'));
    await waitFor(() => expect(value.controller.undo).toHaveBeenCalledTimes(1));
    result.rerender(view({...value, snapshot: {...value.snapshot, canUndo: false, canRedo: true}},
        <RedoUndo onChange={onChange} />));
    fireEvent.click(button('redo'));
    await waitFor(() => expect(value.controller.redo).toHaveBeenCalledTimes(1));
    expect(onChange).not.toHaveBeenCalled();
});

test('pins only selected nodes, unpins an entirely fixed selection, and disables for edges', async () => {
    const value = makeValue();
    const result = render(view(value, <FixNode />));
    fireEvent.click(button('fix_node'));
    await waitFor(() => expect(value.controller.pin).toHaveBeenLastCalledWith(['a', 'b'], true));
    result.rerender(view({...value, snapshot: {...value.snapshot, selectedIds: ['a']}}, <FixNode />));
    expect(button('fix_node')).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(button('fix_node'));
    await waitFor(() => expect(value.controller.pin).toHaveBeenLastCalledWith(['a'], false));
    result.rerender(view({...value, snapshot: {...value.snapshot, selectedIds: ['edge']}}, <FixNode />));
    expect(button('fix_node')).toBeDisabled();
});

test('keeps zoom directions, fitting and layout refresh on finite controller commands', async () => {
    const value = makeValue();
    render(view(value, <><ZoomGraph /><FitCenter /><RefreshGraph /></>));
    fireEvent.click(button('zoom_out'));
    fireEvent.click(button('zoom_in'));
    fireEvent.click(button('fit_center'));
    fireEvent.click(button('refresh_layout'));
    await waitFor(() => expect(value.controller.setLayout).toHaveBeenCalledWith(value.layout));
    expect(value.controller.zoomBy.mock.calls).toEqual([[0.8], [1.25]]);
    expect(value.controller.fitView).toHaveBeenCalledTimes(1);
});

test('detects isolated nodes from edges and restores the same connected node set', async () => {
    const value = makeValue();
    const result = render(view(value, <ZeroDegreeNode />));
    fireEvent.click(button('isolated_nodes'));
    await waitFor(() => expect(value.controller.hide).toHaveBeenLastCalledWith(['a', 'b'], true));
    const hiddenNodes = value.snapshot.data.nodes.map(node => node.id === 'alone' ? node
        : {...node, style: {visibility: 'hidden'}});
    result.rerender(view({...value, snapshot: {...value.snapshot,
        data: {...value.snapshot.data, nodes: hiddenNodes}}}, <ZeroDegreeNode />));
    expect(button('isolated_nodes')).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(button('isolated_nodes'));
    await waitFor(() => expect(value.controller.hide).toHaveBeenLastCalledWith(['a', 'b'], false));
});

test('controls minimap per instance and releases the old instance when switching', async () => {
    const first = makeValue();
    const second = makeValue();
    const result = render(view(first, <MiniMap />));
    await waitFor(() => expect(first.controller.setMinimap).toHaveBeenCalledWith(true));
    result.rerender(view(second, <MiniMap />));
    await waitFor(() => expect(second.controller.setMinimap).toHaveBeenCalledWith(true));
    expect(first.controller.setMinimap.mock.calls).toEqual([[true], [false]]);
    result.unmount();
    await waitFor(() => expect(second.controller.setMinimap.mock.calls).toEqual([[true], [false]]));
});

test('clears a new dataset only after confirmation and notifies after the command applies', async () => {
    const value = makeValue();
    const onChange = jest.fn();
    render(view(value, <ClearGraph enable onChange={onChange} />));
    fireEvent.click(button('clear_canvas'));
    expect(value.controller.replaceData).not.toHaveBeenCalled();
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', {
        name: 'analysis.canvas.clear_graph.ok',
    }));
    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    expect(value.controller.replaceData).toHaveBeenCalledWith({nodes: [], edges: []}, {newDataset: true});
});

test('does not notify a replacement instance after an old clear resolves', async () => {
    const first = makeValue();
    let resolve;
    first.controller.replaceData.mockImplementation(() => new Promise(done => { resolve = done; }));
    const onChange = jest.fn();
    const result = render(view(first, <ClearGraph enable onChange={onChange} />));
    fireEvent.click(button('clear_canvas'));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', {
        name: 'analysis.canvas.clear_graph.ok',
    }));
    await waitFor(() => expect(first.controller.replaceData).toHaveBeenCalled());
    result.rerender(view(makeValue(), <ClearGraph enable onChange={onChange} />));
    await act(async () => resolve({status: 'applied'}));
    expect(onChange).not.toHaveBeenCalled();
});

test('disables commands before ready and handles command rejection through run', async () => {
    const value = makeValue();
    value.controller.zoomBy.mockRejectedValueOnce(new Error('disposed'));
    const result = render(view(value, <ZoomGraph />));
    fireEvent.click(button('zoom_in'));
    await waitFor(() => expect(value.report).toHaveBeenCalledWith(expect.any(Error)));
    result.rerender(view({...value, snapshot: {...value.snapshot, status: 'updating'}},
        <><ZoomGraph /><FitCenter /><FixNode /><RefreshGraph /><RedoUndo /><ClearGraph enable />
            <ZeroDegreeNode /></>));
    screen.getAllByRole('button').forEach(control => expect(control).toBeDisabled());
});

test('renders toolbar content as React DOM without navigation or engine plugin wrappers', () => {
    render(<ToolBar hasPadding extra={[{key: 'fit', content: <FitCenter />}]} />);
    expect(button('fit_center')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
});


test('clear confirmation delegates to the Hubble projection owner before notifying the parent', async () => {
    const value = makeValue();
    value.clearGraph = jest.fn(async () => ({status: 'applied'}));
    const onChange = jest.fn();
    render(view(value, <ClearGraph enable onChange={onChange} />));
    fireEvent.click(button('clear_canvas'));
    expect(value.clearGraph).not.toHaveBeenCalled();
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', {
        name: 'analysis.canvas.clear_graph.ok',
    }));
    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    expect(value.clearGraph).toHaveBeenCalledTimes(1);
    expect(value.controller.replaceData).not.toHaveBeenCalled();
});
