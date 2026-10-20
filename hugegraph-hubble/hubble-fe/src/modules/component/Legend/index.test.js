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
import Legend from './index';
import {GraphContext} from '../Context';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
const fixture = () => ({controller: {highlight: jest.fn(async () => ({})), hide: jest.fn(async () => ({}))},
    snapshot: {status: 'ready', data: {nodes: [{id: 'a', data: {legend: '<person>'}},
        {id: 'b', data: {legend: '<person>'}}, {id: 'c', data: {legend: 'company'}}],
    edges: [{id: 'e', data: {legend: 'knows'}}]}}, run: operation => Promise.resolve().then(operation)});

test('groups only corresponding elements, uses safe text, and isolates instances', async () => {
    const first = fixture();
    const second = fixture();
    const result = render(<>
        <GraphContext.Provider value={first}><section aria-label="first"><Legend /></section></GraphContext.Provider>
        <GraphContext.Provider value={second}><section aria-label="second"><Legend /></section></GraphContext.Provider>
    </>);
    const label = within(screen.getByRole('region', {name: 'first'})).getByRole('button', {name: '<person>'});
    fireEvent.pointerEnter(label);
    fireEvent.click(label);
    await waitFor(() => expect(first.controller.hide).toHaveBeenCalledWith(['a', 'b'], true));
    expect(first.controller.highlight).toHaveBeenCalledWith(['a', 'b']);
    expect(second.controller.hide).not.toHaveBeenCalled();
    expect(second.controller.highlight).not.toHaveBeenCalled();
    expect(label.querySelector('person')).toBeNull();
    fireEvent.pointerLeave(label);
    await waitFor(() => expect(first.controller.highlight).toHaveBeenLastCalledWith([]));
    result.unmount();
    await waitFor(() => expect(second.controller.highlight).toHaveBeenLastCalledWith([]));
});

test('restores hidden group and disables commands while updating', async () => {
    const value = fixture();
    value.snapshot.data.edges[0].style = {visibility: 'hidden'};
    const result = render(<GraphContext.Provider value={value}><Legend /></GraphContext.Provider>);
    const label = screen.getByRole('button', {name: 'knows'});
    expect(label).toHaveAttribute('aria-pressed', 'false');
    fireEvent.click(label);
    await waitFor(() => expect(value.controller.hide).toHaveBeenCalledWith(['e'], false));
    result.rerender(<GraphContext.Provider value={{...value, snapshot: {...value.snapshot, status: 'updating'}}}>
        <Legend />
    </GraphContext.Provider>);
    screen.getAllByRole('button').forEach(button => expect(button).toBeDisabled());
});
