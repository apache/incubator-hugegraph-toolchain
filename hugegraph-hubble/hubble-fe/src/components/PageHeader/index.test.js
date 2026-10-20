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
import PageHeader from './index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

test('exposes a heading, back action and page-specific controls', async () => {
    const onBack = jest.fn();
    render(<PageHeader title='Schema' subTitle='Model' onBack={onBack}
        extra={<button>New label</button>}><input aria-label='Search labels' /></PageHeader>);
    expect(screen.getByRole('heading', {name: 'Schema', level: 2})).toBeVisible();
    expect(screen.getByRole('button', {name: 'New label'})).toBeVisible();
    expect(screen.getByRole('textbox', {name: 'Search labels'})).toBeVisible();
    await userEvent.click(screen.getByRole('button', {name: 'common.action.back'}));
    expect(onBack).toHaveBeenCalledTimes(1);
});
