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

export type UpdateResult<T> = {status: 'applied'; value: T}
    | {status: 'superseded' | 'disposed'};

export interface UpdateContext {
    /** Check again after every engine await before publishing or mutating data. */
    isCurrent(): boolean;
}
interface QueueItem {
    version: number | null;
    key?: string;
    run(): Promise<void>;
    cancel(status: 'superseded' | 'disposed'): void;
}
interface SubmitOptions {
    /** null scopes a camera/read operation to the instance, rather than its dataset. */
    version?: number | null;
    /** Only the immediately adjacent queued update with this key can be replaced. */
    key?: string;
}

/** One owner, one serial engine chain. Expected cancellation always settles callers. */
export class UpdateQueue {
    private pending: QueueItem[] = [];
    private running = false;
    private scheduled = false;
    private alive = true;
    private dataVersion = 0;
    private idleListeners = new Set<() => void>();

    get version(): number {
        return this.dataVersion;
    }

    nextDataVersion(): number {
        this.dataVersion++;
        this.pending = this.pending.filter(item => {
            if (item.version !== null && item.version !== this.dataVersion) {
                item.cancel('superseded');
                return false;
            }
            return true;
        });
        return this.dataVersion;
    }

    submit<T>(run: (context: UpdateContext) => Promise<T> | T,
        options: SubmitOptions = {}): Promise<UpdateResult<T>> {
        if (!this.alive) {
            return Promise.resolve({status: 'disposed'});
        }
        const version = options.version === undefined ? this.dataVersion : options.version;
        return new Promise((resolve, reject) => {
            const current = () => this.alive && (version === null || version === this.dataVersion);
            const item: QueueItem = {
                version,
                ...(options.key ? {key: options.key} : {}),
                cancel: status => resolve({status}),
                run: async () => {
                    if (!current()) {
                        resolve({status: this.alive ? 'superseded' : 'disposed'});
                        return;
                    }
                    try {
                        const value = await run({isCurrent: current});
                        resolve(current() ? {status: 'applied', value}
                            : {status: this.alive ? 'superseded' : 'disposed'});
                    }
                    catch (error) {
                        if (current()) reject(error);
                        else resolve({status: this.alive ? 'superseded' : 'disposed'});
                    }
                },
            };
            const previous = this.pending[this.pending.length - 1];
            if (options.key && previous?.key === options.key && previous.version === version) {
                previous.cancel('superseded');
                this.pending[this.pending.length - 1] = item;
            }
            else {
                this.pending.push(item);
            }
            this.schedule();
        });
    }

    private schedule(): void {
        if (this.running || this.scheduled) {
            return;
        }
        this.scheduled = true;
        queueMicrotask(() => {
            this.scheduled = false;
            // run() catches command errors and settles that command. The chain
            // can still process later commands, including after a failed draw.
            void this.drain();
        });
    }

    private async drain(): Promise<void> {
        if (this.running) {
            return;
        }
        this.running = true;
        try {
            while (this.pending.length) {
                await this.pending.shift()!.run();
            }
        }
        finally {
            this.running = false;
            this.notifyIdle();
        }
    }

    private notifyIdle(): void {
        if (!this.running && this.pending.length === 0) {
            this.idleListeners.forEach(resolve => resolve());
            this.idleListeners.clear();
        }
    }

    idle(): Promise<void> {
        if (!this.running && this.pending.length === 0) {
            return Promise.resolve();
        }
        return new Promise(resolve => this.idleListeners.add(resolve));
    }

    /** Invalidate first; the owner releases the engine after the active await settles. */
    dispose(): Promise<void> {
        this.alive = false;
        this.pending.splice(0).forEach(item => item.cancel('disposed'));
        this.notifyIdle();
        return this.idle();
    }
}
