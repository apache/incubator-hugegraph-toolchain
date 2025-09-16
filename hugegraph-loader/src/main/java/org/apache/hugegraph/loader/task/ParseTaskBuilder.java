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

package org.apache.hugegraph.loader.task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.NopEdgeBuilder;
import org.apache.hugegraph.loader.builder.NopVertexBuilder;
import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.exception.ParseException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.hugegraph.loader.metrics.LoadMetrics;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.structure.GraphElement;

public final class ParseTaskBuilder {

    private static final Logger LOG = Log.logger(ParseTaskBuilder.class);

    private final LoadContext context;
    private final InputStruct struct;
    private final List<ElementBuilder> builders;

    public ParseTaskBuilder(LoadContext context, InputStruct struct) {
        this.context = context;
        this.struct = struct;
        this.builders = new ArrayList<>();

        if (SourceType.GRAPH.equals(struct.input().type())) {
            // 当数据源为 HugeGraph 时，不做任何转换。
            this.builders.add(new NopVertexBuilder(this.context, struct));
            this.builders.add(new NopEdgeBuilder(this.context, struct));
        } else {
            for (VertexMapping mapping : struct.vertices()) {
                this.builders.add(
                        new VertexBuilder(this.context, struct, mapping));
            }
            for (EdgeMapping mapping : struct.edges()) {
                this.builders.add(
                        new EdgeBuilder(this.context, struct, mapping));
            }
        }
    }

    public InputStruct struct() {
        return this.struct;
    }

    public List<ParseTask> build(List<Line> lines) {
        List<ParseTask> tasks = new ArrayList<>(this.builders.size());
        for (ElementBuilder builder : this.builders) {
            if (builder.mapping().skip()) {
                continue;
            }
            // Iterate mappings(vertex/edge label) one by one
            tasks.add(this.buildTask(builder, lines));
        }
        return tasks;
    }

    private ParseTask buildTask(ElementBuilder builder, List<Line> lines) {
        final LoadMetrics metrics = this.context.summary().metrics(this.struct);
        final int batchSize = this.context.options().batchSize;
        final ElementMapping mapping = builder.mapping();
        return new ParseTask(mapping, () -> {
            List<List<Record>> batches = new ArrayList<>();
            // One batch record
            List<Record> records = new ArrayList<>(batchSize);
            int count = 0;
            for (Line line : lines) {
                try {
                    // NOTE: don't remove entry in keyValues
                    @SuppressWarnings("unchecked")
                    List<GraphElement> elements = builder.build(line.names(),
                                                                line.values());
                    E.checkState(elements.size() <= batchSize,
                                 "The number of columns in a line cannot " +
                                 "exceed the size of a batch, but got %s > %s",
                                 elements.size(), batchSize);
                    // Prevent batch size from exceeding limit
                    if (records.size() + elements.size() > batchSize) {
                        LOG.debug("Create a new batch for {}", mapping);
                        // Add current batch and create a new batch
                        batches.add(records);
                        records = new ArrayList<>(batchSize);
                    }
                    for (GraphElement element : elements) {
                        if (this.context.filterGroup().filter(element)){
                            records.add(new Record(line.rawLine(), element));
                            count++;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    metrics.increaseParseFailure(mapping);
                    ParseException pe = new ParseException(line.rawLine(), e);
                    this.handleParseFailure(mapping, pe);
                }
            }
            if (!records.isEmpty()) {
                batches.add(records);
            }
            metrics.plusParseSuccess(mapping, count);
            return batches;
        });
    }

    private void handleParseFailure(ElementMapping mapping, ParseException e) {
        LOG.error("Parse {} error", mapping.type(), e);
        this.context.occurredError();
        if (this.context.options().testMode) {
            throw e;
        }
        // Write to current mapping's parse failure log
        this.context.failureLogger(this.struct).write(e);
    }

    public static class ParseTask implements Supplier<List<List<Record>>> {

        private final ElementMapping mapping;
        private final Supplier<List<List<Record>>> supplier;

        public ParseTask(ElementMapping mapping,
                         Supplier<List<List<Record>>> supplier) {
            this.mapping = mapping;
            this.supplier = supplier;
        }

        public ElementMapping mapping() {
            return this.mapping;
        }

        @Override
        public List<List<Record>> get() {
            return this.supplier.get();
        }
    }
}
