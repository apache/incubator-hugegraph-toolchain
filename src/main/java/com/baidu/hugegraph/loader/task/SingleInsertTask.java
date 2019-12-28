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

import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.failure.FailureLogger;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.ImmutableList;

public class SingleInsertTask extends InsertTask {

    private static final Logger LOG = Log.logger(TaskManager.class);

    public SingleInsertTask(LoadContext context, InputStruct struct,
                            ElementMapping mapping, List<Record> batch) {
        super(context, struct, mapping, batch);
    }

    @Override
    public void run() {
        ElemType type = this.type();
        FailureLogger logger = this.context.failureLogger(this.struct);
        for (Record record : this.batch) {
            try {
                if (this.mapping.updateStrategies().isEmpty()) {
                    this.addSingle(type, this.options(), record);
                } else {
                    this.updateSingle(type, this.options(), record);
                }
                this.increaseLoadSuccess();
            } catch (Exception e) {
                this.metrics().increaseLoadFailure();
                LOG.error("Single insert {} error", type, e);
                if (this.options().testMode) {
                    throw e;
                }
                // Write to current mapping's insert failure log
                logger.write(new InsertException(record.rawLine(), e));

                long failures = this.context.summary().totalInsertFailures();
                if (failures >= this.options().maxInsertErrors) {
                    if (!this.context.stopped()) {
                        // Just print once
                        Printer.printError("More than %s %s insert error, " +
                                           "stop parsing and waiting other " +
                                           "insert tasks finished",
                                           this.options().maxInsertErrors,
                                           type.string());
                    }
                    this.context.stopLoading();
                }
            }
        }
        Printer.printProgress(this.summary(), SINGLE_PRINT_FREQ,
                              this.batch.size());
    }

    private void addSingle(ElemType type, LoadOptions options, Record record) {
        this.addBatch(type, ImmutableList.of(record), options.checkVertex);
    }

    private void updateSingle(ElemType type, LoadOptions options,
                              Record record) {
        this.updateBatch(type, ImmutableList.of(record), options.checkVertex);
    }
}
