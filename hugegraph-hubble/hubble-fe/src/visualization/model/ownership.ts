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

import {GraphDataError} from './data';

// Reuse only copies created here; Object.isFrozen alone does not imply deep ownership.
const owned = new WeakSet<object>();
const copyValue = <T>(value: T, freeze: boolean, seen = new WeakMap<object, object>(),
    ancestors = new WeakSet<object>()): T => {
    if (typeof value === 'function' || typeof value === 'symbol') {
        throw new GraphDataError('Graph records must contain data values');
    }
    if (value === null || typeof value !== 'object') return value;
    if (freeze && owned.has(value)) return value;
    // A repeated ancestor is a cycle; a completed branch may legitimately share a record.
    if (ancestors.has(value)) throw new GraphDataError('Graph records cannot contain cycles');
    const previous = seen.get(value);
    if (previous) return previous as T;
    const prototype: unknown = Object.getPrototypeOf(value);
    const constructor = value.constructor as {isBigNumber?: (item: unknown) => boolean} | undefined;
    if (!Array.isArray(value) && prototype !== Object.prototype && prototype !== null
        && !constructor?.isBigNumber?.(value)) {
        throw new GraphDataError('Graph records must contain JSON values or lossless numbers');
    }
    const result: object = Array.isArray(value) ? [] : Object.create(prototype as object | null);
    seen.set(value, result);
    ancestors.add(value);
    try {
        for (const key of Reflect.ownKeys(value)) {
            if (Array.isArray(value) && key === 'length') continue;
            const descriptor = Object.getOwnPropertyDescriptor(value, key)!;
            if (!('value' in descriptor)) throw new GraphDataError('Graph records cannot contain accessors');
            Object.defineProperty(result, key, {value: copyValue(descriptor.value, freeze, seen, ancestors),
                enumerable: descriptor.enumerable, writable: true, configurable: true});
        }
        if (Array.isArray(value)) Reflect.set(result, 'length', value.length);
        if (freeze) { Object.freeze(result); owned.add(result); }
        return result as T;
    } finally {
        ancestors.delete(value);
    }
};
/** Detach caller records. Controller snapshots can reuse these owned frozen records. */
export const immutableCopy = <T>(value: T): T => copyValue(value, true);
/** Engine data and style values must stay writable and detached from public snapshots. */
export const mutableCopy = <T>(value: T): T => copyValue(value, false);
