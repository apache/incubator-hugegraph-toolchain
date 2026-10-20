/*
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

import {useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';
import {message} from 'antd';
import {isEqual} from 'lodash';
import {useTranslation} from 'react-i18next';
import {useGraphSnapshot} from '../../../visualization/react';
import {adaptAnalysisGraph} from '../../../utils/analysisGraphData';
import {analysisLayout, normalizeLayoutSettings, getAnalysisStyles, applyAnalysisStyles}
    from '../../../utils/analysisGraphPresentation';
import {exportHubbleGraph, hubbleRenderId, projectHubbleGraph} from '../../../visualization/hubble/adapter';
import {mutableCopy} from '../../../visualization/model/ownership';
import {GraphDataError} from '../../../visualization/model/data';
import {filterData} from '../../../utils/filter';
import {Algorithm_Layout, GRAPH_STATUS} from '../../../utils/constants';
import * as api from '../../../api';

const EMPTY_RAW = {vertices: [], edges: []};
const EMPTY_META = {};
const EMPTY_OPTIONS = {};
const EMPTY_GRAPH = {nodes: [], edges: []};
const rawGraph = value => ({vertices: value?.vertices ?? [], edges: value?.edges ?? []});
const sameBusinessProjection = (expected, actual) => {
    if (expected.nodes.length !== actual.nodes.length || expected.edges.length !== actual.edges.length) return false;
    const nodes = new Map(actual.nodes.map(node => [node.id, node.data.raw]));
    const edges = new Map(actual.edges.map(edge => [edge.id, edge]));
    return expected.nodes.every(node => nodes.has(node.id) && isEqual(node.data.raw, nodes.get(node.id)))
        && expected.edges.every(edge => {
            const other = edges.get(edge.id);
            return other && edge.source === other.source && edge.target === other.target
                && isEqual(edge.data.raw, other.data.raw);
        });
};

/** Shared query/algorithm business boundary. A mounted controller is the only graph truth. */
export const useAnalysisGraph = ({data = EMPTY_RAW, metaData = EMPTY_META, options = EMPTY_OPTIONS,
    algorithmName, graphSpaceInfo, queryStatus}) => {
    const {t} = useTranslation();
    const [controller, setController] = useState(null);
    const controllerRef = useRef(null);
    const adoptedController = useRef(null);
    const stylesRef = useRef({});
    const previousData = useRef(data);
    const staleInput = useRef(null);
    const generation = useRef(0);
    const alive = useRef(true);
    const pendingRaw = useRef(null);
    const retainedVisibility = useRef(null);
    const scope = JSON.stringify([graphSpaceInfo.graphSpace, graphSpaceInfo.graph]);
    const liveScope = useRef({key: scope});
    if (liveScope.current.key !== scope) liveScope.current = {key: scope};
    // Identity also invalidates callbacks after A -> B -> A, before passive effects run.
    const scopeOwner = liveScope.current;
    const previousScope = useRef(scopeOwner);
    const ownerScope = useRef(null);
    const convert = useCallback((raw, normalizeOptions = {}, styles = stylesRef.current) =>
        adaptAnalysisGraph(rawGraph(raw), metaData ?? EMPTY_META, styles, options, algorithmName, normalizeOptions),
    [algorithmName, metaData, options]);
    const project = useCallback((raw, normalizeOptions = {}, styles = stylesRef.current) => {
        const projection = projectHubbleGraph(rawGraph(raw), normalizeOptions.existingNodeIds);
        return {data: convert(projection.graph, normalizeOptions, styles), pendingEdges: projection.pendingEdges};
    }, [convert]);
    const rememberPending = (projection, owner) => {
        pendingRaw.current = {owner, edges: projection.pendingEdges,
            nodeIds: new Set(projection.data.nodes.map(node => node.id)),
            awaitingCommit: true};
    };
    const [dataset, setDataset] = useState(() => {
        const projection = project(data);
        rememberPending(projection, scopeOwner);
        return {data: projection.data, key: 0, owner: scopeOwner};
    });
    const datasetRef = useRef(dataset);
    datasetRef.current = dataset;
    const labelFields = useRef(null);
    if (labelFields.current?.owner !== scopeOwner || labelFields.current.key !== dataset.key) {
        labelFields.current = {owner: scopeOwner, key: dataset.key, current: {node: new Map(), edge: new Map()}};
    }
    const hasAdoptedData = useCallback((owner, state) => {
        if (adoptedController.current === owner) return true;
        if (state.status !== 'ready' || datasetRef.current.owner !== scopeOwner
            || !sameBusinessProjection(datasetRef.current.data, state.data)) return false;
        adoptedController.current = owner;
        return true;
    }, [scopeOwner]);
    const defaultLayout = useMemo(() => analysisLayout({
        layout: Algorithm_Layout[algorithmName] ?? 'force', startId: options?.startId,
    }), [algorithmName, options?.startId]);
    const [layoutSettings, setLayoutSettings] = useState(() => defaultLayout);
    const layout = useMemo(() => normalizeLayoutSettings(layoutSettings), [layoutSettings]);
    const activeController = ownerScope.current === scopeOwner ? controller : null;
    const snapshot = useGraphSnapshot(activeController);
    const initialData = dataset.owner === scopeOwner ? dataset.data : EMPTY_GRAPH;
    const graphData = activeController && hasAdoptedData(activeController, snapshot)
        && snapshot.status !== 'idle' && snapshot.status !== 'disposed'
        ? snapshot.data : initialData;
    const currentData = useCallback(() => {
        if (liveScope.current !== scopeOwner) return EMPTY_GRAPH;
        if (ownerScope.current === scopeOwner && controllerRef.current) {
            const state = controllerRef.current.getSnapshot();
            if (hasAdoptedData(controllerRef.current, state)
                && state.status !== 'idle' && state.status !== 'disposed') return state.data;
        }
        return datasetRef.current.owner === scopeOwner ? datasetRef.current.data : EMPTY_GRAPH;
    }, [hasAdoptedData, scopeOwner]);
    const current = useCallback((token, owner) => alive.current && token === generation.current
        && liveScope.current === scopeOwner && ownerScope.current === scopeOwner
        && owner === controllerRef.current, [scopeOwner]);
    useLayoutEffect(() => {
        alive.current = true;
        return () => { alive.current = false; };
    }, []);
    useEffect(() => {
        generation.current++;
        if (previousScope.current !== scopeOwner) {
            previousScope.current = scopeOwner;
            const fresh = previousData.current !== data;
            staleInput.current = fresh ? null : {data};
            stylesRef.current = {};
            retainedVisibility.current = null;
            // A result arriving with the new scope is valid; an unchanged old input is not.
            const projection = fresh && queryStatus !== GRAPH_STATUS.LOADING ? project(data)
                : {data: EMPTY_GRAPH, pendingEdges: []};
            rememberPending(projection, scopeOwner);
            const next = projection.data;
            previousData.current = data;
            setDataset(previous => ({data: next, key: previous.key + 1, owner: scopeOwner}));
            if (ownerScope.current !== scopeOwner) {
                controllerRef.current = null;
                ownerScope.current = null;
                setController(null);
            }
            return;
        }
        if (queryStatus === GRAPH_STATUS.LOADING) return;
        if (staleInput.current?.data === data) return;
        staleInput.current = null;
        const fresh = previousData.current !== data;
        if (fresh) {
            stylesRef.current = {};
            retainedVisibility.current = null;
        }
        let next;
        if (fresh) {
            const projection = project(data);
            rememberPending(projection, scopeOwner);
            next = projection.data;
        }
        else {
            // Metadata/settings refresh the current result, never replay the original query after local
            // expansion, edit or deletion. Before canvas replacement commits, use its pending input.
            const retained = pendingRaw.current?.awaitingCommit ? datasetRef.current.data : currentData();
            next = convert(exportHubbleGraph(retained));
        }
        setDataset(previous => ({data: next, key: previous.key + (fresh ? 1 : 0), owner: scopeOwner}));
        previousData.current = data;
    }, [convert, currentData, data, project, queryStatus, scopeOwner]);
    useEffect(() => {
        setLayoutSettings(mutableCopy(defaultLayout));
    }, [defaultLayout]);
    const onReady = useCallback(value => {
        if (!alive.current || liveScope.current !== scopeOwner) return;
        const previous = controllerRef.current;
        if (!value && previous && ownerScope.current === scopeOwner) {
            if (pendingRaw.current?.owner === scopeOwner && pendingRaw.current.awaitingCommit) {
                // The fresh query owns the handoff even before its queued canvas replacement.
                // Its preceding snapshot and masks belong to the old result.
                retainedVisibility.current = null;
            }
            else if (hasAdoptedData(previous, previous.getSnapshot())) {
                // Switching to the independent 3D renderer retains one immutable final snapshot.
                const saved = previous.getSnapshot().data;
                retainedVisibility.current = {owner: scopeOwner, key: datasetRef.current.key,
                    manual: previous.getHiddenIds(), filter: previous.getHiddenIds('filter')};
                setDataset(currentDataset => ({...currentDataset, data: saved, owner: scopeOwner}));
            }
            // A remounted canvas has no owned snapshot until its first replace commits.
            // Keep the dataset and prior masks if it is released during that initial draw.
        }
        if (value !== previous) adoptedController.current = null;
        controllerRef.current = value;
        ownerScope.current = value ? scopeOwner : null;
        generation.current++;
        setController(value);
        // GraphCanvas queues its initial replacement before transferring ownership.
        // Queue both sources in that same dataset version. Clearing a restored filter
        // must still leave any independently hidden elements hidden.
        const retained = retainedVisibility.current;
        if (value && retained?.owner === scopeOwner && retained.key === datasetRef.current.key
            && (retained.manual.length || retained.filter.length)) {
            return Promise.all([
                ...(retained.manual.length ? [value.hide(retained.manual)] : []),
                ...(retained.filter.length ? [value.setHiddenIds('filter', retained.filter)] : []),
            ]);
        }
    }, [hasAdoptedData, scopeOwner]);
    const run = useCallback(async (operation, propagateError = false) => {
        const token = generation.current;
        const owner = controllerRef.current;
        if (!current(token, owner)) return undefined;
        try {
            const result = await operation();
            return current(token, owner) ? result : undefined;
        }
        catch (error) {
            if (current(token, owner)) {
                // A submitted server write owns its completion message. Let
                // its drawer distinguish a local apply failure from POST failure.
                if (propagateError) throw error;
                message.error(error instanceof Error ? error.message : String(error));
            }
            return undefined;
        }
    }, [current]);
    // A server deletion removes pending business edges incident to its formerly present node.
    // Observe every controller publication, so deleting then re-adding before React paints cannot revive them.
    const prunePending = useCallback(owner => {
        const pending = pendingRaw.current;
        if (pending?.owner !== scopeOwner || liveScope.current !== scopeOwner
            || ownerScope.current !== scopeOwner || controllerRef.current !== owner) return;
        const state = owner.getSnapshot();
        hasAdoptedData(owner, state);
        if (!pending.awaitingCommit && !pending.edges.length) return;
        if (state.status !== 'ready' || pending.snapshotData === state.data) return;
        pending.snapshotData = state.data;
        const nodeIds = new Set(state.data.nodes.map(node => node.id));
        // Dataset props reach this hook before the canvas commits replacement. Do not interpret the
        // previous ready snapshot as a deletion in the new result (including an empty edge-only view).
        if (pending.awaitingCommit) {
            if (datasetRef.current.owner !== scopeOwner
                || !sameBusinessProjection(datasetRef.current.data, state.data)) return;
            pending.awaitingCommit = false;
            pending.nodeIds = nodeIds;
            return;
        }
        const removed = new Set([...pending.nodeIds].filter(id => !nodeIds.has(id)));
        if (removed.size) pending.edges = pending.edges.filter(edge =>
            !removed.has(hubbleRenderId('node', edge.source)) && !removed.has(hubbleRenderId('node', edge.target)));
        pending.nodeIds = nodeIds;
    }, [hasAdoptedData, scopeOwner]);
    useEffect(() => {
        if (!activeController) return undefined;
        prunePending(activeController);
        return activeController.subscribe?.(() => prunePending(activeController));
    }, [activeController, prunePending]);
    const currentRawData = useCallback(() => {
        if (!alive.current || liveScope.current !== scopeOwner) return {vertices: [], edges: []};
        const owner = ownerScope.current === scopeOwner ? controllerRef.current : null;
        if (owner) prunePending(owner);
        const pending = pendingRaw.current?.owner === scopeOwner ? pendingRaw.current : null;
        // A fresh result owns its projection before the queued canvas replacement commits.
        // Combining its pending edges with the preceding ready snapshot would mix query results.
        const source = pending?.awaitingCommit && datasetRef.current.owner === scopeOwner
            ? datasetRef.current.data : currentData();
        return exportHubbleGraph(source, pending?.edges ?? []);
    }, [currentData, prunePending, scopeOwner]);
    const clearGraph = useCallback(async () => {
        if (!alive.current || liveScope.current !== scopeOwner
            || ownerScope.current !== scopeOwner || !controllerRef.current) return undefined;
        // Invalidate an expansion before waiting for the controller queue, including a pending-only empty view.
        const token = ++generation.current;
        const owner = controllerRef.current;
        const previous = pendingRaw.current;
        const cleared = {owner: scopeOwner, edges: [], nodeIds: new Set()};
        pendingRaw.current = cleared;
        const result = await run(() => owner.replaceData(EMPTY_GRAPH, {newDataset: true}));
        if (current(token, owner) && result?.status !== 'applied' && pendingRaw.current === cleared) {
            pendingRaw.current = previous;
        }
        return result;
    }, [current, run, scopeOwner]);
    const expand = useCallback(params => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const token = generation.current;
        const response = await api.analysis.putExecutionQuery(graphSpaceInfo.graphSpace, graphSpaceInfo.graph, params);
        if (!current(token, owner)) return;
        if (response.status !== 200) {
            if (!response.message) message.error(t('analysis.query_result.expand_failed'));
            return;
        }
        prunePending(owner);
        const pending = pendingRaw.current?.owner === scopeOwner ? pendingRaw.current.edges : [];
        const responseGraph = rawGraph(response.data?.graph_view);
        const edges = new Map(pending.map(edge => [hubbleRenderId('edge', edge.id), edge]));
        const responseIds = new Set();
        responseGraph.edges.forEach(edge => {
            const id = hubbleRenderId('edge', edge.id);
            if (responseIds.has(id)) throw new GraphDataError(`Duplicate edge ID ${id}`);
            responseIds.add(id);
            edges.set(id, edge);
        });
        const projection = project({...responseGraph, edges: [...edges.values()]},
            {existingNodeIds: new Set(owner.getSnapshot().data.nodes.map(node => node.id))});
        const result = await owner.mergeData(projection.data, {preserveAppearance: true});
        if (current(token, owner) && result.status === 'applied') {
            pendingRaw.current = {owner: scopeOwner, edges: projection.pendingEdges,
                nodeIds: new Set(owner.getSnapshot().data.nodes.map(node => node.id))};
            if (!result.value && !projection.pendingEdges.length) message.warning(t('analysis.query_result.no_more_neighbors'));
        }
        return result;
    }), [current, graphSpaceInfo.graph, graphSpaceInfo.graphSpace, project, prunePending, run, scopeOwner, t]);
    const addElements = useCallback(raw => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const next = convert(raw, {existingNodeIds: new Set(owner.getSnapshot().data.nodes.map(node => node.id))});
        return owner.updateElements(next);
    }, true), [convert, run]);
    const editElement = useCallback((kind, id, patch) => run(async () => {
        const owner = controllerRef.current;
        const datum = owner?.getSnapshot().data[kind]?.find(item => item.id === id);
        if (!datum) return;
        const raw = {...datum.data.raw, ...patch,
            properties: {...datum.data.raw.properties, ...patch.properties}};
        const next = convert({vertices: kind === 'nodes' ? [raw] : [], edges: kind === 'edges' ? [raw] : []},
            {existingNodeIds: new Set(owner.getSnapshot().data.nodes.map(node => node.id))},
            getAnalysisStyles(owner.getSnapshot().data));
        return owner.updateElements(next);
    }, true), [convert, run]);
    const changeStyles = useCallback(styles => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const token = generation.current;
        const changes = applyAnalysisStyles(owner.getSnapshot().data, styles);
        const result = await owner.updateElements(changes, {record: true, resetHistory: false});
        if (current(token, owner) && result.status === 'applied') stylesRef.current = styles;
        return result;
    }), [current, run]);
    const changeSettings = useCallback(patches => run(() => controllerRef.current?.applyStyle(patches)), [run]);
    const changeLayout = useCallback(selection => {
        if (!alive.current || liveScope.current !== scopeOwner) return;
        setLayoutSettings(mutableCopy(selection));
    }, [scopeOwner]);
    const filter = useCallback(values => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const source = exportHubbleGraph(owner.getSnapshot().data);
        const rules = values?.filter;
        const result = rules ? filterData(source, rules.rules, rules.logic) : source;
        const kept = new Set([
            ...(result?.vertices ?? []).map(node => hubbleRenderId('node', node.id)),
            ...(result?.edges ?? []).map(edge => hubbleRenderId('edge', edge.id)),
        ]);
        const hidden = [...owner.getSnapshot().data.nodes, ...owner.getSnapshot().data.edges]
            .filter(item => !kept.has(item.id)).map(item => item.id);
        return owner.setHiddenIds('filter', hidden);
    }), [run]);
    const exportPng = useCallback(fileName => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const token = generation.current;
        const result = await owner.exportPng();
        if (result.status !== 'applied' || !current(token, owner)) return;
        const link = document.createElement('a');
        link.href = result.value;
        link.download = `${String(fileName || 'graph').replace(/\.png$/i, '')}.png`;
        link.click();
    }), [current, run]);
    const styleConfigData = useMemo(() => getAnalysisStyles(graphData), [graphData]);
    return {controller: activeController, onReady, graphData, initialData, datasetKey: dataset.key, scopeKey: scope,
        labelFields: labelFields.current,
        layout, layoutSettings, defaultLayout, currentData, currentRawData, clearGraph, styleConfigData, run, expand, addElements, editElement,
        changeStyles, changeSettings, changeLayout, filter, exportPng};
};
