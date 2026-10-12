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

package org.apache.hugegraph.loader.flink;

import javax.annotation.Nonnull;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.apache.flink.util.Preconditions;

public class HugeGraphSink<T> implements Sink<T> {

    private static final long serialVersionUID = -2259171589402599426L;
    private final HugeGraphOutputFormat<Object> outputFormat;

    public HugeGraphSink(@Nonnull HugeGraphOutputFormat<Object> outputFormat) {
        this.outputFormat = Preconditions.checkNotNull(outputFormat);
    }

    @Override
    public SinkWriter<T> createWriter(WriterInitContext context) {
        this.outputFormat.open(new OutputFormat.InitializationContext() {
            @Override
            public int getNumTasks() {
                return context.getTaskInfo().getNumberOfParallelSubtasks();
            }

            @Override
            public int getTaskNumber() {
                return context.getTaskInfo().getIndexOfThisSubtask();
            }

            @Override
            public int getAttemptNumber() {
                return context.getTaskInfo().getAttemptNumber();
            }
        });
        return new SinkWriter<T>() {
            @Override
            public void write(T value, Context context) {
                HugeGraphSink.this.outputFormat.writeRecord(value);
            }

            @Override
            public void flush(boolean endOfInput) {
                HugeGraphSink.this.outputFormat.flushAll();
            }

            @Override
            public void close() {
                HugeGraphSink.this.outputFormat.close();
            }
        };
    }
}
