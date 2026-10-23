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

import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GraphSpaceCard from './Card';

const mockNavigate = jest.fn();

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: (key, options) => key === 'graphspace.unit.inherited'
        ? `Inherited (${options.value})` : key}),
}));

jest.mock('react-router-dom', () => {
    const router = jest.requireActual('react-router-dom');
    const React = require('react');

    return {
        ...router,
        Link: ({children, to}) => React.createElement('a', {href: to}, children),
        useNavigate: () => mockNavigate,
    };
});

beforeEach(() => {
    mockNavigate.mockClear();
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
    }));
});

test('opens the GraphSpace from header whitespace, creation date, body and enter button once', async () => {
    const {container} = render(<GraphSpaceCard item={{
        name: 'space', nickname: 'Space', create_time: '2026-10-23',
    }} />);
    for (const target of [
        container.querySelector('.ant-card-head'),
        screen.getByText(/2026-10-23 graphspace.card.created/),
        screen.getByRole('button', {name: 'Space'}),
        container.querySelector('.card_content'),
        screen.getByRole('button', {name: 'graphspace.card.enter'}),
    ]) {
        mockNavigate.mockClear();
        await userEvent.click(target);
        expect(mockNavigate).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith('/graphspace/space');
    }
    mockNavigate.mockClear();
    fireEvent.keyDown(screen.getByRole('button', {name: 'Space'}), {key: 'Enter'});
    expect(mockNavigate).toHaveBeenCalledTimes(1);
});

test('does not navigate when opening the menu or editing through its portal', async () => {
    const item = {name: 'space', nickname: 'Space'};
    const editGraphspace = jest.fn();
    render(<GraphSpaceCard item={item} editGraphspace={editGraphspace} />);
    await userEvent.click(screen.getByRole('button', {name: 'common.action.more'}));
    await userEvent.click(await screen.findByText('common.action.edit'));
    expect(editGraphspace).toHaveBeenCalledWith(item);
    expect(mockNavigate).not.toHaveBeenCalled();
});

test('keeps statistics help available through icon clicks and the keyboard without navigating', async () => {
    render(<GraphSpaceCard item={{
        name: 'space', nickname: 'Space',
        statistic: {vertex: 12, edge: 3, date: '2026-10-23'},
    }} />);
    const help = screen.getByLabelText('graphspace.card.statistic_help');
    for (const target of [help.querySelector('span'), help.querySelector('svg')]) {
        await userEvent.click(target);
        expect(mockNavigate).not.toHaveBeenCalled();
    }
    expect(help).toHaveAttribute('type', 'button');
    await userEvent.unhover(help);
    await userEvent.tab();
    await userEvent.tab({shift: true});
    expect(help).toHaveFocus();
    expect(await screen.findByRole('tooltip')).toHaveTextContent('graphspace.card.daily_update');
    expect(screen.getByRole('tooltip')).toHaveTextContent('graphspace.card.last_update');
    await userEvent.keyboard('{Enter} ');
    expect(mockNavigate).not.toHaveBeenCalled();
});

test('does not expose a default GraphSpace mutation action', async () => {
    const handleSetDefault = jest.fn();
    const item = {
        name: 'DEFAULT',
        nickname: 'Default',
        create_time: '2026-07-10',
        default: false,
        auth: false,
        max_graph_number: 10,
        cpu_limit: 2,
        memory_limit: 4,
        storage_limit: 100,
        storage_used: 1,
        storage_percent: 0.01,
        statistic: {vertex: 0, edge: 0, date: '2026-07-10'},
    };

    render(
        <GraphSpaceCard
            item={item}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleSetDefault={handleSetDefault}
            handleInit={jest.fn()}
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'common.action.more'}));
    await screen.findByText('common.action.schema_manage');
    expect(screen.queryByText('common.action.set_default')).not.toBeInTheDocument();
    expect(handleSetDefault).not.toHaveBeenCalled();
    expect(screen.getByText('graphspace.default_name')).toBeInTheDocument();
    expect(screen.queryByText('Default')).not.toBeInTheDocument();
    expect(screen.getByText('graphspace.card.daily_snapshot')).toBeInTheDocument();
    expect(screen.getByLabelText('graphspace.card.statistic_help')).toBeInTheDocument();
    expect(screen.getByText('graphspace.card.vertex: 0')).toBeInTheDocument();
    expect(screen.getByText('graphspace.card.edge: 0')).toBeInTheDocument();
});

