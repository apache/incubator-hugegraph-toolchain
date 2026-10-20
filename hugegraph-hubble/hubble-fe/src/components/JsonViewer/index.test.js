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
import JSONbig from 'json-bigint';
import JsonViewer from './index';

test('escapes keys and values, and hides collapsed descendants', async () => {
    const {container} = render(<JsonViewer value={{'<script>': {text: '<img src=x onerror=alert(1)>'}}} />);
    expect(screen.getByText('"<img src=x onerror=alert(1)>"')).toBeVisible();
    expect(container.querySelector('img, script')).toBeNull();
    const root = container.querySelector('details');
    await userEvent.click(root.querySelector('summary'));
    await waitFor(() => expect(root).not.toHaveAttribute('open'));
    expect(screen.queryByText('"<img src=x onerror=alert(1)>"')).not.toBeInTheDocument();
    await userEvent.click(root.querySelector('summary'));
    expect(await screen.findByText('"<img src=x onerror=alert(1)>"')).toBeVisible();
});

test('groups long arrays without losing any records', async () => {
    render(<JsonViewer value={Array.from({length: 51}, (_, i) => `record-${i}`)} />);
    const group = screen.getByText('[50 … 50]');
    expect(screen.queryByText('"record-50"')).not.toBeInTheDocument();
    await userEvent.click(group);
    expect(await screen.findByText('"record-50"')).toBeVisible();
});

test('preserves large numeric values when rendering and copying', async () => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {configurable: true, value: {writeText}});
    const large = '9223372036854775807';
    const value = JSONbig.parse(`{"id":${large}}`);
    render(<JsonViewer value={{...value, native: BigInt(large)}} />);
    expect(screen.getAllByText(large)).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', {name: /copy/i}));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(
        `{\n  "id": ${large},\n  "native": ${large}\n}`
    ));
});
