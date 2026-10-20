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

import {DragElement} from '@antv/g6';

// Exercise the published behavior's real Shortcut against native window events.
// Rendering is covered by the real-browser lifecycle contract; this context
// supplies only the emitter and canvas boundary needed during construction.
test('destroy releases native modifier and focus listeners on every mount', () => {
    const add = jest.spyOn(window, 'addEventListener');
    const remove = jest.spyOn(window, 'removeEventListener');
    const types = ['keydown', 'keyup', 'focus'];
    try {
        for (let cycle = 0; cycle < 3; cycle++) {
            add.mockClear(); remove.mockClear();
            const canvas = document.createElement('canvas');
            const context = {
                graph: {on: jest.fn(), off: jest.fn()},
                canvas: {getLayer: () => ({getContextService: () => ({$canvas: canvas})})},
            } as unknown as ConstructorParameters<typeof DragElement>[0];
            const behavior = new DragElement(context, {type: 'drag-element'});
            const owned = add.mock.calls.filter(([type]) => types.includes(type));
            try {
                expect(owned.map(([type]) => type).sort()).toEqual([...types].sort());
            } finally { behavior.destroy(); }
            for (const [type, listener] of owned) {
                expect(remove).toHaveBeenCalledWith(type, listener);
            }
        }
    } finally { add.mockRestore(); remove.mockRestore(); }
});
