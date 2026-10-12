/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fs from 'fs';
import path from 'path';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import GraphCard from './Card';
import {formatToGraphInData} from '../../utils/formatGraphInData';

const mockT = (key, values) => {
    if (key === 'graph.card.element_counts') {
        return `${key}:${values.vertices}/${values.edges}`;
    }
    if (key === 'graph.card.view_schema') {
        return `${key}:${values.graph}`;
    }
    return key;
};

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: mockT}),
}));

jest.mock('../../components/GraphinView', () => () => <div>graph preview</div>);

jest.mock('../../utils/formatGraphInData', () => ({
    formatToGraphInData: jest.fn(() => ({nodes: [], edges: []})),
}));

jest.mock('../../utils/config', () => ({
    isPdEnabled: () => true,
}));

beforeEach(() => {
    formatToGraphInData.mockReturnValue({nodes: [], edges: []});
});

const LocationProbe = () => <div data-testid='location'>{useLocation().pathname}</div>;

test('falls back to the graph name when a PD graph has no alias', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'hugegraph',
                    nickname: null,
                    graphspace: 'DEFAULT',
                    graphspace_nickname: '默认图空间',
                    default: true,
                    storage: 0,
                    create_time: '2026-07-12',
                }}
                menus={[]}
            />
        </MemoryRouter>
    );

    expect(screen.getByTitle('graphspace.default_name-hugegraph')).toBeInTheDocument();
    expect(screen.queryByTitle('graphspace.default_name-null')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'graph.card.more_actions'}))
        .toBeInTheDocument();
});

test('treats backend nickname echoes as unset aliases', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'movies', nickname: 'movies', graphspace: 'space',
                    graphspace_nickname: 'space', storage: 0,
                    create_time: '2026-07-12', schemaview: {vertices: [], edges: []},
                }}
                menus={[]}
            />
        </MemoryRouter>
    );

    expect(screen.getByTitle('space-movies')).toBeInTheDocument();
});

test('uses a compact actionable state when the graph has no schema', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'hugegraph',
                    graphspace: 'DEFAULT',
                    graphspace_nickname: 'Default',
                    storage: 0,
                    create_time: '2026-07-12',
                    schemaview: {vertices: [], edges: []},
                }}
                menus={[]}
            />
        </MemoryRouter>
    );

    expect(screen.getByText('graph.card.empty_schema')).toBeInTheDocument();
    expect(screen.getByText(/graph\.card\.schema_types/)).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'graph.card.open_schema'}))
        .toBeInTheDocument();
    expect(screen.queryByText('graph preview')).not.toBeInTheDocument();
    expect(formatToGraphInData).toHaveBeenCalledWith(
        {vertices: [], edges: []},
        true
    );
    expect(screen.getByText(/graph\.col\.create_time/).closest('[role="button"]'))
        .not.toBeInTheDocument();
});

test('labels an empty schema as view-only for a read-only account', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'hugegraph',
                    graphspace: 'space',
                    storage: 0,
                    schemaview: {vertices: [], edges: []},
                }}
                menus={[]}
                canMutate={false}
            />
        </MemoryRouter>
    );

    expect(screen.getByRole('button', {name: 'graph.menu.view_schema'}))
        .toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'graph.card.open_schema'}))
        .not.toBeInTheDocument();
});

test('ships the requested graph creation and schema labels', () => {
    const zh = require('../../i18n/resources/zh-CN/modules/pages.json');
    const en = require('../../i18n/resources/en-US/modules/pages.json');

    expect(zh.graph.create).toBe('新建图');
    expect(en.graph.create).toBe('New Graph');
    expect(zh.graph.form.title_create).toBe('新建图');
    expect(en.graph.form.title_create).toBe('New Graph');
    expect(zh.graph.form.schema).toBe('Graph Schema');
    expect(en.graph.form.schema).toBe('Graph Schema');
    expect(zh.graph.form.name_help).toContain(' / ');
    expect(en.graph.form.name_help).toContain(' / ');
    expect(zh.graph.card.view_schema).toBe('查看 {{graph}} 的 Schema');
    expect(en.graph.card.view_schema).toBe('View Schema for {{graph}}');
});

