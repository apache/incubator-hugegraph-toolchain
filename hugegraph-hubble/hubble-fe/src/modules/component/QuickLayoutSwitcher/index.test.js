/*
 *
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

import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QuickLayoutSwitcher from '.';
import {analysisLayout} from '../../../utils/analysisGraphPresentation';
import {ALGORITHM_NAME, Algorithm_Layout} from '../../../utils/constants';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

test.each(['d3-force', 'grid', 'circular', undefined])('offers radial from %s without mislabeling the current layout', async type => {
    const onClick = jest.fn();
    render(<QuickLayoutSwitcher buttonEnable layout={type ? {type, options: {kept: true}} : undefined}
        onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'}));
    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onClick).toHaveBeenCalledWith({type: 'radial'});
});

test('returns from radial to the default force selection without replaying prior options', async () => {
    const onClick = jest.fn();
    render(<QuickLayoutSwitcher buttonEnable layout={{type: 'radial', options: {focusNode: 'previous'}}}
        onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_force'}));
    expect(onClick).toHaveBeenCalledWith({type: 'd3-force'});
});

test('does not change layouts while disabled', async () => {
    const onClick = jest.fn();
    render(<QuickLayoutSwitcher buttonEnable={false} layout={{type: 'radial'}} onClick={onClick} />);
    const button = screen.getByRole('button', {name: 'analysis.canvas.quick_layout_force'});
    expect(button).toBeDisabled();
    await userEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
});

test.each([ALGORITHM_NAME.K_OUT, ALGORITHM_NAME.K_NEIGHBOR].flatMap(algorithmName =>
    [0, '0', 900719925474099312345n].map(startId => [algorithmName, startId])))(
    'restores %s radial focus for typed start vertex %s after force', async (algorithmName, startId) => {
    const defaultLayout = analysisLayout({layout: Algorithm_Layout[algorithmName], startId});
    const onClick = jest.fn();
    const view = render(<QuickLayoutSwitcher buttonEnable layout={defaultLayout} defaultLayout={defaultLayout}
        startId={startId} onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_force'}));
    expect(onClick).toHaveBeenLastCalledWith({type: 'd3-force'});
    view.rerender(<QuickLayoutSwitcher buttonEnable layout={onClick.mock.lastCall[0]} defaultLayout={defaultLayout}
        startId={startId} onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'}));
    expect(onClick).toHaveBeenLastCalledWith(defaultLayout);
});

test.each([ALGORITHM_NAME.SHORTEST_PATH, ALGORITHM_NAME.SAME_NEIGHBORS,
    ALGORITHM_NAME.ALLPATHS, ALGORITHM_NAME.CUSTOMIZEDPATHS])(
    'can return to the existing %s algorithm default after radial', async algorithmName => {
    const defaultLayout = analysisLayout({layout: Algorithm_Layout[algorithmName]});
    const onClick = jest.fn();
    const view = render(<QuickLayoutSwitcher buttonEnable layout={defaultLayout} defaultLayout={defaultLayout}
        onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_radial'}));
    expect(onClick).toHaveBeenLastCalledWith({type: 'radial'});
    view.rerender(<QuickLayoutSwitcher buttonEnable layout={{type: 'radial'}} defaultLayout={defaultLayout}
        onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', {name: 'analysis.canvas.quick_layout_default'}));
    expect(onClick).toHaveBeenLastCalledWith(defaultLayout);
});
