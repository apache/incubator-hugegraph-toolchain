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

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import GraphContextSwitcher from './index';
import * as api from '../../api';

jest.mock('../../api', () => ({
    manage: {
        getGraphSpaceList: jest.fn(),
        getGraphList: jest.fn(),
    },
}));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

jest.mock('antd', () => {
    const Select = ({
        'aria-description': ariaDescription,
        'aria-label': ariaLabel,
        options = [],
        disabled,
        onChange,
        onSearch,
        onPopupScroll,
        title,
        value,
        style,
    }) => (
        <div>
            {onSearch && <input aria-label={`${ariaLabel}.search`} onChange={event => onSearch(event.target.value)} />}
        <select
            aria-description={ariaDescription}
            aria-label={ariaLabel}
            disabled={disabled}
            title={title}
            value={value || ''}
            style={style}
            onChange={event => onChange?.(event.target.value)}
            onScroll={onPopupScroll}
        >
            <option value="" />
            {options.map(option => (
                <option key={option.value} value={option.value} title={option.title}>{option.label}</option>
            ))}
        </select>
        </div>
    );
    return {
        Alert: ({action, title}) => <div role="alert">{title}{action}</div>,
        Button: ({children, onClick}) => <button onClick={onClick}>{children}</button>,
        Select,
        Space: ({children}) => <div>{children}</div>,
        Tag: ({children}) => <span>{children}</span>,
    };
});

const LocationProbe = () => {
    const location = useLocation();
    return <output>{location.pathname}</output>;
};

const renderSwitcher = path => render(
    <MemoryRouter
        initialEntries={[path]}
        future={{v7_startTransition: true, v7_relativeSplatPath: true}}
    >
        <GraphContextSwitcher />
        <LocationProbe />
    </MemoryRouter>
);

