/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.loader.task;

import java.util.List;
import java.util.function.Supplier;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.GraphElement;

public abstract class InsertionTask<E extends GraphElement>
       implements Supplier<Integer> {

    private static final String ILLEGAL_ARGUMENT_EXCEPTION =
            "class java.lang.IllegalArgumentException";

    private final List<E> batch;
    private final LoadOptions options;
    private final HugeClient client;

    public InsertionTask(List<E> batch, LoadOptions options) {
        this.batch = batch;
        this.options = options;
        this.client = HugeClientWrapper.get(options);
    }

    public List<E> batch() {
        return this.batch;
    }

    public LoadOptions options() {
        return this.options;
    }

    public HugeClient client() {
        return this.client;
    }

    @Override
    public Integer get() {
        if (this.batch == null || this.batch.isEmpty()) {
            return 0;
        }

        int retryCount = 0;
        do {
            try {
                this.execute();
                break;
            } catch (ClientException e) {
                retryCount = this.waitThenRetry(retryCount, e);
            } catch (ServerException e) {
                if (ILLEGAL_ARGUMENT_EXCEPTION.equals(e.exception())) {
                    throw e;
                }
                retryCount = this.waitThenRetry(retryCount, e);
            }
        } while (retryCount > 0 && retryCount <= this.options.retryTimes);

        return this.batch.size();
    }

    protected int waitThenRetry(int retryCount, RuntimeException e) {
        try {
            Thread.sleep(this.options.retryInterval * 1000);
        } catch (InterruptedException ignored) {
            // That's fine, just continue.
        }

        if (++retryCount > this.options.retryTimes) {
            throw e;
        }
        return retryCount;
    }

    protected abstract void execute();
}
