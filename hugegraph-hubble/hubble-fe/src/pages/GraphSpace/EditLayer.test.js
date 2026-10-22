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
import {EditLayer} from './EditLayer';
import * as api from '../../api';
import {isAuthEnabled} from '../../utils/config';

jest.mock('react-i18next', () => {
    const t = (key, options) => key === 'graphspace.unit.inherited'
        ? `Inherited (${options.value})` : key;
    return {
        initReactI18next: {type: '3rdParty', init: jest.fn()},
        useTranslation: () => ({t}),
    };
});

jest.mock('../../api', () => ({
    auth: {getUserList: jest.fn()},
    manage: {
        addGraphSpace: jest.fn(),
        getGraphSpace: jest.fn(),
        updateGraphSpace: jest.fn(),
    },
}));

jest.mock('../../utils/config', () => ({
    isAuthEnabled: jest.fn(),
}));

beforeEach(() => {
    jest.clearAllMocks();
    isAuthEnabled.mockReturnValue(true);
    api.auth.getUserList.mockReturnValue(new Promise(() => {}));
    api.manage.addGraphSpace.mockResolvedValue({status: 200});
    api.manage.updateGraphSpace.mockResolvedValue({status: 200});
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

test('uses the shared keyboard-focusable help affordance for every field label', () => {
    render(
        <EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />
    );

    const nameHelp = screen.getByRole('img', {name: /graphspace\.form\.id_help/});
    expect(nameHelp).toHaveAttribute('tabindex', '0');
    expect(nameHelp).toHaveAttribute('role', 'img');
});

test('reserves enough label width for long localized labels', () => {
    render(
        <EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />
    );

    const form = document.querySelector('.graphspace-edit-form');
    const labelColumn = form.querySelector('.ant-form-item-label');
    const controlColumn = form.querySelector('.ant-form-item-control');

    expect(labelColumn).toHaveStyle({flex: '0 0 200px'});
    expect(controlColumn).toHaveStyle({flex: '1 1 0'});
});

test('does not expose per-GraphSpace authentication controls', () => {
    render(
        <EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />
    );

    expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    expect(screen.queryByText('graphspace.form.auth')).not.toBeInTheDocument();
});

test('creates a GraphSpace with visible resource defaults', async () => {
    render(
        <EditLayer
            visible
            detail={{}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );
    await waitFor(() => expect(api.auth.getUserList).toHaveBeenCalled());

    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'demo_space'},
    });
    fireEvent.change(screen.getByPlaceholderText('graphspace.form.name_placeholder'), {
        target: {value: 'DemoSpaceDisplayName'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));

    await waitFor(() => expect(api.manage.addGraphSpace).toHaveBeenCalled());
    const values = api.manage.addGraphSpace.mock.calls[0][0];
    expect(values).toMatchObject({
        name: 'demo_space',
        nickname: 'DemoSpaceDisplayName',
        auth: true,
        description: '',
    });
    expect(values).toMatchObject({
        max_graph_number: 100,
        cpu_limit: 128,
        memory_limit: 128,
        compute_cpu_limit: 128,
        compute_memory_limit: 128,
        storage_limit: 10240,
    });
});

test('creates an anonymous-mode GraphSpace with authentication disabled', async () => {
    isAuthEnabled.mockReturnValue(false);
    render(
        <EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />
    );
    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'anonymous_space'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));

    await waitFor(() => expect(api.manage.addGraphSpace).toHaveBeenCalled());
    expect(api.manage.addGraphSpace.mock.calls[0][0].auth).toBe(false);
});