describe('GraphContextSwitcher', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.clear();
        sessionStorage.clear();
        api.manage.getGraphSpaceList.mockResolvedValue({
            status: 200,
            data: {records: [
                {name: 'space_a', nickname: 'Space A'},
                {name: 'space_b', nickname: 'Space B'},
            ]},
        });
        api.manage.getGraphList.mockResolvedValue({
            status: 200,
            data: {records: [{name: 'graph_a', nickname: 'Graph A'}]},
        });
    });

    test('non-PD fixes GraphSpace to DEFAULT and loads its graphs', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        renderSwitcher('/navigation');

        const graphspace = screen.getByRole('combobox', {
            name: 'workbench.context.graphspace',
        });
        expect(graphspace).toBeDisabled();
        expect(graphspace).toHaveAttribute(
            'aria-description', 'DEFAULT'
        );
        expect(graphspace).toHaveAttribute('title', 'DEFAULT');
        expect(screen.getByRole('option', {name: 'DEFAULT'})).toBeInTheDocument();
        expect(screen.queryByText('workbench.context.graphspace_label'))
            .not.toBeInTheDocument();
        expect(screen.getByText('/')).toBeInTheDocument();
        await waitFor(() => {
            expect(api.manage.getGraphList).toHaveBeenCalledWith(
                'DEFAULT',
                expect.objectContaining({brief: true, page_no: 1, page_size: 100}),
                expect.any(Object)
            );
        });
        expect(screen.queryByText('workbench.context.non_pd_fixed')).not.toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'workbench.context.name'})).toBeInTheDocument();
    });

    test('loads a bounded PD GraphSpace page', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphSpaceList.mockResolvedValueOnce({
            status: 200,
            data: {records: [
                {name: 'DEFAULT', nickname: 'Default'},
                {name: 'demo_space', nickname: 'Demo Space'},
                {name: 'loader_space', nickname: 'Loader Space'},
            ]},
        });
        renderSwitcher('/graphspace/demo_space');

        expect(await screen.findByRole('option', {name: 'Loader Space'})).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'workbench.context.graphspace'}))
            .toHaveValue('demo_space');
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledWith(
            {brief: true, page_no: 1, page_size: 100, query: '', selected: 'demo_space'},
            expect.any(Object)
        );
    });

    test('sizes current names within bounds and exposes full values', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        const longName = 'graph_with_a_name_that_is_far_too_long_for_the_topbar';
        api.manage.getGraphList.mockResolvedValueOnce({
            status: 200,
            data: {records: [{name: longName}]},
        });
        renderSwitcher(`/gremlin/space_a/${longName}`);

        const graphspace = screen.getByRole('combobox', {
            name: 'workbench.context.graphspace',
        });
        const graph = await screen.findByRole('combobox', {
            name: 'workbench.context.graph',
        });
        await waitFor(() => expect(graph).toHaveValue(longName));

        expect(graphspace).toHaveStyle({width: '112px'});
        expect(graph).toHaveStyle({width: '240px'});
        expect(graph).toHaveAttribute('title', longName);
        expect(graph).toHaveAttribute('aria-description', longName);
    });

    test('temporarily keeps the selected GraphSpace in options while loading', () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphSpaceList.mockReturnValueOnce(new Promise(() => {}));
        renderSwitcher('/graphspace/demo_space');

        expect(screen.getByRole('combobox', {name: 'workbench.context.graphspace'}))
            .toHaveValue('demo_space');
        expect(screen.getByRole('option', {name: 'demo_space'})).toBeInTheDocument();
    });

    test('clears a missing GraphSpace instead of guessing another one', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphSpaceList.mockResolvedValueOnce({
            status: 200,
            data: {records: [{name: 'space_b', nickname: 'Space B'}]},
        });
        renderSwitcher('/gremlin/deleted_space/old_graph');

        await screen.findByText('/navigation');
        expect(screen.getByRole('combobox', {name: 'workbench.context.graphspace'}))
            .toHaveValue('');
        expect(screen.getByRole('combobox', {name: 'workbench.context.graph'}))
            .not.toHaveValue('old_graph');
        expect(localStorage.getItem('hubble_workbench_graph_context')).toBeNull();
    });

    test('clears a deep-linked graph that does not belong to its GraphSpace', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphList.mockResolvedValueOnce({
            status: 200,
            data: {records: [{name: 'graph_b', nickname: 'Graph B'}]},
        });
        renderSwitcher('/gremlin/space_a/missing_graph');

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'workbench.context.graph'}))
                .toHaveValue('');
        });
        await waitFor(() => {
            expect(screen.getByText('/graphspace/space_a')).toBeInTheDocument();
        });
        expect(JSON.parse(localStorage.getItem('hubble_workbench_graph_context'))).toEqual({
            graphspace: 'space_a',
        });
    });

    test('route context selects and persists the current graph', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        renderSwitcher('/gremlin/space_a/graph_a');

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'workbench.context.graph'}))
                .toHaveValue('graph_a');
        });
        expect(JSON.parse(localStorage.getItem('hubble_workbench_graph_context'))).toEqual({
            graphspace: 'space_a',
            graph: 'graph_a',
        });
    });

    test('selecting a graph opens its overview', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        renderSwitcher('/graphspace/space_a');

        const graphSelect = await screen.findByRole('combobox', {
            name: 'workbench.context.graph',
        });
        await screen.findByRole('option', {name: 'Graph A'});
        fireEvent.change(graphSelect, {target: {value: 'graph_a'}});
        expect(screen.getByText('/graphspace/space_a/graph/graph_a/detail')).toBeInTheDocument();
    });

    test.each(['gremlin', 'algorithms', 'asyncTasks'])(
        'selecting a graph preserves the %s workbench',
        async moduleName => {
            sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
            renderSwitcher(`/${moduleName}`);

            const graphspace = screen.getByRole('combobox', {
                name: 'workbench.context.graphspace',
            });
            await screen.findByRole('option', {name: 'Space A'});
            fireEvent.change(graphspace, {target: {value: 'space_a'}});
            expect(screen.getByText(`/${moduleName}`)).toBeInTheDocument();

            const graph = await screen.findByRole('combobox', {
                name: 'workbench.context.graph',
            });
            await screen.findByRole('option', {name: 'Graph A'});
            fireEvent.change(graph, {target: {value: 'graph_a'}});

            expect(screen.getByText(`/${moduleName}/space_a/graph_a`))
                .toBeInTheDocument();
        }
    );

    test('shows an inline retry when graph loading fails', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        api.manage.getGraphList.mockRejectedValueOnce(new Error('offline'));
        renderSwitcher('/navigation');

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'workbench.context.graphs_load_failed'
        );
        fireEvent.click(screen.getByRole('button', {
            name: 'workbench.context.retry_graphs',
        }));
        await waitFor(() => expect(api.manage.getGraphList).toHaveBeenCalledTimes(2));
    });

    test('treats a resolved graph business error as retryable', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        api.manage.getGraphList.mockResolvedValueOnce({status: 503, data: null});
        renderSwitcher('/navigation');

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'workbench.context.graphs_load_failed'
        );
    });

    test('distinguishes missing graph permission from a temporary failure', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        api.manage.getGraphList.mockResolvedValueOnce({status: 403, data: null});
        renderSwitcher('/navigation');

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'workbench.context.graphs_forbidden'
        );
        expect(screen.queryByRole('button', {
            name: 'workbench.context.retry_graphs',
        })).not.toBeInTheDocument();
    });

    test('clears a forbidden PD GraphSpace left by a previous account', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        localStorage.setItem('hubble_workbench_graph_context', JSON.stringify({
            graphspace: 'space_a',
            graph: 'graph_a',
        }));
        api.manage.getGraphList.mockResolvedValueOnce({status: 403, data: null});
        renderSwitcher('/gremlin/space_a/graph_a');

        await waitFor(() => {
            expect(screen.getByText('/navigation')).toBeInTheDocument();
        });
        expect(localStorage.getItem('hubble_workbench_graph_context')).toBeNull();
        expect(screen.getByRole('combobox', {
            name: 'workbench.context.graphspace',
        })).toHaveValue('');
        expect(screen.queryByText('workbench.context.graphs_forbidden'))
            .not.toBeInTheDocument();
    });

    test('clears a masked missing PD GraphSpace after access is revoked', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        localStorage.setItem('hubble_workbench_graph_context', JSON.stringify({
            graphspace: 'space_a',
            graph: 'graph_a',
        }));
        api.manage.getGraphList.mockResolvedValueOnce({status: 404, data: null});
        renderSwitcher('/gremlin/space_a/graph_a');

        await waitFor(() => {
            expect(screen.getByText('/navigation')).toBeInTheDocument();
        });
        expect(localStorage.getItem('hubble_workbench_graph_context')).toBeNull();
        expect(screen.getByRole('combobox', {
            name: 'workbench.context.graphspace',
        })).toHaveValue('');
        expect(screen.queryByText('workbench.context.graphs_load_failed'))
            .not.toBeInTheDocument();
    });

    test('graph success cannot erase a concurrent GraphSpace failure', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphSpaceList.mockResolvedValueOnce({status: 500, data: null});
        renderSwitcher('/gremlin/space_a/graph_a');

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'workbench.context.graphspaces_load_failed'
        );
        expect(screen.getByRole('combobox', {name: 'workbench.context.graph'}))
            .toHaveValue('graph_a');
    });

    test('switching GraphSpace immediately removes graphs from the previous space', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        let resolveSpaceB;
        api.manage.getGraphList.mockImplementation(graphspace => {
            if (graphspace === 'space_b') {
                return new Promise(resolve => {
                    resolveSpaceB = resolve;
                });
            }
            return Promise.resolve({
                status: 200,
                data: {records: [{name: 'graph_a', nickname: 'Graph A'}]},
            });
        });
        renderSwitcher('/graphspace/space_a');
        await screen.findByRole('option', {name: 'Graph A'});

        fireEvent.change(
            screen.getByRole('combobox', {name: 'workbench.context.graphspace'}),
            {target: {value: 'space_b'}}
        );

        expect(screen.queryByRole('option', {name: 'Graph A'})).not.toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'workbench.context.graph'})).toBeDisabled();
        await waitFor(() => {
            expect(api.manage.getGraphList).toHaveBeenCalledWith(
                'space_b',
                expect.objectContaining({brief: true, page_no: 1, page_size: 100}),
                expect.any(Object)
            );
        });
        resolveSpaceB({
            status: 200,
            data: {records: [{name: 'graph_b', nickname: 'Graph B'}]},
        });
        expect(await screen.findByRole('option', {name: 'Graph B'})).toBeInTheDocument();
    });

    test('ignores a graph response from the previously selected GraphSpace', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        let resolveSpaceA;
        api.manage.getGraphList.mockImplementation(graphspace => {
            if (graphspace === 'space_a') {
                return new Promise(resolve => {
                    resolveSpaceA = resolve;
                });
            }
            return Promise.resolve({
                status: 200,
                data: {records: [{name: 'graph_b', nickname: 'Graph B'}]},
            });
        });
        renderSwitcher('/graphspace/space_a');
        await screen.findByRole('option', {name: 'Space B'});

        fireEvent.change(
            screen.getByRole('combobox', {name: 'workbench.context.graphspace'}),
            {target: {value: 'space_b'}}
        );
        expect(await screen.findByRole('option', {name: 'Graph B'})).toBeInTheDocument();

        await act(async () => {
            resolveSpaceA({
                status: 200,
                data: {records: [{name: 'graph_a', nickname: 'Graph A'}]},
            });
            await Promise.resolve();
        });
        expect(screen.queryByRole('option', {name: 'Graph A'})).not.toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Graph B'})).toBeInTheDocument();
    });

    test('stacks both failures and retries only the selected source', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        api.manage.getGraphSpaceList.mockRejectedValueOnce(new Error('pd offline'));
        api.manage.getGraphList.mockRejectedValueOnce(new Error('server offline'));
        renderSwitcher('/gremlin/space_a/graph_a');

        expect(await screen.findByText('workbench.context.graphspaces_load_failed'))
            .toBeInTheDocument();
        expect(await screen.findByText('workbench.context.graphs_load_failed'))
            .toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {
            name: 'workbench.context.retry_graphspaces',
        }));
        await waitFor(() => expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(2));
        expect(api.manage.getGraphList).toHaveBeenCalledTimes(1);

        fireEvent.click(screen.getByRole('button', {
            name: 'workbench.context.retry_graphs',
        }));
        await waitFor(() => expect(api.manage.getGraphList).toHaveBeenCalledTimes(2));
    });
    test('keeps an authorized selected graph beyond the first page without loading every page', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        api.manage.getGraphList.mockResolvedValue({
            status: 200,
            data: {
                records: [{name: 'graph_00000'}], total: 10001,
                selected: {name: 'graph_10000', nickname: 'Late graph'},
            },
        });

        renderSwitcher('/gremlin/DEFAULT/graph_10000');

        expect(await screen.findByRole('option', {name: 'Late graph'})).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'workbench.context.graph'})).toHaveValue('graph_10000');
        expect(screen.getByText('/gremlin/DEFAULT/graph_10000')).toBeInTheDocument();
        expect(api.manage.getGraphList).toHaveBeenCalledTimes(1);
        expect(api.manage.getGraphList).toHaveBeenCalledWith('DEFAULT', {
            brief: true, page_no: 1, page_size: 100, query: '', selected: 'graph_10000',
        }, expect.any(Object));
    });

    test('loads the next graph page only when the dropdown reaches the bottom and selects its graph', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        const first = Array.from({length: 100}, (_, index) => ({name: `graph_${index}`}));
        const late = {name: 'late_graph', nickname: 'Late graph'};
        api.manage.getGraphList.mockImplementation((space, params) => Promise.resolve({
            status: 200,
            data: {
                records: params.page_no === 2 ? [late] : first,
                total: 101,
                selected: params.selected === late.name ? late : null,
            },
        }));
        renderSwitcher('/navigation');
        await screen.findByRole('option', {name: 'graph_0'});
        expect(api.manage.getGraphList).toHaveBeenCalledTimes(1);
        expect(screen.queryByRole('option', {name: 'Late graph'})).not.toBeInTheDocument();
        const graph = screen.getByRole('combobox', {name: 'workbench.context.graph'});
        Object.defineProperties(graph, {clientHeight: {value: 100}, scrollHeight: {value: 200}});
        fireEvent.scroll(graph, {target: {scrollTop: 100}});

        expect(await screen.findByRole('option', {name: 'Late graph'})).toBeInTheDocument();
        expect(api.manage.getGraphList).toHaveBeenCalledTimes(2);
        expect(api.manage.getGraphList).toHaveBeenLastCalledWith('DEFAULT', expect.objectContaining({
            page_no: 2, page_size: 100,
        }), expect.any(Object));
        fireEvent.change(graph, {target: {value: 'late_graph'}});
        expect(await screen.findByText('/graphspace/DEFAULT/graph/late_graph/detail')).toBeInTheDocument();
        await waitFor(() => expect(graph).toHaveValue('late_graph'));
    });

    test.each(['late_graph', 'Late Alias'])('searches all graph names and aliases remotely for %s', async query => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        const late = {name: 'late_graph', nickname: 'Late Alias'};
        api.manage.getGraphList.mockImplementation((space, params) => Promise.resolve({
            status: 200,
            data: {records: params.query ? [late] : [{name: 'first'}], total: params.query ? 1 : 10001,
                selected: params.selected === late.name ? late : null},
        }));
        renderSwitcher('/navigation');
        await screen.findByRole('option', {name: 'first'});
        fireEvent.change(screen.getByRole('textbox', {name: 'workbench.context.graph.search'}), {
            target: {value: query},
        });

        expect(await screen.findByRole('option', {name: 'Late Alias'})).toBeInTheDocument();
        expect(api.manage.getGraphList).toHaveBeenCalledTimes(2);
        expect(api.manage.getGraphList).toHaveBeenLastCalledWith('DEFAULT', expect.objectContaining({
            query, page_no: 1, page_size: 100,
        }), expect.any(Object));
        fireEvent.change(screen.getByRole('combobox', {name: 'workbench.context.graph'}), {
            target: {value: 'late_graph'},
        });
        expect(await screen.findByText('/graphspace/DEFAULT/graph/late_graph/detail')).toBeInTheDocument();
    });

    test('ignores a late search response after a new search completes', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: false}));
        let resolveOld;
        api.manage.getGraphList.mockImplementation((space, params) => {
            if (params.query === 'old') {
                return new Promise(resolve => {resolveOld = resolve;});
            }
            return Promise.resolve({status: 200, data: {
                records: [{name: params.query || 'first'}], total: 1, selected: null,
            }});
        });
        renderSwitcher('/navigation');
        await screen.findByRole('option', {name: 'first'});
        const search = screen.getByRole('textbox', {name: 'workbench.context.graph.search'});
        fireEvent.change(search, {target: {value: 'old'}});
        await waitFor(() => expect(resolveOld).toBeDefined());
        fireEvent.change(search, {target: {value: 'new'}});
        expect(await screen.findByRole('option', {name: 'new'})).toBeInTheDocument();

        await act(async () => resolveOld({status: 200, data: {records: [{name: 'old'}], total: 1}}));

        expect(screen.getByRole('option', {name: 'new'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'old'})).not.toBeInTheDocument();
    });

    test('keeps an authorized GraphSpace beyond the first page with one bounded request', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        const selected = {name: 'space_10000', nickname: 'Late space'};
        api.manage.getGraphSpaceList.mockImplementation(params => Promise.resolve(params.page_size === -1
            ? {status: 400, message: 'GraphSpace list exceeds the maximum all-record limit of 10000'}
            : {status: 200, data: {records: [{name: 'first'}], total: 10001, selected}}));

        renderSwitcher('/graphspace/space_10000');

        expect(await screen.findByRole('option', {name: 'Late space'})).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'workbench.context.graphspace'})).toHaveValue(selected.name);
        expect(screen.getByText('/graphspace/space_10000')).toBeInTheDocument();
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(1);
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledWith({
            brief: true, page_no: 1, page_size: 100, query: '', selected: selected.name,
        }, expect.any(Object));
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    test('loads the next GraphSpace page only at the bottom and selects its space', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        const first = Array.from({length: 100}, (_, index) => ({name: `space_${index}`}));
        const late = {name: 'late_space', nickname: 'Late space'};
        api.manage.getGraphSpaceList.mockImplementation(params => Promise.resolve({status: 200, data: {
            records: params.page_no === 2 ? [late] : first, total: 101,
            selected: params.selected === late.name ? late : null,
        }}));
        renderSwitcher('/navigation');
        await screen.findByRole('option', {name: 'space_0'});
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(1);
        expect(screen.queryByRole('option', {name: 'Late space'})).not.toBeInTheDocument();
        const select = screen.getByRole('combobox', {name: 'workbench.context.graphspace'});
        Object.defineProperties(select, {clientHeight: {value: 100}, scrollHeight: {value: 200}});
        fireEvent.scroll(select, {target: {scrollTop: 0}});
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(1);
        fireEvent.scroll(select, {target: {scrollTop: 100}});

        expect(await screen.findByRole('option', {name: 'Late space'})).toBeInTheDocument();
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(2);
        expect(api.manage.getGraphSpaceList).toHaveBeenLastCalledWith({
            brief: true, page_no: 2, page_size: 100, query: '', selected: '',
        }, expect.any(Object));
        fireEvent.change(select, {target: {value: late.name}});
        expect(await screen.findByText('/graphspace/late_space')).toBeInTheDocument();
        await waitFor(() => expect(select).toHaveValue(late.name));
    });

    test.each(['space_10000', 'Late Alias'])('searches GraphSpace names and aliases for %s', async query => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        const current = {name: 'space_a', nickname: 'Space A'};
        const late = {name: 'space_10000', nickname: 'Late Alias'};
        api.manage.getGraphSpaceList.mockImplementation(params => Promise.resolve({status: 200, data: {
            records: params.query ? [late] : [current], total: params.query ? 1 : 10001, selected: current,
        }}));
        renderSwitcher('/graphspace/space_a');
        await screen.findByRole('option', {name: 'Space A'});
        fireEvent.change(screen.getByRole('textbox', {name: 'workbench.context.graphspace.search'}), {
            target: {value: query},
        });

        expect(await screen.findByRole('option', {name: 'Late Alias'})).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'workbench.context.graphspace'})).toHaveValue(current.name);
        expect(screen.getByRole('option', {name: 'Space A'})).toBeInTheDocument();
        expect(api.manage.getGraphSpaceList).toHaveBeenCalledTimes(2);
        expect(api.manage.getGraphSpaceList).toHaveBeenLastCalledWith({
            brief: true, page_no: 1, page_size: 100, query, selected: current.name,
        }, expect.any(Object));
    });

    test('ignores an old GraphSpace search response after the next search completes', async () => {
        sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
        let resolveOld;
        api.manage.getGraphSpaceList.mockImplementation(params => {
            if (params.query === 'old') return new Promise(resolve => {resolveOld = resolve;});
            return Promise.resolve({status: 200, data: {
                records: [{name: params.query || 'first'}], total: 1, selected: null,
            }});
        });
        renderSwitcher('/navigation');
        await screen.findByRole('option', {name: 'first'});
        const search = screen.getByRole('textbox', {name: 'workbench.context.graphspace.search'});
        fireEvent.change(search, {target: {value: 'old'}});
        await waitFor(() => expect(resolveOld).toBeDefined());
        fireEvent.change(search, {target: {value: 'new'}});
        expect(await screen.findByRole('option', {name: 'new'})).toBeInTheDocument();

        await act(async () => resolveOld({status: 200, data: {records: [{name: 'old'}], total: 1}}));

        expect(screen.getByRole('option', {name: 'new'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'old'})).not.toBeInTheDocument();
    });

});