test('ships a localized default GraphSpace name in both languages', () => {
    const zh = require('../../i18n/resources/zh-CN/modules/pages.json');
    const en = require('../../i18n/resources/en-US/modules/pages.json');

    expect(zh.graphspace.default_name).toBe('默认图空间');
    expect(en.graphspace.default_name).toBe('Default GraphSpace');
});

test('shows nonzero precise usage when the legacy ratio is rounded to zero', () => {
    render(<GraphSpaceCard item={{
        name: 'small', storage_used_kb: 1024, storage_used: 0,
        storage_limit: 10240, storage_percent: 0,
    }} />);

    expect(screen.getByText('<0.01%')).toBeInTheDocument();
    expect(screen.getByText(/1 MB/)).toBeInTheDocument();
    expect(screen.getByText('1 MB / 10240 GB')).toBeInTheDocument();
});

test.each([
    [600 * 1024, '99.99%'],
    [512 * 1024, '<100%'],
    [1, '<100%'],
])('keeps %s KB of remaining quota visibly below full', (remainingKb, expected) => {
    const {container} = render(<GraphSpaceCard item={{
        name: 'nearly-full', storage_limit: 10240,
        storage_used_kb: 10240 * 1024 * 1024 - remainingKb,
    }} />);

    expect(screen.getByText(expected)).toBeInTheDocument();
    expect(screen.queryByText('100%')).not.toBeInTheDocument();
    expect(container.querySelector('.ant-progress-status-normal')).toBeInTheDocument();
});

test.each([0, 1024])('keeps full or exceeded quota in the exception state (%s KB over)', excessKb => {
    const {container} = render(<GraphSpaceCard item={{
        name: 'full', storage_limit: 10240,
        storage_used_kb: 10240 * 1024 * 1024 + excessKb,
    }} />);

    expect(screen.queryByText('<100%')).not.toBeInTheDocument();
    expect(container.querySelector('.ant-progress-status-exception')).toBeInTheDocument();
    expect(screen.getByRole('img', {name: 'info'})).toBeInTheDocument();
});

test('omits unavailable totals without claiming that no snapshot exists', () => {
    const item = {
        name: 'empty',
        nickname: 'Empty',
        statistic: {},
        create_time: '2026-07-10',
        auth: false,
        max_graph_number: 10,
        cpu_limit: 2,
        memory_limit: 4,
        storage_limit: 100,
        storage_used: 0,
        storage_percent: 0,
    };

    render(
        <GraphSpaceCard
            item={item}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleInit={jest.fn()}
        />
    );

    expect(screen.queryByText(/^graphspace.card.vertex:/)).not.toBeInTheDocument();
    expect(screen.queryByText(/^graphspace.card.edge:/)).not.toBeInTheDocument();
    expect(screen.queryByText('graph.detail.not_collected')).not.toBeInTheDocument();
    expect(screen.getByTitle('Empty')).toBeInTheDocument();
    expect(screen.getByText(/graphspace.card.max_graph/)).toHaveTextContent('10');
});

test('treats a backend nickname echo as an unset alias', () => {
    const item = {
        name: 'space', nickname: 'space', create_time: '2026-07-10', auth: false,
        max_graph_number: 10, cpu_limit: 2, memory_limit: 4,
        storage_limit: 100, storage_used: 0, storage_percent: 0,
    };
    render(
        <GraphSpaceCard
            item={item}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleInit={jest.fn()}
        />
    );

    expect(screen.getByTitle('space')).toBeInTheDocument();
});

