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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {InputColorSelect} from './index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

test('opens the actual picker, edits hex and closes with focus recovery', async () => {
    const onChange = jest.fn();
    render(<InputColorSelect value='#5c73e6' onChange={onChange} />);
    const trigger = screen.getByRole('button', {name: 'style_config.color_picker'});
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    trigger.focus();
    await userEvent.keyboard('{Enter}');
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    const hex = await screen.findByDisplayValue('5c73e6');
    fireEvent.change(hex, {target: {value: '123456'}});
    fireEvent.blur(hex);
    await waitFor(() => expect(onChange).toHaveBeenCalledWith('#123456'));
    await userEvent.keyboard('{Escape}');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(trigger).toHaveFocus();
});

test('disables the trigger and accepts external color updates', () => {
    const {rerender} = render(<InputColorSelect value='#123456' disable />);
    const trigger = screen.getByRole('button', {name: 'style_config.color_picker'});
    expect(trigger).toBeDisabled();
    expect(trigger).toHaveStyle({background: '#123456'});
    rerender(<InputColorSelect value='#654321' />);
    expect(trigger).toBeEnabled();
    expect(trigger).toHaveStyle({background: '#654321'});
});
