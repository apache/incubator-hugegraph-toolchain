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
import IconSelect from './index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

test.each(['UserOutlined', 'user', 'unknown-saved-icon', '__proto__'])(
    'renders saved icon %s safely without rewriting the form value', value => {
        const onChange = jest.fn();
        const {container} = render(<IconSelect value={value} onChange={onChange} />);
        expect(screen.getByRole('textbox')).toHaveValue(value);
        expect(container.querySelector('svg[data-icon="user"]')).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    }
);

test('selects a supported icon and resets safely when the parent clears it', async () => {
    const onChange = jest.fn();
    const {rerender, container} = render(<IconSelect value='user' onChange={onChange} />);
    await userEvent.click(screen.getByRole('textbox'));
    await userEvent.click(await screen.findByRole('button', {name: 'home', exact: true}));
    expect(onChange).toHaveBeenCalledWith('HomeOutlined');
    rerender(<IconSelect value={undefined} onChange={onChange} />);
    expect(container.querySelector('svg[data-icon="user"]')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveValue('');
});
