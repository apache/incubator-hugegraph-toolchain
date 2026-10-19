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
import {useTranslation} from 'react-i18next';
import {useGraphSnapshot} from '../../../visualization/react';
import {adaptAnalysisGraph} from '../../../utils/analysisGraphData';
import {analysisLayout, normalizeLayoutSettings, getAnalysisStyles, applyAnalysisStyles}
    from '../../../utils/analysisGraphPresentation';
import {exportHubbleGraph, hubbleRenderId} from '../../../visualization/hubble/adapter';
import {filterData} from '../../../utils/filter';
import {Algorithm_Layout, GRAPH_STATUS} from '../../../utils/constants';
import * as api from '../../../api';

const EMPTY_RAW = {vertices: [], edges: []};
const EMPTY_META = {};
const EMPTY_OPTIONS = {};
const EMPTY_GRAPH = {nodes: [], edges: []};
const rawGraph = value => ({vertices: value?.vertices ?? [], edges: value?.edges ?? []});

/** Shared query/algorithm business boundary. A mounted controller is the only graph truth. */
export const useAnalysisGraph = ({data = EMPTY_RAW, metaData = EMPTY_META, options = EMPTY_OPTIONS,
    algorithmName, graphSpaceInfo, queryStatus}) => {
    const {t} = useTranslation();
    const [controller, setController] = useState(null);
    const controllerRef = useRef(null);
    const stylesRef = useRef({});
    const previousData = useRef(data);
    const staleInput = useRef(null);
    const generation = useRef(0);
    const alive = useRef(true);
    const filteredIds = useRef(new Set());
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
    const [dataset, setDataset] = useState(() => ({data: convert(data), key: 0, owner: scopeOwner}));
    const datasetRef = useRef(dataset);
    datasetRef.current = dataset;
    const [layout, setCurrentLayout] = useState(() => analysisLayout({
        layout: Algorithm_Layout[algorithmName] ?? 'force', startId: options?.startId,
    }));
    const activeController = ownerScope.current === scopeOwner ? controller : null;
    const snapshot = useGraphSnapshot(activeController);
    const initialData = dataset.owner === scopeOwner ? dataset.data : EMPTY_GRAPH;
    const graphData = activeController && snapshot.status !== 'idle' && snapshot.status !== 'disposed'
        ? snapshot.data : initialData;
    const currentData = useCallback(() => {
        if (liveScope.current !== scopeOwner) return EMPTY_GRAPH;
        if (ownerScope.current === scopeOwner && controllerRef.current) return controllerRef.current.getSnapshot().data;
        return datasetRef.current.owner === scopeOwner ? datasetRef.current.data : EMPTY_GRAPH;
    }, [scopeOwner]);
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
            filteredIds.current.clear();
            // A result arriving with the new scope is valid; an unchanged old input is not.
            const next = fresh && queryStatus !== GRAPH_STATUS.LOADING ? convert(data) : EMPTY_GRAPH;
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
        if (fresh) { stylesRef.current = {}; filteredIds.current.clear(); }
        const next = convert(data);
        setDataset(previous => ({data: next, key: previous.key + (fresh ? 1 : 0), owner: scopeOwner}));
        previousData.current = data;
    }, [convert, data, queryStatus, scopeOwner]);
    useEffect(() => {
        setCurrentLayout(analysisLayout({layout: Algorithm_Layout[algorithmName] ?? 'force', startId: options?.startId}));
    }, [algorithmName, options?.startId]);
    const onReady = useCallback(value => {
        if (!alive.current || liveScope.current !== scopeOwner) return;
        const previous = controllerRef.current;
        if (!value && previous && ownerScope.current === scopeOwner) {
            // Switching to the independent 3D renderer retains one immutable final snapshot.
            const saved = previous.getSnapshot().data;
            setDataset(currentDataset => ({...currentDataset, data: saved, owner: scopeOwner}));
        }
        controllerRef.current = value;
        ownerScope.current = value ? scopeOwner : null;
        generation.current++;
        setController(value);
    }, [scopeOwner]);
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
        const next = convert(response.data?.graph_view, {existingNodeIds: new Set(owner.getSnapshot().data.nodes.map(node => node.id))});
        const result = await owner.mergeData(next);
        if (current(token, owner) && result.status === 'applied' && !result.value) {
            message.warning(t('analysis.query_result.no_more_neighbors'));
        }
        return result;
    }), [convert, current, graphSpaceInfo.graph, graphSpaceInfo.graphSpace, run, t]);
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
    }), [convert, run]);
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
        setCurrentLayout(normalizeLayoutSettings(selection));
    }, [scopeOwner]);
    const filter = useCallback(values => run(async () => {
        const owner = controllerRef.current;
        if (!owner) return;
        const token = generation.current;
        const source = exportHubbleGraph(owner.getSnapshot().data);
        const rules = values?.filter;
        const result = rules ? filterData(source, rules.rules, rules.logic) : source;
        const kept = new Set([
            ...(result?.vertices ?? []).map(node => hubbleRenderId('node', node.id)),
            ...(result?.edges ?? []).map(edge => hubbleRenderId('edge', edge.id)),
        ]);
        const hidden = new Set([...owner.getSnapshot().data.nodes, ...owner.getSnapshot().data.edges]
            .filter(item => !kept.has(item.id)).map(item => item.id));
        const show = [...filteredIds.current].filter(id => !hidden.has(id));
        const hide = [...hidden].filter(id => !filteredIds.current.has(id));
        if (show.length) await owner.hide(show, false);
        if (!current(token, owner)) return;
        if (hide.length) await owner.hide(hide);
        if (current(token, owner)) filteredIds.current = hidden;
    }), [current, run]);
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
        layout, currentData, styleConfigData, run, expand, addElements, editElement,
        changeStyles, changeSettings, changeLayout, filter, exportPng};
};
