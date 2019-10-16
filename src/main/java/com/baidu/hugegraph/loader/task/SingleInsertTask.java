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
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.failure.FailureLogger;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.ImmutableList;

public class SingleInsertTask<GE extends GraphElement> extends InsertTask<GE> {

    private static final Logger LOG = Log.logger(TaskManager.class);

    public SingleInsertTask(LoadContext context, ElementStruct struct,
                            List<Record<GE>> batch) {
        super(context, struct, batch);
    }

    @Override
    public void run() {
        FailureLogger logger = this.context().failureLogger(this.struct());
        for (Record<GE> record : this.batch()) {
            try {
                if (this.struct().updateStrategies().isEmpty()) {
                    this.addSingle(this.type(), record);
                } else {
                    this.updateSingle(this.type(), this.options(), record);
                }
                this.metrics().increaseLoadSuccess();
            } catch (Exception e) {
                this.metrics().increaseLoadFailure();
                LOG.error("Single insert {} error", this.type(), e);
                if (this.options().testMode) {
                    throw e;
                }
                // Write to current struct's insert failure log
                logger.write(new InsertException(record.rawLine(), e));

                long failures = this.context().summary().totalInsertFailures();
                if (failures >= this.options().maxInsertErrors) {
                    Printer.printError("More than %s %s insert error, stop " +
                                       "parsing and waiting other insert " +
                                       "tasks finished",
                                       this.options().maxInsertErrors,
                                       this.type().string());
                    this.context().stopLoading();
                }
            }
        }
        Printer.printProgress(this.type(), this.metrics().loadSuccess(),
                              SINGLE_PRINT_FREQ, this.batch().size());
    }

    private void addSingle(ElemType type, Record<GE> record) {
        HugeClient client = HugeClientHolder.get(this.context().options());
        if (type.isVertex()) {
            client.graph().addVertex((Vertex) record.element());
        } else {
            assert type.isEdge();
            client.graph().addEdge((Edge) record.element());
        }
    }

    private void updateSingle(ElemType type, LoadOptions options,
                              Record<GE> record) {
        // TODO: Adapt single update later
        this.updateBatch(type, ImmutableList.of(record), options.checkVertex);
    }
}
