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

import {Alert, Button, Select, Space} from 'antd';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {isPdEnabled} from '../../utils/config';
import {DEFAULT_GRAPHSPACE} from '../../utils/productMode';
import {
    clearWorkbenchGraphContext,
    extractWorkbenchGraphContext,
    readWorkbenchGraphContext,
    resolveWorkbenchGraphContext,
    writeWorkbenchGraphContext,
} from '../../utils/workbenchGraphContext';
import style from './index.module.scss';
import {getResourceDisplayName} from '../../utils/displayName';
import {loadGraphOptions, loadGraphspaceOptions, mergeGraphOptions} from '../../utils/graphOptions';

const INLINE_ERROR_CONFIG = {suppressBusinessErrorToast: true};
const SELECT_MIN_WIDTH = 112;
const SELECT_MAX_WIDTH = 240;

const headerSelectStyles = ({props}) => {
    const color = props.disabled
        ? 'rgba(255, 255, 255, 0.72)'
        : 'rgba(255, 255, 255, 0.92)';
    const secondary = 'rgba(255, 255, 255, 0.64)';
    return {root: {color}, input: {color}, suffix: {color: secondary}, placeholder: {color: secondary}};
};

const getSelectWidth = label => {
    const textWidth = Array.from(label ?? '').reduce(
        (width, character) => width + (character.codePointAt(0) > 255 ? 14 : 8),
        48
    );
    return Math.min(SELECT_MAX_WIDTH, Math.max(SELECT_MIN_WIDTH, textWidth));
};

const getAnalysisModule = pathname => pathname.match(
    /^\/(gremlin|algorithms|asyncTasks)(?:\/|$)/
)?.[1];

const errorStatus = error => error?.status ?? error?.response?.data?.status
                     ?? error?.response?.status;

const errorKind = error => (
    errorStatus(error) === 403 ? 'forbidden' : true
);

const unavailableGraphSpace = error => [403, 404].includes(errorStatus(error));

