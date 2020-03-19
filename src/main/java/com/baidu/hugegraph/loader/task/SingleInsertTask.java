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
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.failure.FailLogger;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.ImmutableList;

public class SingleInsertTask extends InsertTask {

    private static final Logger LOG = Log.logger(TaskManager.class);

    public SingleInsertTask(InputStruct struct, ElementMapping mapping,
                            List<Record> batch) {
        super(struct, mapping, batch);
    }

    @Override
    public void run() {
        for (Record record : this.batch) {
            try {
                if (this.mapping.updateStrategies().isEmpty()) {
                    this.addSingle(this.options(), record);
                } else {
                    this.updateSingle(this.options(), record);
                }
                this.increaseLoadSuccess();
            } catch (Exception e) {
                this.metrics().increaseInsertFailure(this.mapping);
                InsertException ie = new InsertException(record.rawLine(), e);
                this.handleInsertFailure(ie);
            }
        }
        Printer.printProgress(this.type(), SINGLE_PRINT_FREQ, this.batch.size());
    }

    private void handleInsertFailure(InsertException e) {
        LOG.error("Single insert {} error", this.type(), e);
        if (this.options().testMode) {
            throw e;
        }
        FailLogger logger = this.context.failureLogger(this.struct);
        // Write to current mapping's insert failure log
        logger.write(e);

        long failures = this.context.summary().totalInsertFailures();
        if (failures < this.options().maxInsertErrors) {
            return;
        }
        if (!this.context.stopped()) {
            synchronized (this.context) {
                if (!this.context.stopped()) {
                    Printer.printError("More than %s %s insert error, " +
                                       "stop parsing and waiting other " +
                                       "insert tasks finished",
                                       this.options().maxInsertErrors,
                                       this.type().string());
                    this.context.stopLoading();
                }
            }
        }
    }

    private void addSingle(LoadOptions options, Record record) {
        this.addBatch(ImmutableList.of(record), options.checkVertex);
    }

    private void updateSingle(LoadOptions options, Record record) {
        this.updateBatch(ImmutableList.of(record), options.checkVertex);
    }
}