test('preserves existing GraphSpace authentication while editing', async () => {
    api.manage.getGraphSpace.mockResolvedValue({
        status: 200,
        data: {
            name: 'legacy_space',
            nickname: 'LegacySpace',
            auth: false,
        },
    });
    render(
        <EditLayer
            visible
            detail={{name: 'legacy_space'}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    await waitFor(() => expect(
        screen.getByPlaceholderText('graphspace.form.id_placeholder')
    ).toHaveValue('legacy_space'));
    fireEvent.click(screen.getByRole('button', {name: 'common.action.save'}));

    await waitFor(() => expect(api.manage.updateGraphSpace).toHaveBeenCalled());
    expect(api.manage.updateGraphSpace.mock.calls[0][1].auth).toBe(false);
});

test('creates without an alias and never copies the GraphSpace name into nickname', async () => {
    render(
        <EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />
    );
    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'demo_space'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));

    await waitFor(() => expect(api.manage.addGraphSpace).toHaveBeenCalled());
    expect(api.manage.addGraphSpace.mock.calls[0][0].nickname).not.toBe('demo_space');
});

test('keeps validation failures inline without calling the API', async () => {
    render(
        <EditLayer
            visible
            detail={{}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );
    await waitFor(() => expect(api.auth.getUserList).toHaveBeenCalled());

    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'Invalid ID'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));

    expect(await screen.findByText('graphspace.form.id_rule')).toBeInTheDocument();
    expect(api.manage.addGraphSpace).not.toHaveBeenCalled();
});

test('keeps the path name immutable while allowing display-name edits', async () => {
    api.manage.getGraphSpace.mockResolvedValue({
        status: 200,
        data: {name: 'demo_space', nickname: 'demo_space'},
    });
    render(
        <EditLayer
            visible
            detail={{name: 'demo_space'}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    const pathName = screen.getByPlaceholderText('graphspace.form.id_placeholder');
    const displayName = screen.getByPlaceholderText('graphspace.form.name_placeholder');
    await waitFor(() => expect(pathName).toHaveValue('demo_space'));
    expect(pathName).toBeDisabled();
    expect(displayName).toBeEnabled();
    expect(displayName).toHaveValue('');
});

test('preserves a real GraphSpace alias while editing', async () => {
    api.manage.getGraphSpace.mockResolvedValue({
        status: 200,
        data: {name: 'demo_space', nickname: 'Demo Space'},
    });
    render(
        <EditLayer
            visible
            detail={{name: 'demo_space'}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );
    await waitFor(() => expect(
        screen.getByPlaceholderText('graphspace.form.name_placeholder')
    ).toHaveValue('Demo Space'));
});

test('resource fields are optional and expose examples and field help', async () => {
    render(
        <EditLayer
            visible
            detail={{}}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );
    await waitFor(() => expect(api.auth.getUserList).toHaveBeenCalled());

    const advanced = document.querySelector('details');
    expect(advanced).not.toHaveAttribute('open');
    fireEvent.click(screen.getByText('graphspace.form.advanced_title'));
    expect(advanced).toHaveAttribute('open');

    expect(screen.getAllByPlaceholderText('graphspace.form.cpu_placeholder')).toHaveLength(2);
    expect(screen.getAllByPlaceholderText('graphspace.form.memory_placeholder')).toHaveLength(2);
    expect(screen.getByPlaceholderText('graphspace.form.max_graph_placeholder'))
        .toBeInTheDocument();
    expect(screen.getByRole('img', {name: /graphspace\.form\.id_help/}))
        .toBeInTheDocument();
    expect(screen.getByRole('img', {name: /graphspace\.form\.graph_cpu_help/}))
        .toBeInTheDocument();
    expect(screen.getByRole('img', {name: /graphspace\.form\.oltp_namespace_help/}))
        .toBeInTheDocument();

    fireEvent.change(screen.getAllByPlaceholderText('graphspace.form.cpu_placeholder')[0], {
        target: {value: '4'},
    });
    fireEvent.click(screen.getByText('graphspace.form.advanced_title'));
    expect(advanced).not.toHaveAttribute('open');

    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'advanced_space'},
    });
    fireEvent.change(screen.getByPlaceholderText('graphspace.form.name_placeholder'), {
        target: {value: 'AdvancedSpace'},
    });
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));
    await waitFor(() => expect(api.manage.addGraphSpace).toHaveBeenCalled());
    expect(api.manage.addGraphSpace.mock.calls[0][0].cpu_limit).toBe(4);
});

