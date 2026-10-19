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

// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';
import {MessageChannel as NodeMessageChannel} from 'node:worker_threads';

// jsdom has no layout engine. Unit tests mount real components; Chrome
// acceptance verifies measurements and actual ResizeObserver behaviour.
if (typeof window !== 'undefined') {
    const browserComputedStyle = window.getComputedStyle;
    window.getComputedStyle = element => browserComputedStyle.call(window, element);
    // rc-form uses a real browser macrotask. Native Node ports preserve that
    // scheduling, while unref prevents their listeners from holding Jest open.
    const channels = new Set();
    globalThis.MessageChannel = class extends NodeMessageChannel {
        constructor() {
            super();
            channels.add(this);
            for (const port of [this.port1, this.port2]) {
                let prototype = port;
                let descriptor;
                while (prototype && !descriptor) {
                    descriptor = Object.getOwnPropertyDescriptor(prototype, 'onmessage');
                    prototype = Object.getPrototypeOf(prototype);
                }
                if (!descriptor?.set || !descriptor?.get) {
                    throw new Error('Node MessagePort must expose onmessage');
                }
                Object.defineProperty(port, 'onmessage', {
                    configurable: true,
                    get: () => descriptor.get.call(port),
                    set: listener => {
                        descriptor.set.call(port, listener);
                        port.unref();
                    },
                });
                port.unref();
            }
        }
    };
    afterAll(() => {
        channels.forEach(channel => {
            channel.port1.close();
            channel.port2.close();
        });
        channels.clear();
    });
    globalThis.ResizeObserver = class {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
}
