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

import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.E;

public class BatchInsertTask<GE extends GraphElement>
       implements Supplier<Integer> {

    private static final String ILLEGAL_ARGUMENT_EXCEPTION =
            "class java.lang.IllegalArgumentException";

    private final ElemType type;
    private final List<GE> batch;
    private final LoadOptions options;

    public BatchInsertTask(ElemType type, List<GE> batch, LoadOptions options) {
        E.checkArgument(batch != null && !batch.isEmpty(),
                        "The batch can't be null or empty");
        this.type = type;
        this.batch = batch;
        this.options = options;
    }

    @Override
    public Integer get() {
        int retryCount = 0;
        do {
            try {
                HugeClientWrapper.addBatch(this.type, this.batch,
                                           this.options.checkVertex);
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

    private int waitThenRetry(int retryCount, RuntimeException e) {
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
}
