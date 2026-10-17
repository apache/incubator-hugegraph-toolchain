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
import AnalysisHome, {extractQueryErrorMessage} from './index';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';
import {EXECUTION_LOGS_TYPE} from '../../../utils/constants';
import {setConfig} from '../../../utils/config';

jest.mock('../../../api', () => ({
    analysis: {
        streamText2Gql: jest.fn(),
        getText2GqlConfig: jest.fn(),
        getExecutionLogs: jest.fn(),
        fetchFavoriteQueries: jest.fn(),
        getGraphData: jest.fn(),
        getExecutionQuery: jest.fn(),
        executeText2Gql: jest.fn(),
        getCypherExecutionQuery: jest.fn(),
        getExecutionTask: jest.fn(),
        getCypherTask: jest.fn(),
    },
    manage: {
        getMetaEdgeList: jest.fn(),
        getMetaVertexList: jest.fn(),
        getMetaPropertyList: jest.fn(),
    },
}));
jest.mock('../QueryBar/Home', () => props => (
    <div>
        <span data-testid='query-content'>{props.codeEditorContent}</span>
        <span data-testid='source-question'>{props.text2gql.question}</span>
        <span data-testid='query-language'>{props.text2gql.language}</span>
        <button onClick={() => props.onTabsChange('Text2GQL')}>Natural language</button>
        <button onClick={() => props.onTabsChange('Cypher')}>Cypher</button>
        <button onClick={() => props.text2gql.changeLanguage('cypher')}>Generate Cypher</button>
        <button onClick={() => props.text2gql.changeLanguage('gremlin')}>Generate Gremlin</button>
        <button onClick={() => props.setCodeEditorContent('MATCH (n) RETURN n LIMIT 4')}>Edit Cypher</button>
        <button onClick={() => props.setCodeEditorContent('g.E().limit(3)')}>Edit query</button>
        <button onClick={() => props.onExecute(props.activeTab)}>Run current</button>
        <button
            onClick={() => {
                props.text2gql.updateConfig('provider', 'ollama');
                props.text2gql.updateConfig('model', 'local-model');
                props.text2gql.setQuestion('Find people');
            }}
        >
            Set model and question
        </button>
        <button disabled={props.text2gql.sharedConfig.loading} onClick={props.text2gql.generate}>Generate</button>
        <button onClick={() => props.text2gql.setQuestion('New question')}>Edit question</button>
    </div>
));
jest.mock('../QueryResult/Home', () => ({
    queryResult,
    queryStatus,
    queryMessage,
    graphRenderMode,
    onGraphRenderModeChange,
    metaData,
    propertyKeysRecords,
    graphNums,
}) => (
    <div>
        query result {queryStatus} {queryMessage}
        <output data-testid='query-result'>{JSON.stringify(queryResult)}</output>
        <span data-testid='query-status'>{queryStatus}</span>
        <span>render mode {graphRenderMode}</span>
        <span>metadata ready {String(Boolean(metaData))}</span>
        <span>properties ready {String(Boolean(propertyKeysRecords))}</span>
        <span>graph counts {graphNums.vertexCount} {graphNums.edgeCount}</span>
        <button onClick={() => onGraphRenderModeChange('3D模式')}>Use 3D</button>
    </div>
));
jest.mock('../LogsDetail/Home', () => props => (
    <div>
        query history
        <button
            disabled={!props.executionLogsData.records?.[0]}
            onClick={() => {
                const row = props.executionLogsData.records[0];
                props.onClickLoadContent(row.content, row.text);
            }}
        >
            Load history statement
        </button>
        <button
            disabled={!props.favoriteQueriesData.records?.[0]}
            onClick={() => props.onClickLoadContent(props.favoriteQueriesData.records[0].content)}
        >
            Load favorite statement
        </button>
        <span>favorite page {props.pageFavorite}</span>
        <span>execution total {props.executionLogsData.total ?? 'pending'}</span>
        <span>favorite total {props.favoriteQueriesData.total ?? 'pending'}</span>
        <button onClick={() => props.onFavoritePageChange(2, 10)}>Go favorite page 2</button>
        <span>{props.executionLogsData.records?.[0]?.content}</span>
        {props.executionLogsError && (
            <button onClick={props.onRetryExecutionLogs}>Retry records</button>
        )}
        {props.favoriteQueriesError && (
            <button onClick={props.onRetryFavoriteQueries}>Retry favorites</button>
        )}
    </div>
));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