test('opens Schema from the graph preview and keeps Gremlin as the footer action', () => {
    formatToGraphInData.mockReturnValue({nodes: [{id: 'person'}], edges: []});
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'hugegraph',
                    nickname: 'HugeGraph',
                    graphspace: 'DEFAULT',
                    graphspace_nickname: 'Default',
                    storage: 1024,
                    create_time: '2026-07-12',
                    statistic: {vertex: 12, edge: 8},
                    schemaview: {vertices: [{name: 'person'}], edges: []},
                }}
                menus={[]}
            />
            <LocationProbe />
        </MemoryRouter>
    );

    const metadata = screen.getByText(/graph\.card\.storage/);
    expect(metadata).toHaveTextContent('graph.col.create_time');
    expect(screen.getByText(/graph\.card\.element_counts/)).toHaveTextContent('12');
    expect(screen.getByText(/graph\.card\.element_counts/)).toHaveTextContent('8');
    expect(screen.getByRole('link', {name: 'graph.card.overview'}))
        .toHaveAttribute('href', '/graphspace/DEFAULT/graph/hugegraph/detail');
    expect(screen.getByRole('link', {name: 'graph.card.query_graph'}))
        .toHaveAttribute('href', '/gremlin/DEFAULT/hugegraph');

    const schemaPreview = screen.getByRole('button', {
        name: 'graph.card.view_schema:HugeGraph',
    });
    expect(schemaPreview).toHaveAttribute('tabindex', '0');
    fireEvent.click(schemaPreview);
    expect(screen.getByTestId('location')).toHaveTextContent(
        '/graphspace/DEFAULT/graph/hugegraph/meta'
    );
});

test.each(['Enter', ' '])(
    'opens Schema from the named graph preview with the %p key',
    key => {
        formatToGraphInData.mockReturnValue({nodes: [{id: 'person'}], edges: []});
        render(
            <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
                <GraphCard
                    item={{
                        name: 'hugegraph',
                        nickname: 'HugeGraph',
                        graphspace: 'DEFAULT',
                        graphspace_nickname: 'Default',
                        storage: 1024,
                        create_time: '2026-07-12',
                        schemaview: {vertices: [{name: 'person'}], edges: []},
                    }}
                    menus={[]}
                />
                <LocationProbe />
            </MemoryRouter>
        );

        const schemaPreview = screen.getByRole('button', {
            name: 'graph.card.view_schema:HugeGraph',
        });
        fireEvent.keyDown(schemaPreview, {key});

        expect(screen.getByTestId('location')).toHaveTextContent(
            '/graphspace/DEFAULT/graph/hugegraph/meta'
        );
    }
);

test('gives the keyboard-focusable schema preview a visible design-system focus ring', () => {
    const stylesheet = fs.readFileSync(path.join(__dirname, 'index.module.scss'), 'utf8');
    const previewRule = stylesheet.match(
        /\.card_content\s*\{[\s\S]*?&:focus-visible\s*\{([^}]*)\}/
    )?.[1] || '';

    expect(previewRule).toContain('outline: 2px solid #1890ff');
    expect(previewRule).toContain('outline-offset: -2px');
});

test('does not invent point and edge counts when the list API omits them', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <GraphCard
                item={{
                    name: 'hugegraph',
                    graphspace: 'DEFAULT',
                    graphspace_nickname: 'Default',
                    storage: 0,
                    create_time: '2026-07-12',
                    schemaview: {vertices: [], edges: []},
                }}
                menus={[]}
            />
        </MemoryRouter>
    );

    expect(screen.queryByText(/graph\.card\.element_counts/)).not.toBeInTheDocument();
});
