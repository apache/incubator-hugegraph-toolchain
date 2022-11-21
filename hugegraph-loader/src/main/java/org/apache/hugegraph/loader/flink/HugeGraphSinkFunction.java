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

package org.apache.hugegraph.loader.flink;

import javax.annotation.Nonnull;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.Preconditions;

public class HugeGraphSinkFunction<T> extends RichSinkFunction<T>
                                      implements CheckpointedFunction {

    private static final long serialVersionUID = -2259171589402599426L;
    private final HugeGraphOutputFormat<Object> outputFormat;

    public HugeGraphSinkFunction(@Nonnull HugeGraphOutputFormat<Object> outputFormat) {
        this.outputFormat = Preconditions.checkNotNull(outputFormat);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        RuntimeContext ctx = getRuntimeContext();
        this.outputFormat.setRuntimeContext(ctx);
        this.outputFormat.open(ctx.getIndexOfThisSubtask(), ctx.getNumberOfParallelSubtasks());
    }

    @Override
    public void invoke(T value, Context context) {
        this.outputFormat.writeRecord(value);
    }

    @Override
    public void initializeState(FunctionInitializationContext context) {
        // pass
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) {
        // pass
    }

    @Override
    public void close() throws Exception {
        this.outputFormat.close();
        super.close();
    }
}
