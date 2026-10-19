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

import {UpdateQueue} from './UpdateQueue';

const deferred = () => {
    let resolve!: () => void;
    const promise = new Promise<void>(done => { resolve = done; });
    return {promise, resolve};
};

test('serializes work and only replaces adjacent pending updates', async () => {
    const queue = new UpdateQueue();
    const barrier = deferred();
    const order: string[] = [];
    const active = queue.submit(async () => {
        order.push('active');
        await barrier.promise;
    });
    await Promise.resolve();
    const first = queue.submit(() => order.push('old-style'), {key: 'style'});
    const last = queue.submit(() => order.push('style'), {key: 'style'});
    const layout = queue.submit(() => order.push('layout'), {key: 'layout'});
    const styleAfterLayout = queue.submit(() => order.push('style-after-layout'), {key: 'style'});
    expect(await first).toEqual({status: 'superseded'});
    expect(order).toEqual(['active']);
    barrier.resolve();
    await Promise.all([active, last, layout, styleAfterLayout]);
    expect(order).toEqual(['active', 'style', 'layout', 'style-after-layout']);
});

test('drops stale datasets and makes an active await unable to publish stale results', async () => {
    const queue = new UpdateQueue();
    const barrier = deferred();
    let published = false;
    const active = queue.submit(async context => {
        await barrier.promise;
        if (context.isCurrent()) {
            published = true;
        }
    });
    await Promise.resolve();
    const stale = queue.submit(() => 'stale merge');
    const version = queue.nextDataVersion();
    const next = queue.submit(() => 'new dataset', {version});
    const camera = queue.submit(() => 'camera', {version: null});
    expect(await stale).toEqual({status: 'superseded'});
    barrier.resolve();
    expect(await active).toEqual({status: 'superseded'});
    expect(await next).toEqual({status: 'applied', value: 'new dataset'});
    expect(await camera).toEqual({status: 'applied', value: 'camera'});
    expect(published).toBe(false);
});

test('settles disposal without callbacks after an active render', async () => {
    const queue = new UpdateQueue();
    const barrier = deferred();
    let engineReleased = false;
    let published = false;
    const active = queue.submit(async context => {
        await barrier.promise;
        if (context.isCurrent()) {
            published = true;
        }
    });
    await Promise.resolve();
    const pending = queue.submit(() => { throw new Error('must not run'); });
    const disposed = queue.dispose().then(() => { engineReleased = true; });
    expect(await pending).toEqual({status: 'disposed'});
    expect(engineReleased).toBe(false);
    barrier.resolve();
    await disposed;
    expect(await active).toEqual({status: 'disposed'});
    expect(published).toBe(false);
    expect(engineReleased).toBe(true);
    expect(await queue.submit(() => 1)).toEqual({status: 'disposed'});
});

test('handles a failed command and continues the same instance queue', async () => {
    const queue = new UpdateQueue();
    const failed = queue.submit(() => { throw new Error('draw failed'); });
    const next = queue.submit(() => 2);
    await expect(failed).rejects.toThrow('draw failed');
    expect(await next).toEqual({status: 'applied', value: 2});
    await queue.idle();
});


test('a rejected stale render settles as superseded instead of reporting an obsolete failure', async () => {
    const queue = new UpdateQueue();
    const barrier = deferred();
    const stale = queue.submit(async () => { await barrier.promise; throw new Error('obsolete render'); });
    await Promise.resolve();
    queue.nextDataVersion();
    barrier.resolve();
    expect(await stale).toEqual({status: 'superseded'});
    expect(await queue.submit(() => 'latest')).toEqual({status: 'applied', value: 'latest'});
});