test('editing CPU keeps raw zero compute limits and updates inherited effective values', async () => {
    api.manage.getGraphSpace.mockResolvedValue({status: 200, data: {
        name: 'inherited', auth: false, cpu_limit: 4, memory_limit: 8,
        compute_cpu_limit: 0, compute_memory_limit: 0,
    }});
    render(<EditLayer visible detail={{name: 'inherited'}} onCancel={jest.fn()} refresh={jest.fn()} />);
    await waitFor(() => expect(document.querySelector('#control-hooks_compute_cpu_limit')).toHaveValue('0'));
    expect(document.querySelector('#control-hooks_compute_memory_limit')).toHaveValue('0');
    expect(await screen.findByText('Inherited (4graphspace.unit.cpu)')).toBeInTheDocument();
    fireEvent.change(document.querySelector('#control-hooks_cpu_limit'), {target: {value: '16'}});
    expect(await screen.findByText('Inherited (16graphspace.unit.cpu)')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'common.action.save'}));
    await waitFor(() => expect(api.manage.updateGraphSpace).toHaveBeenCalled());
    expect(api.manage.updateGraphSpace.mock.calls[0][1]).toMatchObject({
        cpu_limit: 16, memory_limit: 8, compute_cpu_limit: 0, compute_memory_limit: 0,
    });
});

test('creation can explicitly select zero compute inheritance without changing form defaults', async () => {
    render(<EditLayer visible detail={{}} onCancel={jest.fn()} refresh={jest.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('graphspace.form.id_placeholder'), {
        target: {value: 'inherit_new'},
    });
    fireEvent.change(document.querySelector('#control-hooks_compute_cpu_limit'), {target: {value: '0'}});
    fireEvent.change(document.querySelector('#control-hooks_compute_memory_limit'), {target: {value: '0'}});
    fireEvent.click(screen.getByRole('button', {name: 'common.action.create'}));
    await waitFor(() => expect(api.manage.addGraphSpace).toHaveBeenCalled());
    expect(api.manage.addGraphSpace.mock.calls[0][0]).toMatchObject({
        cpu_limit: 128, memory_limit: 128, compute_cpu_limit: 0, compute_memory_limit: 0,
    });
});

test('clearing positive compute fields shows inheritance before submitting null', async () => {
    api.manage.getGraphSpace.mockResolvedValue({status: 200, data: {
        name: 'cleared', auth: false, cpu_limit: 4, memory_limit: 8,
        compute_cpu_limit: 6, compute_memory_limit: 7,
    }});
    render(<EditLayer visible detail={{name: 'cleared'}} onCancel={jest.fn()} refresh={jest.fn()} />);
    const cpu = document.querySelector('#control-hooks_compute_cpu_limit');
    const memory = document.querySelector('#control-hooks_compute_memory_limit');
    await waitFor(() => expect(cpu).toHaveValue('6'));
    expect(screen.queryByText('Inherited (4graphspace.unit.cpu)')).not.toBeInTheDocument();
    fireEvent.change(cpu, {target: {value: ''}});
    fireEvent.change(memory, {target: {value: ''}});
    expect(await screen.findByText('Inherited (4graphspace.unit.cpu)')).toBeInTheDocument();
    expect(await screen.findByText('Inherited (8graphspace.unit.memory)')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'common.action.save'}));
    await waitFor(() => expect(api.manage.updateGraphSpace).toHaveBeenCalled());
    expect(api.manage.updateGraphSpace.mock.calls[0][1]).toMatchObject({
        compute_cpu_limit: null, compute_memory_limit: null,
    });
});

test('both locales explain clearing and the paired Server requirement', () => {
    for (const locale of ['en-US', 'zh-CN']) {
        const pages = require(`../../i18n/resources/${locale}/modules/pages.json`);
        expect(pages.graphspace.form.compute_inheritance_help).toContain('apache/hugegraph#3391');
    }
});
