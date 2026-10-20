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

import {describe, expect, jest, test} from '@jest/globals';
import {GraphDataError} from './data';
import {immutableCopy, mutableCopy} from './ownership';

describe.each([
    ['immutable', immutableCopy],
    ['mutable', mutableCopy],
] as const)('%s record ownership', (_name, copy) => {
    test('rejects a direct cycle even when its source is frozen', () => {
        const raw: Record<string, unknown> = {id: 'a'};
        raw.self = raw;
        Object.freeze(raw);
        expect(() => copy(raw)).toThrow(GraphDataError);
        expect(() => copy(raw)).toThrow('Graph records cannot contain cycles');
    });

    test('rejects an indirect cycle crossing object and array branches', () => {
        const raw: Record<string, unknown> = {id: 'a'};
        raw.properties = {path: [raw]};
        expect(() => copy(raw)).toThrow('Graph records cannot contain cycles');
    });

    test('keeps a shared descendant as one detached copy without rejecting a DAG', () => {
        const shared = {values: [1, 2]};
        const raw = {left: shared, right: {shared}, list: [shared]};
        const result = copy(raw);
        expect(result).not.toBe(raw);
        expect(result.left).not.toBe(shared);
        expect(result.left).toBe(result.right.shared);
        expect(result.left).toBe(result.list[0]);
        shared.values.push(3);
        expect(result.left.values).toEqual([1, 2]);
    });

    test('preserves lossless number prototypes and native bigint IDs', () => {
        const json = jest.requireActual('json-bigint') as (options: {alwaysParseAsBig: boolean}) => {
            parse(text: string): Record<string, unknown>; stringify(value: unknown): string;
        };
        const lossless = json({alwaysParseAsBig: true});
        const number = lossless.parse('{"id":900719925474099312345}').id;
        const raw = {number, native: 900719925474099312346n};
        const result = copy(raw);
        expect(result.number).not.toBe(number);
        expect(Object.getPrototypeOf(result.number)).toBe(Object.getPrototypeOf(number));
        expect(lossless.stringify(result.number)).toBe('900719925474099312345');
        expect(result.native).toBe(900719925474099312346n);
    });
});

test('reuses only owned deep-frozen records while detaching shallow-frozen input', () => {
    const nested = {value: 1};
    const input = Object.freeze({nested});
    const result = immutableCopy(input);
    expect(result).not.toBe(input);
    expect(result.nested).not.toBe(nested);
    expect(Object.isFrozen(result)).toBe(true);
    expect(Object.isFrozen(result.nested)).toBe(true);
    expect(immutableCopy(result)).toBe(result);
    expect(immutableCopy({nested: result.nested}).nested).toBe(result.nested);
    expect(Object.isFrozen(nested)).toBe(false);
});

test('makes frozen engine style arrays writable while keeping shared descendants', () => {
    const point = {x: 1, y: 2};
    const snapshot = immutableCopy({controlPoints: [point, point]});
    const result = mutableCopy(snapshot);
    expect(result.controlPoints[0]).toBe(result.controlPoints[1]);
    expect(result.controlPoints[0]).not.toBe(snapshot.controlPoints[0]);
    result.controlPoints[0].x = 3;
    result.controlPoints.push({x: 4, y: 5});
    expect(result.controlPoints).toEqual([{x: 3, y: 2}, {x: 3, y: 2}, {x: 4, y: 5}]);
    expect(snapshot.controlPoints).toEqual([{x: 1, y: 2}, {x: 1, y: 2}]);
});
