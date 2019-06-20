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

import static com.baidu.hugegraph.loader.constant.Constants.BATCH_PRINT_FREQ;

import java.util.List;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;

public class BatchInsertTask<GE extends GraphElement> extends InsertTask<GE> {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private static final String ILLEGAL_ARGUMENT_EXCEPTION =
            "class java.lang.IllegalArgumentException";

    public BatchInsertTask(LoadContext context, ElementStruct struct,
                           List<GE> batch) {
        super(context, struct, batch);
    }

    @Override
    public void run() {
        int retryCount = 0;
        ElemType type = this.struct().type();
        LoadOptions options = this.context().options();
        do {
            try {
                this.addBatch(type, this.batch(), options.checkVertex);
                break;
            } catch (ClientException e) {
                retryCount = this.waitThenRetry(retryCount, e);
            } catch (ServerException e) {
                if (ILLEGAL_ARGUMENT_EXCEPTION.equals(e.exception())) {
                    throw e;
                }
                retryCount = this.waitThenRetry(retryCount, e);
            }
        } while (retryCount > 0 && retryCount <= options.retryTimes);

        LoadMetrics metrics = this.context().summary().metrics(this.struct());
        int count = this.batch().size();
        metrics.plusLoadSuccess(count);
        Printer.printProgress(type, metrics.loadSuccess(),
                              BATCH_PRINT_FREQ, count);
    }

    @SuppressWarnings("unchecked")
    private void addBatch(ElemType type, List<GE> elements, boolean check) {
        HugeClient client = HugeClientWrapper.get(this.context().options());
        if (type.isVertex()) {
            client.graph().addVertices((List<Vertex>) elements);
        } else {
            assert type.isEdge();
            client.graph().addEdges((List<Edge>) elements, check);
        }
    }

    private int waitThenRetry(int retryCount, RuntimeException e) {
        LoadOptions options = this.context().options();
        try {
            Thread.sleep(options.retryInterval * 1000);
        } catch (InterruptedException ignored) {
            // That's fine, just continue.
        }

        if (++retryCount > options.retryTimes) {
            LOG.error("Batch insert has retried exceed {} times",
                      options.retryTimes);
            throw e;
        }
        return retryCount;
    }
}