test('keeps a long GraphSpace title accessible without a fixed text width', () => {
    const displayName = 'A GraphSpace name that is much longer than its card';
    const item = {
        name: 'long-space', nickname: displayName, create_time: '2026-07-10',
        auth: false, max_graph_number: 10, cpu_limit: 2, memory_limit: 4,
        storage_limit: 100, storage_used: 0, storage_percent: 0,
    };

    render(
        <GraphSpaceCard
            item={item}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleInit={jest.fn()}
        />
    );

    const title = screen.getByRole('button', {name: displayName});
    expect(title).toHaveAttribute('title', displayName);
    expect(title).toHaveClass('card_title_text');
    expect(title).not.toHaveStyle({maxWidth: '244px'});
    expect(screen.getAllByText('graphspace.card.enter').length).toBeGreaterThan(0);
});

test('keeps public GraphSpace navigation but hides mutation actions for viewers', async () => {
    const item = {
        name: 'public', nickname: 'Public', create_time: '2026-07-10',
        auth: false, max_graph_number: 10, cpu_limit: 2, memory_limit: 4,
        storage_limit: 100, storage_used: 1, storage_percent: 0.01,
        statistic: {vertex: 0, edge: 0},
    };

    render(
        <GraphSpaceCard
            item={item}
            canUpdate={false}
            canDelete={false}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleInit={jest.fn()}
        />
    );

    expect(screen.getAllByText('graphspace.card.enter').length).toBeGreaterThan(0);
    await userEvent.click(screen.getByRole('button', {name: 'common.action.more'}));
    expect(await screen.findByText('common.action.schema_manage')).toBeInTheDocument();
    expect(screen.queryByText('common.action.edit')).not.toBeInTheDocument();
    expect(screen.queryByText('common.action.delete')).not.toBeInTheDocument();
});

test('keeps GraphSpace update and delete actions independently gated', async () => {
    const item = {
        name: 'managed', nickname: 'Managed', create_time: '2026-07-10',
        auth: false, max_graph_number: 10, cpu_limit: 2, memory_limit: 4,
        storage_limit: 100, storage_used: 1, storage_percent: 0.01,
        statistic: {vertex: 0, edge: 0},
    };

    render(
        <GraphSpaceCard
            item={item}
            canUpdate
            canDelete={false}
            editGraphspace={jest.fn()}
            deleteGraphspace={jest.fn()}
            handleInit={jest.fn()}
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'common.action.more'}));
    expect(await screen.findByText('common.action.edit')).toBeInTheDocument();
    expect(screen.queryByText('common.action.delete')).not.toBeInTheDocument();
});

test('shows inherited compute resources with their current effective limits', () => {
    render(<GraphSpaceCard item={{
        name: 'inherited', cpu_limit: 4, memory_limit: 8,
        compute_cpu_limit: 0, compute_memory_limit: 0,
        storage_limit: 10240, storage_used_kb: 0,
    }} />);
    expect(screen.getByText(/Inherited \(4graphspace.unit.cpu\)/)).toBeInTheDocument();
    expect(screen.getByText(/Inherited \(8graphspace.unit.memory\)/)).toBeInTheDocument();
});

test.each([1024, undefined])('keeps unlimited usage visible without a percentage (%s KB)', usedKb => {
    const {container} = render(<GraphSpaceCard item={{
        name: 'unlimited', storage_limit: 2147483647, storage_used_kb: usedKb,
        storage_used: 1, storage_percent: 1,
    }} />);
    expect(screen.getByText('--')).toBeInTheDocument();
    expect(screen.getByText(/graphspace.unit.unlimited/)).toBeInTheDocument();
    expect(screen.getByText(usedKb ? /1 MB/ : /1 GB/)).toBeInTheDocument();
    expect(screen.queryByText('<0.01%')).not.toBeInTheDocument();
    expect(container.querySelector('.ant-progress-status-exception')).not.toBeInTheDocument();
});
