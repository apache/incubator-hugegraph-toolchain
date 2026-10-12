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

import {useCallback, useState} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import KeyboardAction from './index';

const ExpandableAction = () => {
    const [expanded, setExpanded] = useState(false);
    const toggle = useCallback(() => setExpanded(value => !value), []);

    return (
        <KeyboardAction
            aria-expanded={expanded}
            onAction={toggle}
        >
            Toggle details
        </KeyboardAction>
    );
};

test('exposes state and supports Enter and Space activation', async () => {
    render(<ExpandableAction />);

    const action = screen.getByRole('button', {name: 'Toggle details'});
    expect(action).toHaveAttribute('aria-expanded', 'false');

    action.focus();
    await userEvent.keyboard('{Enter}');
    expect(action).toHaveAttribute('aria-expanded', 'true');

    await userEvent.keyboard(' ');
    expect(action).toHaveAttribute('aria-expanded', 'false');
});