const okList = {status: 200, data: {records: [], total: 0}};
let consoleError;

beforeAll(() => {
    consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
});

beforeEach(() => {
    setConfig({cypher_enabled: true});
    api.analysis.getText2GqlConfig.mockResolvedValue({status: 200, data: {available: false, reason: 'not_configured'}});
    window.localStorage.clear();
    window.sessionStorage.setItem('user_', JSON.stringify({
        id: 'alice',
        user_name: 'alice',
    }));
    api.analysis.getExecutionLogs.mockResolvedValue(okList);
    api.analysis.fetchFavoriteQueries.mockResolvedValue(okList);
    api.analysis.getGraphData.mockResolvedValue({
        status: 200,
        data: {vertexcount: 0, edgecount: 0},
    });
    api.manage.getMetaEdgeList.mockResolvedValue(okList);
    api.manage.getMetaVertexList.mockResolvedValue(okList);
    api.manage.getMetaPropertyList.mockResolvedValue(okList);
});

const waitForInitialData = async () => {
    expect(await screen.findByText('execution total 0')).toBeInTheDocument();
    expect(await screen.findByText('favorite total 0')).toBeInTheDocument();
    expect(await screen.findByText('metadata ready true')).toBeInTheDocument();
    expect(await screen.findByText('properties ready true')).toBeInTheDocument();
    expect(await screen.findByText('graph counts 0 0')).toBeInTheDocument();
};

it.each([null, undefined])('waits for a graph before executing a query (%s)', async graph => {
    api.analysis.getExecutionQuery.mockResolvedValue({status: 200, data: {}});
    const {rerender} = render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );

    await act(async () => {
        fireEvent.click(screen.getByRole('button', {name: 'Run current'}));
    });
    expect(api.analysis.getExecutionQuery).not.toHaveBeenCalled();
    expect(api.analysis.getGraphData).not.toHaveBeenCalled();

    rerender(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'ready_graph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    await act(async () => {
        fireEvent.click(screen.getByRole('button', {name: 'Run current'}));
    });
    expect(api.analysis.getExecutionQuery).toHaveBeenCalledWith(
        'DEFAULT', 'ready_graph', 'g.V().limit(10)'
    );
});

it('starts with a limited default only when no saved query exists', async () => {
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    expect(await screen.findByTestId('query-content')).toHaveTextContent('g.V().limit(10)');
    await act(async () => {
        fireEvent.click(screen.getByRole('button', {name: 'Cypher'}));
        await Promise.resolve();
    });
    expect(screen.getByTestId('query-content'))
        .toHaveTextContent('MATCH (n) RETURN n LIMIT 10');
});

it('restores the last input for the current graph instead of replacing it', async () => {
    window.localStorage.setItem(
        'hubble.query.alice.DEFAULT.hugegraph.Gremlin',
        'g.E().hasLabel("created")'
    );
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    expect(screen.getByTestId('query-content'))
        .toHaveTextContent('g.E().hasLabel("created")');
    fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
    expect(window.localStorage.getItem('hubble.query.alice.DEFAULT.hugegraph.Gremlin'))
        .toBe('g.E().limit(3)');
});

it('falls back to the real default after a cleared draft is mounted again', async () => {
    window.localStorage.setItem('hubble.query.alice.DEFAULT.hugegraph.Gremlin', '');
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    expect(screen.getByTestId('query-content')).toHaveTextContent('g.V().limit(10)');
});

it('does not restore another user draft for the same graph', async () => {
    window.localStorage.setItem(
        'hubble.query.bob.DEFAULT.hugegraph.Gremlin',
        'g.V().has("private", true)'
    );

    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    expect(screen.getByTestId('query-content')).toHaveTextContent('g.V().limit(10)');
    expect(screen.getByTestId('query-content')).not.toHaveTextContent('private');
});

it('prefers a multiline backend diagnostic for rejected HTTP queries', () => {
    expect(extractQueryErrorMessage({
        response: {data: {message: 'Syntax error\nline 2: unexpected token'}},
        message: 'Request failed',
    }, 'fallback')).toBe('Syntax error\nline 2: unexpected token');
    expect(extractQueryErrorMessage({message: 'Network Error'}, 'fallback'))
        .toBe('Network Error');
});