const GraphContextSwitcher = () => {
    const {t} = useTranslation();
    const location = useLocation();
    const navigate = useNavigate();
    const pdEnabled = isPdEnabled();
    const routeContext = useMemo(
        () => extractWorkbenchGraphContext(location.pathname),
        [location.pathname]
    );
    const [context, setContext] = useState(() => resolveWorkbenchGraphContext({
        pdEnabled,
        routeContext,
        storedContext: readWorkbenchGraphContext(),
    }));
    const [graphspaces, setGraphspaces] = useState(() => (
        pdEnabled ? [] : [{name: DEFAULT_GRAPHSPACE, nickname: DEFAULT_GRAPHSPACE}]
    ));
    const [graphs, setGraphs] = useState([]);
    const [graphspaceBrowse, setGraphspaceBrowse] = useState({query: '', pageNo: 1});
    const graphspaceRequest = useRef(null);
    const graphspaceQuery = graphspaceBrowse.query;
    const graphspacePageNo = graphspaceBrowse.pageNo;
    const [graphBrowse, setGraphBrowse] = useState({});
    const graphRequest = useRef(null);
    const graphQuery = graphBrowse.graphspace === context.graphspace ? graphBrowse.query ?? '' : '';
    const graphPageNo = graphBrowse.graphspace === context.graphspace ? graphBrowse.pageNo ?? 1 : 1;
    const graphScope = JSON.stringify([context.graphspace, context.graph]);
    const [loading, setLoading] = useState({graphspaces: pdEnabled, graphs: true});
    const [errors, setErrors] = useState({graphspaces: false, graphs: false});
    const [reloadTokens, setReloadTokens] = useState({graphspaces: 0, graphs: 0});

    useEffect(() => {
        const nextContext = resolveWorkbenchGraphContext({
            pdEnabled,
            routeContext,
            storedContext: readWorkbenchGraphContext(),
        });
        setContext(nextContext);
        if (nextContext.graphspace) {
            writeWorkbenchGraphContext(localStorage, nextContext);
        }
    }, [pdEnabled, routeContext]);

    useEffect(() => {
        let cancelled = false;
        if (!pdEnabled) {
            setGraphspaces([{name: DEFAULT_GRAPHSPACE, nickname: DEFAULT_GRAPHSPACE}]);
            setLoading(value => ({...value, graphspaces: false}));
            return undefined;
        }

        const request = Symbol('graphspace-options');
        graphspaceRequest.current = request;
        setLoading(value => ({...value, graphspaces: true}));
        const fetchPage = () => loadGraphspaceOptions({
            pageNo: graphspacePageNo,
            query: graphspaceQuery,
            selected: context.graphspace,
        }, INLINE_ERROR_CONFIG).then(options => {
            if (!cancelled) {
                setGraphspaces(current => mergeGraphOptions(
                    graphspacePageNo > 1 ? [...current, ...options.records] : options.records,
                    options.selected
                ));
                setGraphspaceBrowse({
                    query: graphspaceQuery,
                    pageNo: graphspacePageNo,
                    hasMore: options.hasMore,
                    scope: context.graphspace,
                });
                setErrors(value => ({...value, graphspaces: false}));
            }
        }).catch(error => {
            if (!cancelled) {
                setErrors(value => ({...value, graphspaces: errorKind(error)}));
            }
        }).finally(() => {
            if (!cancelled) {
                graphspaceRequest.current = null;
                setLoading(value => ({...value, graphspaces: false}));
            }
        });
        const timer = graphspaceQuery && graphspacePageNo === 1 ? setTimeout(fetchPage, 200) : undefined;
        if (!timer) {
            fetchPage();
        }
        return () => {
            cancelled = true;
            clearTimeout(timer);
            if (graphspaceRequest.current === request) {
                graphspaceRequest.current = null;
            }
        };
    }, [context.graphspace, graphspacePageNo, graphspaceQuery, pdEnabled, reloadTokens.graphspaces]);

    useEffect(() => {
        let cancelled = false;
        if (!context.graphspace) {
            setGraphs([]);
            setLoading(value => ({...value, graphs: false}));
            return undefined;
        }

        const request = Symbol('graph-options');
        graphRequest.current = request;
        setLoading(value => ({...value, graphs: true}));
        setErrors(value => ({...value, graphs: false}));
        const fetchPage = () => loadGraphOptions(context.graphspace, {
            pageNo: graphPageNo,
            query: graphQuery,
            selected: context.graph,
        }, INLINE_ERROR_CONFIG).then(options => {
            if (!cancelled) {
                setGraphs(current => mergeGraphOptions(
                    graphPageNo > 1 ? [...current, ...options.records] : options.records,
                    options.selected
                ));
                setGraphBrowse({
                    graphspace: context.graphspace,
                    query: graphQuery,
                    pageNo: graphPageNo,
                    hasMore: options.hasMore,
                    scope: graphScope,
                });
                setErrors(value => ({...value, graphs: false}));
            }
        }).catch(error => {
            if (!cancelled) {
                const kind = errorKind(error);
                if (pdEnabled && unavailableGraphSpace(error)) {
                    clearWorkbenchGraphContext(localStorage);
                    setContext({});
                    setGraphs([]);
                    setErrors(value => ({...value, graphs: false}));
                    navigate('/navigation', {replace: true});
                }
                else {
                    setErrors(value => ({...value, graphs: kind}));
                }
            }
        }).finally(() => {
            if (!cancelled) {
                graphRequest.current = null;
                setLoading(value => ({...value, graphs: false}));
            }
        });
        // Avoid rebuilding the upstream profile list for every search keystroke.
        const timer = graphQuery && graphPageNo === 1 ? setTimeout(fetchPage, 200) : undefined;
        if (!timer) {
            fetchPage();
        }
        return () => {
            cancelled = true;
            clearTimeout(timer);
            if (graphRequest.current === request) {
                graphRequest.current = null;
            }
        };
    }, [context.graph, context.graphspace, graphPageNo, graphQuery, graphScope, navigate, pdEnabled,
        reloadTokens.graphs]);

    useEffect(() => {
        if (!pdEnabled || graphspaceBrowse.scope !== context.graphspace || loading.graphspaces ||
            errors.graphspaces || !context.graphspace || (
            graphspaces.some(item => item.name === context.graphspace)
        )) {
            return;
        }

        setContext({});
        setGraphs([]);
        clearWorkbenchGraphContext(localStorage);
        navigate('/navigation', {replace: true});
    }, [
        context.graphspace,
        errors.graphspaces,
        graphspaces,
        graphspaceBrowse.scope,
        loading.graphspaces,
        navigate,
        pdEnabled,
    ]);

    useEffect(() => {
        if (graphBrowse.scope !== graphScope || loading.graphs || errors.graphs || !context.graph || (
            graphs.some(item => item.name === context.graph)
        )) {
            return;
        }

        const nextContext = {graphspace: context.graphspace};
        setContext(nextContext);
        writeWorkbenchGraphContext(localStorage, nextContext);
        navigate(
            `/graphspace/${encodeURIComponent(context.graphspace)}`,
            {replace: true}
        );
    }, [
        context.graph,
        context.graphspace,
        errors.graphs,
        graphBrowse.scope,
        graphScope,
        graphs,
        loading.graphs,
        navigate,
    ]);

    const graphspaceOptions = useMemo(() => {
        if (!context.graphspace || graphspaces.some(item => item.name === context.graphspace)) {
            return graphspaces;
        }
        if (!loading.graphspaces && !errors.graphspaces) {
            return graphspaces;
        }
        return [{name: context.graphspace, nickname: context.graphspace}, ...graphspaces];
    }, [context.graphspace, errors.graphspaces, graphspaces, loading.graphspaces]);
    const selectedGraphspace = graphspaceOptions.find(item => (
        item.name === context.graphspace
    ));
    const selectedGraphspaceLabel = selectedGraphspace?.name === DEFAULT_GRAPHSPACE
        ? DEFAULT_GRAPHSPACE
        : getResourceDisplayName(
            selectedGraphspace?.name ?? context.graphspace,
            selectedGraphspace?.nickname
        );
    const selectedGraph = graphs.find(item => item.name === context.graph);
    const selectedGraphLabel = getResourceDisplayName(
        selectedGraph?.name ?? context.graph,
        selectedGraph?.nickname
    );

    const selectGraphspace = useCallback(graphspace => {
        const nextContext = {graphspace};
        setGraphspaceBrowse({query: '', pageNo: 1});
        setGraphs([]);
        setErrors(value => ({...value, graphs: false}));
        setLoading(value => ({...value, graphs: true}));
        setContext(nextContext);
        writeWorkbenchGraphContext(localStorage, nextContext);
        if (!getAnalysisModule(location.pathname)) {
            navigate(`/graphspace/${encodeURIComponent(graphspace)}`);
        }
    }, [location.pathname, navigate]);

    const selectGraph = useCallback(graph => {
        if (!context.graphspace) {
            return;
        }
        const nextContext = {graphspace: context.graphspace, graph};
        setGraphBrowse({graphspace: context.graphspace, query: '', pageNo: 1});
        setContext(nextContext);
        writeWorkbenchGraphContext(localStorage, nextContext);
        const analysisModule = getAnalysisModule(location.pathname);
        const graphspacePath = encodeURIComponent(context.graphspace);
        const graphPath = encodeURIComponent(graph);
        navigate(analysisModule
            ? `/${analysisModule}/${graphspacePath}/${graphPath}`
            : `/graphspace/${graphspacePath}/graph/${graphPath}/detail`);
    }, [context.graphspace, location.pathname, navigate]);

    const searchGraphs = useCallback(query => {
        setGraphBrowse(current => ({...current, graphspace: context.graphspace, query, pageNo: 1, hasMore: false}));
    }, [context.graphspace]);

    const searchGraphspaces = useCallback(query => {
        setGraphspaceBrowse(current => ({...current, query, pageNo: 1, hasMore: false}));
    }, []);

    const loadMoreGraphspaces = useCallback(event => {
        const list = event.currentTarget;
        if (!graphspaceBrowse.hasMore || graphspaceRequest.current || loading.graphspaces ||
            list.scrollTop + list.clientHeight < list.scrollHeight - 16) {
            return;
        }
        graphspaceRequest.current = Symbol('next-graphspace-page');
        setGraphspaceBrowse(current => ({...current, pageNo: current.pageNo + 1}));
    }, [graphspaceBrowse.hasMore, loading.graphspaces]);

    const loadMoreGraphs = useCallback(event => {
        const list = event.currentTarget;
        if (!graphBrowse.hasMore || graphRequest.current || loading.graphs ||
            list.scrollTop + list.clientHeight < list.scrollHeight - 16) {
            return;
        }
        graphRequest.current = Symbol('next-graph-page');
        setGraphBrowse(current => ({...current, pageNo: (current.pageNo ?? 1) + 1}));
    }, [graphBrowse.hasMore, loading.graphs]);

    const retryGraphspaces = useCallback(() => {
        setLoading(value => ({...value, graphspaces: true}));
        setErrors(value => ({...value, graphspaces: false}));
        setReloadTokens(value => ({...value, graphspaces: value.graphspaces + 1}));
    }, []);

    const retryGraphs = useCallback(() => {
        setLoading(value => ({...value, graphs: true}));
        setErrors(value => ({...value, graphs: false}));
        setReloadTokens(value => ({...value, graphs: value.graphs + 1}));
    }, []);

    return (
        <div
            className={`${style.context} ${!pdEnabled ? style.fixedGraphspace : ''}`}
            role="group"
            aria-label={t('workbench.context.name')}
            aria-description={selectedGraphspaceLabel}
        >
            <Space size={8}>
                <Select
                    aria-description={selectedGraphspaceLabel}
                    aria-label={t('workbench.context.graphspace')}
                    className={style.graphspace}
                    disabled={!pdEnabled}
                    loading={loading.graphspaces}
                    showSearch
                    filterOption={false}
                    onSearch={searchGraphspaces}
                    onPopupScroll={loadMoreGraphspaces}
                    onChange={selectGraphspace}
                    placeholder={t('workbench.context.select_graphspace')}
                    title={selectedGraphspaceLabel}
                    value={context.graphspace}
                    style={{width: getSelectWidth(selectedGraphspaceLabel)}}
                    styles={headerSelectStyles}
                    options={graphspaceOptions.map(item => {
                        const displayName = item.name === DEFAULT_GRAPHSPACE
                            ? DEFAULT_GRAPHSPACE
                            : getResourceDisplayName(item.name, item.nickname);
                        return {value: item.name, title: displayName, label: displayName};
                    })}
                />
                <span className={style.separator}>/</span>
                <Select
                    aria-description={selectedGraphLabel}
                    aria-label={t('workbench.context.graph')}
                    className={style.graph}
                    disabled={!context.graphspace || Boolean(errors.graphs) ||
                        loading.graphs && graphBrowse.scope !== graphScope}
                    loading={loading.graphs}
                    showSearch
                    filterOption={false}
                    onSearch={searchGraphs}
                    onPopupScroll={loadMoreGraphs}
                    onChange={selectGraph}
                    placeholder={t('workbench.context.select_graph')}
                    title={selectedGraphLabel}
                    value={context.graph}
                    style={{width: getSelectWidth(selectedGraphLabel)}}
                    styles={headerSelectStyles}
                    options={graphs.map(item => ({
                        value: item.name, label: getResourceDisplayName(item.name, item.nickname),
                    }))}
                />
            </Space>
            {(errors.graphspaces || errors.graphs) && (
                <div className={style.errorStack}>
                    {errors.graphspaces && (
                        <Alert
                            className={style.error}
                            type="error"
                            showIcon
                            title={t(errors.graphspaces === 'forbidden'
                                ? 'workbench.context.graphspaces_forbidden'
                                : 'workbench.context.graphspaces_load_failed')}
                            action={errors.graphspaces !== 'forbidden' && (
                                <Button size="small" onClick={retryGraphspaces}>
                                    {t('workbench.context.retry_graphspaces')}
                                </Button>
                            )}
                        />
                    )}
                    {errors.graphs && (
                        <Alert
                            className={style.error}
                            type="error"
                            showIcon
                            title={t(errors.graphs === 'forbidden'
                                ? 'workbench.context.graphs_forbidden'
                                : 'workbench.context.graphs_load_failed')}
                            action={errors.graphs !== 'forbidden' && (
                                <Button size="small" onClick={retryGraphs}>
                                    {t('workbench.context.retry_graphs')}
                                </Button>
                            )}
                        />
                    )}
                </div>
            )}
        </div>
    );
};

export default GraphContextSwitcher;
