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

import React from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import ResizeObserver from 'resize-observer-polyfill';
import ResultsLayout from './index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('resize-observer-polyfill', () => jest.fn());

let resize;
let measuredHeight;
let disconnect;

beforeEach(() => {
    measuredHeight = 560;
    disconnect = jest.fn();
    jest.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => ({
        height: measuredHeight,
    }));
    ResizeObserver.mockImplementation(callback => {
        resize = callback;
        return {observe: jest.fn(), disconnect};
    });
});

afterEach(() => jest.restoreAllMocks());

const renderStatefulResult = height => <div data-height={height}><input aria-label='result state' /></div>;
const renderHeight = height => <output>{height}</output>;

it('redistributes the same space without remounting either pane', () => {
    const view = render(
        <ResultsLayout
            renderResult={renderStatefulResult}
            history={<input aria-label='history state' />}
        />
    );
    const result = screen.getByRole('textbox', {name: 'result state'});
    const history = screen.getByRole('textbox', {name: 'history state'});
    fireEvent.change(result, {target: {value: 'selected graph'}});
    fireEvent.change(history, {target: {value: 'search'}});
    const divider = screen.getByRole('separator');
    const before = Number(divider.getAttribute('aria-valuenow'));
    expect(before).toBe(336);
    fireEvent.keyDown(divider, {key: 'ArrowDown'});
    const after = Number(divider.getAttribute('aria-valuenow'));
    expect(after).toBeGreaterThan(before);
    expect(view.container.firstChild.style.gridTemplateRows).toBe(`${after}px 14px minmax(0, 1fr)`);
    expect(screen.getByRole('textbox', {name: 'result state'})).toBe(result);
    expect(result).toHaveValue('selected graph');
    expect(history).toHaveValue('search');

    fireEvent.keyDown(divider, {key: 'End'});
    expect(divider).toHaveAttribute('aria-valuenow', '396');
    fireEvent.keyDown(divider, {key: 'Home'});
    expect(divider).toHaveAttribute('aria-valuenow', '240');
    view.unmount();
    expect(disconnect).toHaveBeenCalledTimes(1);
});

it('clamps a resized result when the viewport budget shrinks', () => {
    render(<ResultsLayout renderResult={renderHeight} history={<div>history</div>} />);
    const divider = screen.getByRole('separator');
    fireEvent.keyDown(divider, {key: 'End'});
    measuredHeight = 440;
    act(() => resize());
    expect(divider).toHaveAttribute('aria-valuenow', '276');
    expect(divider).toHaveAttribute('aria-valuemax', '276');
});
