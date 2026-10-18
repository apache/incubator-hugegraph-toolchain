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

package org.apache.hugegraph.config;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Completes metadata pool cleanup even when the context-closing thread is interrupted.
 */
public class HubbleDataSource extends HikariDataSource {

    private FutureTask<Void> shutdown;

    @Override
    public void close() {
        FutureTask<Void> closing = this.startShutdown();
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    closing.get();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw new IllegalStateException("Failed to close the metadata datasource", cause);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized FutureTask<Void> startShutdown() {
        if (this.shutdown == null) {
            // Hikari marks itself closed before its interruptible cleanup. Retrying
            // close cannot finish an interrupted shutdown, so isolate that work
            // from both pending and newly arriving interrupts on the caller.
            FutureTask<Void> closing = new FutureTask<>(() -> {
                super.close();
                return null;
            });
            Thread worker = new Thread(closing, "hubble-metadata-close");
            worker.start();
            this.shutdown = closing;
        }
        // Concurrent and repeated callers must await the same complete cleanup,
        // including callers that already observe Hikari's closed flag.
        return this.shutdown;
    }
}
