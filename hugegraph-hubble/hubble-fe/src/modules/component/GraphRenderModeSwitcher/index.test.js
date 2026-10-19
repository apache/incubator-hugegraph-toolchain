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

import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RenderModeSwitcher from '.';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

const mode = 'analysis.canvas.render_mode';
const experimental3D = 'analysis.canvas.mode_3d_experimental';
const mode2D = 'analysis.canvas.mode_2d';

test('defaults to 2D and supports explicit experimental 3D selection and returning to 2D', async () => {
    const user = userEvent.setup();
    const onClick = jest.fn();
    const {rerender} = render(<RenderModeSwitcher buttonEnable onClick={onClick} />);

    expect(screen.getByText(mode2D)).toBeInTheDocument();
    await user.click(screen.getByRole('combobox', {name: mode}));
    await user.click(await screen.findByText(experimental3D, {selector: '.ant-select-item-option-content'}));
    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onClick).toHaveBeenCalledWith(GRAPH_RENDER_MODE.CANVAS3D);
    rerender(<RenderModeSwitcher buttonEnable onClick={onClick} value={GRAPH_RENDER_MODE.CANVAS3D} />);
    await waitFor(() => expect(screen.getByRole('combobox', {name: mode})).toHaveAttribute('aria-expanded', 'false'));
    await user.click(screen.getByRole('combobox', {name: mode}));
    await user.click(await screen.findByText(mode2D, {selector: '.ant-select-item-option-content'}));
    expect(onClick).toHaveBeenLastCalledWith(GRAPH_RENDER_MODE.CANVAS2D);
});

test('disables mode choices while the canvas is unavailable', async () => {
    const user = userEvent.setup();
    const onClick = jest.fn();
    render(<RenderModeSwitcher buttonEnable={false} onClick={onClick} />);

    const input = screen.getByRole('combobox', {name: mode});
    expect(input).toBeDisabled();
    await user.click(input);
    expect(input).toHaveAttribute('aria-expanded', 'false');
    expect(onClick).not.toHaveBeenCalled();
});
