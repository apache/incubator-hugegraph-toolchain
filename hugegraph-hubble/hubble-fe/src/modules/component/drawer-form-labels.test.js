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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {render, screen, within} from '@testing-library/react';
import DynamicAddNode from './DynamicAddNode';
import DynamicAddEdge from './DynamicAddEdge';
import GraphAnalysisContext from '../Context';

jest.mock('react-i18next', () => {
    const t = key => key;
    return {...jest.requireActual('react-i18next'), useTranslation: () => ({t})};
});
jest.mock('../../api', () => ({analysis: {fetchVertexlinks: jest.fn(), fetchEdgeLabels: jest.fn()}}));

const label = key => `analysis.canvas.dynamic_add.${key}`;
const drawers = (nodeOpen, edgeOpen) => (
    <GraphAnalysisContext.Provider value={{graphSpace: 'space', graph: 'graph'}}>
        <DynamicAddNode open={nodeOpen} drawerInfo={[]} />
        <DynamicAddEdge open={edgeOpen} graphData={{vertices: [], edges: []}} isClickNew isOutEdge />
    </GraphAnalysisContext.Provider>
);

beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});

test.each(['vertex', 'edge'])('associates labels with the active form after closing the %s drawer', async first => {
    const nodeFirst = first === 'vertex';
    const view = render(drawers(nodeFirst, !nodeFirst));
    view.rerender(drawers(!nodeFirst, nodeFirst));
    // AntD uses a constant drawer title ID in Jest; locate its own title before
    // checking the real form label associations, which are the regression here.
    const title = await screen.findByText(label(nodeFirst ? 'add_out_edge' : 'add_vertex'),
        {selector: '.ant-drawer-title'});
    const active = title.closest('[role="dialog"]');
    const input = within(active).getByLabelText(new RegExp(label(nodeFirst ? 'edge_type' : 'vertex_type')));
    expect(active).toContainElement(input);
    expect(input).toHaveAttribute('role', 'combobox');
    expect(input).toHaveAccessibleName(new RegExp(label(nodeFirst ? 'edge_type' : 'vertex_type')));
});

test('keeps labels associated with their own graph instance', () => {
    render(<>{drawers(true, false)}{drawers(true, false)}</>);
    const dialogs = screen.getAllByRole('dialog', {name: label('add_vertex')});
    const inputs = dialogs.map(dialog => within(dialog).getByLabelText(new RegExp(label('vertex_type'))));
    expect(inputs[0]).not.toBe(inputs[1]);
    dialogs.forEach((dialog, index) => expect(dialog).toContainElement(inputs[index]));
});