afterEach(() => {
    expect(consoleError).not.toHaveBeenCalled();
    consoleError.mockClear();
    jest.clearAllMocks();
});

afterAll(() => consoleError.mockRestore());

it('shows query results and Gremlin history for Text2GQL without executing an empty query', async () => {
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    api.analysis.getExecutionQuery.mockClear();
    fireEvent.click(screen.getByRole('button', {name: 'Natural language'}));
    fireEvent.click(screen.getByRole('button', {name: 'Run current'}));
    await act(async () => Promise.resolve());
    expect(api.analysis.getExecutionQuery).not.toHaveBeenCalled();
    expect(screen.getByText(/query result/)).toBeInTheDocument();
    expect(api.analysis.getExecutionLogs).toHaveBeenLastCalledWith(
        'DEFAULT', 'hugegraph', {page_size: 10, page_no: 1, type: '0'}
    );
});

it('turns a rejected synchronous query into a recoverable failed result', async () => {
    api.analysis.getExecutionQuery.mockRejectedValue(new Error('offline'));
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    fireEvent.click(screen.getByRole('button', {name: 'Run current'}));

    expect(await screen.findByText(/query result failed/)).toHaveTextContent(
        'offline'
    );
});

it('shows a backend query diagnostic when the service returns one', async () => {
    api.analysis.getExecutionQuery.mockResolvedValue({
        status: 400,
        message: 'Syntax error near limit',
    });
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    fireEvent.click(screen.getByRole('button', {name: 'Run current'}));

    expect(await screen.findByText(/query result failed/))
        .toHaveTextContent('Syntax error near limit');
});

it('does not start a duplicate query while the first request is pending', async () => {
    let resolveQuery;
    api.analysis.getExecutionQuery.mockImplementation(() => new Promise(resolve => {
        resolveQuery = resolve;
    }));
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    const run = screen.getByRole('button', {name: 'Run current'});
    fireEvent.click(run);
    fireEvent.click(run);

    expect(api.analysis.getExecutionQuery).toHaveBeenCalledTimes(1);
    await act(async () => resolveQuery({status: 200, data: {}}));
});

it('preserves the 3D canvas mode while repeating a graph query', async () => {
    let resolveRepeatedQuery;
    api.analysis.getExecutionQuery
        .mockResolvedValueOnce({
            status: 200,
            data: {
                graph_view: {
                    vertices: [{id: 'v1'}],
                    edges: [],
                },
            },
        })
        .mockImplementationOnce(() => new Promise(resolve => {
            resolveRepeatedQuery = resolve;
        }));
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();

    const run = screen.getByRole('button', {name: 'Run current'});
    fireEvent.click(run);
    await screen.findByText(/query result success/);
    fireEvent.click(screen.getByRole('button', {name: 'Use 3D'}));
    expect(screen.getByText('render mode 3D模式')).toBeInTheDocument();

    fireEvent.click(run);
    expect(await screen.findByText(/query result loading/)).toBeInTheDocument();
    expect(screen.getByText('render mode 3D模式')).toBeInTheDocument();

    await act(async () => resolveRepeatedQuery({
        status: 200,
        data: {
            graph_view: {
                vertices: [{id: 'v1'}],
                edges: [],
            },
        },
    }));
});

it('keeps execution-history failure separate and retries only that source', async () => {
    api.analysis.getExecutionLogs
        .mockRejectedValueOnce(new Error('history offline'))
        .mockResolvedValueOnce(okList);
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );

    fireEvent.click(await screen.findByRole('button', {name: 'Retry records'}));

    await waitFor(() => expect(api.analysis.getExecutionLogs).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByRole('button', {
        name: 'Retry records',
    })).not.toBeInTheDocument());
    expect(api.analysis.fetchFavoriteQueries).toHaveBeenCalledTimes(1);
});

