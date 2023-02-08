/*
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

package org.apache.hugegraph.loader.task;

import static org.apache.hugegraph.loader.constant.Constants.SINGLE_PRINT_FREQ;

import java.util.List;

import org.apache.hugegraph.loader.util.Printer;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.InsertException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.util.Log;
import com.google.common.collect.ImmutableList;

public class SingleInsertTask extends InsertTask {

    private static final Logger LOG = Log.logger(TaskManager.class);

    public SingleInsertTask(LoadContext context, InputStruct struct,
                            ElementMapping mapping, List<Record> batch) {
        super(context, struct, mapping, batch);
    }

    @Override
    public void execute() {
        for (Record record : this.batch) {
            try {
                if (this.mapping.updateStrategies().isEmpty()) {
                    this.insertSingle(this.options(), record);
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
        Printer.printProgress(this.context, this.type(),
                              SINGLE_PRINT_FREQ, this.batch.size());
    }

    private void handleInsertFailure(InsertException e) {
        LOG.error("Single insert {} error", this.type(), e);
        this.context.occurredError();
        if (this.options().testMode) {
            throw e;
        }
        // Write to current mapping's insert failure log
        this.context.failureLogger(this.struct).write(e);

        long failures = this.context.summary().totalInsertFailures();
        if (this.options().maxInsertErrors != Constants.NO_LIMIT &&
            failures >= this.options().maxInsertErrors) {
            if (this.context.stopped()) {
                return;
            }
            synchronized (this.context) {
                if (!this.context.stopped()) {
                    Printer.printError("More than %s %s insert error, " +
                                       "stop parsing and waiting other " +
                                       "insert tasks stopped",
                                       this.options().maxInsertErrors,
                                       this.type().string());
                    this.context.stopLoading();
                }
            }
        }
    }

    private void insertSingle(LoadOptions options, Record record) {
        this.insertBatch(ImmutableList.of(record), options.checkVertex);
    }

    private void updateSingle(LoadOptions options, Record record) {
        this.updateBatch(ImmutableList.of(record), options.checkVertex);
    }
}
