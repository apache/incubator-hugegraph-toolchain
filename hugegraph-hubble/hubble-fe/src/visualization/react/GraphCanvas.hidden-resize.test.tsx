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

import '@testing-library/jest-dom';
import {act, render, screen} from '@testing-library/react';
import {GraphCanvas} from './GraphCanvas';
import {createGraphController} from '../runtime';
import type {ControllerSnapshot} from '../runtime/types';
import type {GraphData} from '../model/types';

jest.mock('../runtime', () => ({createGraphController: jest.fn()}));

const factory = jest.mocked(createGraphController);
const data: GraphData = {nodes: [{id: 'a', data: {raw: {id: 'a'}}}], edges: []};
const snapshot: ControllerSnapshot = {
    status: 'ready', data, canUndo: false, canRedo: false, selectedIds: [], hoveredIds: [],
};

let observerCallback: ResizeObserverCallback;
let observe: jest.Mock;
let disconnect: jest.Mock;

const controller = {
    replaceData: jest.fn().mockResolvedValue({status: 'applied', value: undefined}),
    setLayout: jest.fn().mockResolvedValue(undefined),
    resize: jest.fn().mockResolvedValue(undefined),
    fitView: jest.fn().mockResolvedValue(undefined),
    dispose: jest.fn().mockResolvedValue(undefined),
    getSnapshot: () => snapshot,
    subscribe: jest.fn(() => () => {}),
};

beforeEach(() => {
    jest.clearAllMocks();
    observe = jest.fn();
    disconnect = jest.fn();
    factory.mockReturnValue(controller as unknown as ReturnType<typeof createGraphController>);
    global.ResizeObserver = class {
        observe = observe;
        unobserve = jest.fn();
        disconnect = disconnect;

        constructor(callback: ResizeObserverCallback) {
            observerCallback = callback;
        }
    } as unknown as typeof ResizeObserver;
});

test('refits once when a hidden interactive host first receives a positive size', async () => {
    render(<GraphCanvas data={data} preset='interactive' />);
    await act(async () => {});

    const host = screen.getByRole('region');
    expect(observe).toHaveBeenCalledWith(host);
    expect(controller.fitView).not.toHaveBeenCalled();

    await act(async () => {
        observerCallback([{target: host, contentRect: {width: 640, height: 240}}]
            as unknown as ResizeObserverEntry[], {} as ResizeObserver);
    });
    expect(controller.resize).toHaveBeenLastCalledWith(640, 240);
    expect(controller.fitView).toHaveBeenCalledTimes(1);

    await act(async () => {
        observerCallback([{target: host, contentRect: {width: 800, height: 320}}]
            as unknown as ResizeObserverEntry[], {} as ResizeObserver);
    });
    expect(controller.resize).toHaveBeenCalledTimes(2);
    expect(controller.resize).toHaveBeenLastCalledWith(800, 320);
    expect(controller.fitView).toHaveBeenCalledTimes(1);
});