it('does not let late history from the previous graph replace current rows', async () => {
    let resolveA;
    let resolveB;
    api.analysis.getExecutionLogs.mockImplementation((space, graph) => (
        new Promise(resolve => {
            if (graph === 'graph-a') {
                resolveA = resolve;
            }
            else {
                resolveB = resolve;
            }
        })
    ));
    const {rerender} = render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'graph-a'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await act(async () => Promise.resolve());
    rerender(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'graph-b'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await act(async () => Promise.resolve());

    await act(async () => resolveB({
        status: 200,
        data: {records: [{id: 2, content: 'current-b'}], total: 1},
    }));
    expect(await screen.findByText('current-b')).toBeInTheDocument();
    await act(async () => resolveA({
        status: 200,
        data: {records: [{id: 1, content: 'stale-a'}], total: 1},
    }));
    expect(screen.getByText('current-b')).toBeInTheDocument();
    expect(screen.queryByText('stale-a')).not.toBeInTheDocument();
});

it('keeps a pending favorite page selected instead of rolling back early', async () => {
    let resolvePageTwo;
    api.analysis.fetchFavoriteQueries
        .mockResolvedValueOnce(okList)
        .mockImplementationOnce(() => new Promise(resolve => {
            resolvePageTwo = resolve;
        }));
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitFor(() => expect(api.analysis.fetchFavoriteQueries).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole('button', {name: 'Go favorite page 2'}));
    await waitFor(() => expect(api.analysis.fetchFavoriteQueries).toHaveBeenCalledTimes(2));
    expect(screen.getByText('favorite page 2')).toBeInTheDocument();

    await act(async () => {
        resolvePageTwo({
            status: 200,
            data: {records: [{id: 2}], total: 11},
        });
    });
    expect(screen.getByText('favorite page 2')).toBeInTheDocument();
    expect(api.analysis.fetchFavoriteQueries).toHaveBeenCalledTimes(2);
});

it.each(['resolve', 'reject'])(
    'keeps the new graph occupied when the old query completes via %s', async completion => {
        let resolveA;
        let rejectA;
        let resolveB;
        api.analysis.getExecutionQuery
            .mockImplementationOnce(() => new Promise((resolve, reject) => {
                resolveA = resolve;
                rejectA = reject;
            }))
            .mockImplementationOnce(() => new Promise(resolve => {
                resolveB = resolve;
            }))
            .mockResolvedValueOnce({status: 200, data: {}});
        const home = graph => (
            <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
                <AnalysisHome />
            </GraphAnalysisContext.Provider>
        );
        const {rerender} = render(home('graph-a'));
        await waitForInitialData();
        const run = () => screen.getByRole('button', {name: 'Run current'});
        fireEvent.click(run());
        fireEvent.click(run());
        expect(api.analysis.getExecutionQuery).toHaveBeenCalledTimes(1);

        rerender(home('graph-b'));
        await waitForInitialData();
        fireEvent.click(run());
        expect(api.analysis.getExecutionQuery).toHaveBeenCalledTimes(2);
        expect(api.analysis.getExecutionQuery).toHaveBeenLastCalledWith(
            'DEFAULT', 'graph-b', 'g.V().limit(10)'
        );
        await act(async () => {
            if (completion === 'resolve') {
                resolveA({status: 200, data: {}, message: 'stale-a'});
            }
            else {
                rejectA(new Error('stale-a'));
            }
        });
        expect(screen.getByText(/query result loading/)).toBeInTheDocument();
        expect(screen.queryByText(/stale-a/)).not.toBeInTheDocument();
        // This QueryBar fixture deliberately keeps its button clickable while loading.
        fireEvent.click(run());
        expect(api.analysis.getExecutionQuery).toHaveBeenCalledTimes(2);

        await act(async () => resolveB({status: 200, data: {}, message: 'current-b'}));
        expect(screen.getByText(/query result success/)).toHaveTextContent('current-b');
        await act(async () => fireEvent.click(run()));
        expect(api.analysis.getExecutionQuery).toHaveBeenCalledTimes(3);
    });

it.each([false, true])('ignores late counts after switching graphs (return to A: %s)', async returnToA => {
    const pending = [];
    api.analysis.getGraphData.mockImplementation((space, graph) => new Promise(resolve => {
        pending.push({graph, resolve});
    }));
    const home = graph => (
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    const {rerender, unmount} = render(home('graph-a'));
    await waitFor(() => expect(pending).toHaveLength(1));
    rerender(home('graph-b'));
    await waitFor(() => expect(pending).toHaveLength(2));
    expect(screen.getByText('graph counts -1 -1')).toBeInTheDocument();
    await act(async () => pending[1].resolve({
        status: 200, data: {vertexcount: 20, edgecount: 21},
    }));
    expect(screen.getByText('graph counts 20 21')).toBeInTheDocument();
    if (returnToA) {
        rerender(home('graph-a'));
        await waitFor(() => expect(pending).toHaveLength(3));
        expect(screen.getByText('graph counts -1 -1')).toBeInTheDocument();
        await act(async () => pending[2].resolve({
            status: 200, data: {vertexcount: 30, edgecount: 31},
        }));
    }
    await act(async () => pending[0].resolve({
        status: 200, data: {vertexcount: 10, edgecount: 11},
    }));
    expect(screen.getByText(returnToA ? 'graph counts 30 31' : 'graph counts 20 21')).toBeInTheDocument();
    unmount();
});

it('does not let an older same-graph refresh overwrite current counts', async () => {
    const pending = [];
    api.analysis.getGraphData.mockImplementation(() => new Promise(resolve => {
        pending.push(resolve);
    }));
    api.analysis.getExecutionQuery.mockResolvedValue({status: 200, data: {}});
    const {unmount} = render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'graph-a'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitFor(() => expect(pending).toHaveLength(1));
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    await waitFor(() => expect(pending).toHaveLength(2));
    await act(async () => pending[1]({status: 200, data: {vertexcount: 20, edgecount: 21}}));
    await act(async () => pending[0]({status: 200, data: {vertexcount: 10, edgecount: 11}}));
    expect(screen.getByText('graph counts 20 21')).toBeInTheDocument();
    // A pending post-query refresh is also detached on unmount.
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    await waitFor(() => expect(pending).toHaveLength(3));
    unmount();
    await act(async () => pending[2]({status: 200, data: {vertexcount: 40, edgecount: 41}}));
});


it('executes generated Gremlin directly and preserves its source when editing and rerunning', async () => {
    api.analysis.streamText2Gql.mockResolvedValue({status: 200,
        data: {valid: true, language: 'gremlin', query: 'g.V().limit(2)'}});
    api.analysis.executeText2Gql.mockResolvedValue({status: 200, data: {}});
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    fireEvent.click(screen.getByRole('button', {name: 'Natural language'}));
    fireEvent.click(screen.getByRole('button', {name: 'Set model and question'}));
    await waitFor(() => expect(screen.getByRole('button', {name: 'Generate'})).toBeEnabled());
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate'})));
    expect(api.analysis.executeText2Gql).toHaveBeenCalledWith(
        'DEFAULT', 'hugegraph', 'gremlin', {content: 'g.V().limit(2)', text: 'Find people', read_only: true},
        expect.objectContaining({signal: expect.any(Object)})
    );
    expect(screen.getByTestId('query-content')).toHaveTextContent('g.V().limit(2)');
    fireEvent.click(screen.getByRole('button', {name: 'Edit question'}));
    fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    expect(api.analysis.executeText2Gql).toHaveBeenLastCalledWith(
        'DEFAULT', 'hugegraph', 'gremlin', {content: 'g.E().limit(3)', text: 'Find people', read_only: true},
        expect.objectContaining({signal: expect.any(Object)})
    );
});


it('keeps the Text2GQL read-only marker when manually running a query with no source question', async () => {
    api.analysis.executeText2Gql.mockResolvedValue({status: 200, data: {}});
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    fireEvent.click(screen.getByRole('button', {name: 'Natural language'}));
    fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    expect(api.analysis.executeText2Gql).toHaveBeenLastCalledWith(
        'DEFAULT', 'hugegraph', 'gremlin', {content: 'g.E().limit(3)', text: '', read_only: true},
        expect.objectContaining({signal: expect.any(Object)})
    );
});


it('generates Cypher, reruns edits with the original question and restores each dialect draft', async () => {
    const query = 'MATCH (n) RETURN n LIMIT 2';
    api.analysis.streamText2Gql.mockResolvedValue({status: 200, data: {valid: true, query, language: 'cypher'}});
    api.analysis.executeText2Gql.mockResolvedValue({status: 200, data: {}});
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    fireEvent.click(screen.getByRole('button', {name: 'Natural language'}));
    fireEvent.click(screen.getByRole('button', {name: 'Set model and question'}));
    fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
    fireEvent.click(screen.getByRole('button', {name: 'Generate Cypher'}));
    expect(screen.getByTestId('query-content')).toBeEmptyDOMElement();
    await waitFor(() => expect(screen.getByRole('button', {name: 'Generate'})).toBeEnabled());
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate'})));
    expect(api.analysis.executeText2Gql).toHaveBeenCalledWith(
        'DEFAULT', 'hugegraph', 'cypher', {content: query, text: 'Find people', read_only: true},
        expect.objectContaining({signal: expect.any(Object)})
    );
    expect(api.analysis.getCypherExecutionQuery).not.toHaveBeenCalled();
    expect(api.analysis.getExecutionLogs).toHaveBeenLastCalledWith('DEFAULT', 'hugegraph',
        expect.objectContaining({type: EXECUTION_LOGS_TYPE.Cypher}));
    fireEvent.click(screen.getByRole('button', {name: 'Edit question'}));
    fireEvent.click(screen.getByRole('button', {name: 'Edit Cypher'}));
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    expect(api.analysis.executeText2Gql).toHaveBeenLastCalledWith(
        'DEFAULT', 'hugegraph', 'cypher', {content: 'MATCH (n) RETURN n LIMIT 4', text: 'Find people', read_only: true},
        expect.any(Object)
    );
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate Gremlin'})));
    expect(screen.getByTestId('query-content')).toHaveTextContent('g.E().limit(3)');
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate Cypher'})));
    expect(screen.getByTestId('query-content')).toHaveTextContent('MATCH (n) RETURN n LIMIT 4');
});


it.each([
    ['Gremlin', 'history'], ['Gremlin', 'favorite'], ['Cypher', 'history'], ['Cypher', 'favorite'],
])('keeps the current %s result when loading a %s statement', async (mode, source) => {
    const graph = {graph_view: {vertices: [{id: 'visible-result'}], edges: []}};
    api.analysis.getExecutionQuery.mockResolvedValue({status: 200, data: graph});
    api.analysis.getCypherExecutionQuery.mockResolvedValue({status: 200, data: graph});
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <AnalysisHome />
        </GraphAnalysisContext.Provider>
    );
    await waitForInitialData();
    if (mode === 'Cypher') {
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Cypher'})));
    }
    const content = mode === 'Gremlin' ? 'g.V().limit(5)' : 'MATCH (n) RETURN n LIMIT 5';
    const records = {status: 200, data: {records: [{content, text: 'Old question'}], total: 1}};
    api.analysis.getExecutionLogs.mockResolvedValue(records);
    api.analysis.fetchFavoriteQueries.mockResolvedValue(records);
    await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
    expect(screen.getByTestId('query-result')).toHaveTextContent(JSON.stringify(graph));
    const status = screen.getByTestId('query-status').textContent;
    fireEvent.click(screen.getByRole('button', {name: `Load ${source} statement`}));
    expect(screen.getByTestId('query-content')).toHaveTextContent(content);
    expect(screen.getByTestId('query-result')).toHaveTextContent(JSON.stringify(graph));
    expect(screen.getByTestId('query-status')).toHaveTextContent(status);
});

it.each(['gremlin', 'cypher'])(
    'clears Text2GQL results when loading %s statements and restores only their original question', async language => {
        const graph = {graph_view: {vertices: [{id: 'old-generated-result'}], edges: []}};
        api.analysis.executeText2Gql.mockResolvedValue({status: 200, data: graph});
        render(
            <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
                <AnalysisHome />
            </GraphAnalysisContext.Provider>
        );
        await waitForInitialData();
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Natural language'})));
        if (language === 'cypher') {
            await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate Cypher'})));
            fireEvent.click(screen.getByRole('button', {name: 'Edit Cypher'}));
        }
        else {
            fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
        }
        const content = language === 'gremlin' ? 'g.V().limit(5)' : 'MATCH (n) RETURN n LIMIT 5';
        const history = {content, text: 'Original historical question'};
        api.analysis.getExecutionLogs.mockResolvedValue({status: 200, data: {records: [history], total: 1}});
        api.analysis.fetchFavoriteQueries.mockResolvedValue({status: 200, data: {records: [{content}], total: 1}});
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
        expect(screen.getByTestId('query-result')).toHaveTextContent(JSON.stringify(graph));
        fireEvent.click(screen.getByRole('button', {name: 'Load history statement'}));
        expect(screen.getByTestId('query-result')).toHaveTextContent('{}');
        expect(screen.getByTestId('query-content')).toHaveTextContent(content);
        expect(screen.getByTestId('source-question')).toHaveTextContent(history.text);
        expect(screen.getByTestId('query-language')).toHaveTextContent(language);
        fireEvent.click(screen.getByRole('button', {name: 'Edit question'}));
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
        expect(api.analysis.executeText2Gql).toHaveBeenLastCalledWith('DEFAULT', 'hugegraph', language,
            {content, text: history.text, read_only: true}, expect.any(Object));
        expect(screen.getByTestId('query-result')).toHaveTextContent(JSON.stringify(graph));
        fireEvent.click(screen.getByRole('button', {name: 'Load favorite statement'}));
        expect(screen.getByTestId('query-result')).toHaveTextContent('{}');
        expect(screen.getByTestId('source-question')).toBeEmptyDOMElement();
        expect(screen.getByTestId('query-language')).toHaveTextContent(language);
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
        expect(api.analysis.executeText2Gql).toHaveBeenLastCalledWith('DEFAULT', 'hugegraph', language,
            {content, text: '', read_only: true}, expect.any(Object));
    }
);


it.each(['gremlin', 'cypher'])(
    'repairs a %s execution failure once and keeps failed manual edits intact', async language => {
        const query = language === 'gremlin' ? "g.V().limit('three')" : 'MATCH (n) RETRN n';
        const fixed = language === 'gremlin' ? 'g.V().limit(3)' : 'MATCH (n) RETURN n LIMIT 3';
        const diagnostic = '非法的 Gremlin 语句，详细信息: No signature of method: GraphTraversal.limit(String)';
        api.analysis.streamText2Gql.mockResolvedValueOnce({status: 200, data: {query, language, valid: true}})
            .mockResolvedValueOnce({status: 200, data: {query: fixed, language, valid: true}});
        const failure = {response: {status: 400, data: {status: 460, message: diagnostic}}};
        // Exercise both a rejected HTTP response and a resolved business failure.
        if (language === 'gremlin') {
            api.analysis.executeText2Gql.mockRejectedValueOnce(failure);
        }
        else {
            api.analysis.executeText2Gql.mockResolvedValueOnce(failure.response.data);
        }
        api.analysis.executeText2Gql.mockResolvedValueOnce({status: 200, data: {}})
            .mockRejectedValueOnce(failure);
        render(
            <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
                <AnalysisHome />
            </GraphAnalysisContext.Provider>
        );
        await waitForInitialData();
        fireEvent.click(screen.getByRole('button', {name: 'Natural language'}));
        fireEvent.click(screen.getByRole('button', {name: 'Set model and question'}));
        if (language === 'cypher') {
            fireEvent.click(screen.getByRole('button', {name: 'Generate Cypher'}));
        }
        await waitFor(() => expect(screen.getByRole('button', {name: 'Generate'})).toBeEnabled());
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Generate'})));
        expect(api.analysis.streamText2Gql).toHaveBeenCalledTimes(2);
        expect(api.analysis.streamText2Gql).toHaveBeenLastCalledWith('DEFAULT', 'hugegraph',
            expect.objectContaining({language, question: 'Find people', repair: {query, error: diagnostic}}),
            expect.any(Object));
        expect(screen.getByTestId('query-content')).toHaveTextContent(fixed);
        expect(screen.getByTestId('query-status')).toHaveTextContent('success');
        fireEvent.click(screen.getByRole('button', {name: 'Edit query'}));
        await act(async () => fireEvent.click(screen.getByRole('button', {name: 'Run current'})));
        expect(api.analysis.streamText2Gql).toHaveBeenCalledTimes(2);
        expect(screen.getByTestId('query-content')).toHaveTextContent('g.E().limit(3)');
        expect(screen.getByTestId('query-status')).toHaveTextContent('failed');
        expect(screen.getByText(/query result failed/)).toHaveTextContent(diagnostic);
    }
);
