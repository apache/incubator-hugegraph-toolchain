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
import AlgorithmPanel, {AlgorithmPanelGroup} from './index';
import {AlgorithmPersistenceContext} from '../algorithmFormPersistence';

const Panel = ({name, title}) => (
    <AlgorithmPersistenceContext value={name}>
        <AlgorithmPanel title={title}><input aria-label={`${title} source`} /></AlgorithmPanel>
    </AlgorithmPersistenceContext>
);

test('coordinates one expanded algorithm per group and preserves form drafts', async () => {
    render(<AlgorithmPanelGroup>
        <Panel name='shortestpath' title='Shortest path' />
        <Panel name='kout' title='K-out' />
    </AlgorithmPanelGroup>);
    const shortest = screen.getByRole('button', {name: /Shortest path/});
    const kout = screen.getByRole('button', {name: /K-out/});
    expect(shortest).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(shortest);
    await userEvent.type(await screen.findByRole('textbox', {name: 'Shortest path source'}), 'vertex-1');
    await userEvent.click(kout);
    expect(kout).toHaveAttribute('aria-expanded', 'true');
    expect(shortest).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(shortest);
    expect(kout).toHaveAttribute('aria-expanded', 'false');
    await waitFor(() => expect(screen.getByRole('textbox', {name: 'Shortest path source'})).toHaveValue('vertex-1'));
});

test('keeps separate algorithm groups independent', async () => {
    render(<>
        <AlgorithmPanelGroup><Panel name='shortestpath' title='First group' /></AlgorithmPanelGroup>
        <AlgorithmPanelGroup><Panel name='kout' title='Second group' /></AlgorithmPanelGroup>
    </>);
    await userEvent.click(screen.getByRole('button', {name: /First group/}));
    await userEvent.click(screen.getByRole('button', {name: /Second group/}));
    expect(screen.getByRole('button', {name: /First group/})).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('button', {name: /Second group/})).toHaveAttribute('aria-expanded', 'true');
});

test('mounts validation fields eagerly when an algorithm requests forceRender', () => {
    render(<AlgorithmPersistenceContext value='pagerank'>
        <AlgorithmPanel title='PageRank' forceRender><input aria-label='PageRank field' /></AlgorithmPanel>
    </AlgorithmPersistenceContext>);
    expect(screen.getByLabelText('PageRank field')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: /PageRank/})).toHaveAttribute('aria-expanded', 'false');
});
