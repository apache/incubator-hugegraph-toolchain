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

import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_PRINT_FREQ;

import java.util.List;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;

public class SingleInsertTask<GE extends GraphElement> extends InsertTask<GE> {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private final FailureLogger failureLogger = FailureLogger.insert();

    public SingleInsertTask(LoadContext context, ElemType type,
                            List<GE> batch) {
        super(context, type, batch);
    }

    @Override
    public void run() {
        ElemType type = this.type();
        LoadOptions options = this.context().options();
        LoadMetrics metrics = this.context().summary().metrics(type);
        for (GE element : this.batch()) {
            try {
                addSingle(type, element);
                metrics.increaseInsertSuccess();
            } catch (Exception e) {
                metrics.increaseInsertFailure();
                LOG.error("Single insert {} error", type, e);
                if (options.testMode) {
                    throw e;
                }
                this.failureLogger.error(type, new InsertException(element, e));

                if (metrics.insertFailure() >= options.maxInsertErrors) {
                    Printer.printError("More than %s %s insert error... " +
                                       "stopping",
                                       options.maxInsertErrors, type);
                    LoadUtil.exit(Constants.EXIT_CODE_ERROR);
                }
            }
        }
        Printer.printProgress(metrics, SINGLE_PRINT_FREQ, this.batch().size());
    }

    private void addSingle(ElemType type, GE element) {
        HugeClient client = HugeClientWrapper.get(this.context().options());
        if (type.isVertex()) {
            client.graph().addVertex((Vertex) element);
        } else {
            assert type.isEdge();
            client.graph().addEdge((Edge) element);
        }
    }
}
