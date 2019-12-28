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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.failure.FailureLogger;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import com.baidu.hugegraph.loader.metrics.LoadMetrics;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public final class ParseTaskBuilder {

    private static final Logger LOG = Log.logger(ParseTaskBuilder.class);

    private final LoadContext context;
    private final InputStruct struct;
    private final Multimap<ElemType, ElementBuilder> builders;

    public ParseTaskBuilder(LoadContext context, InputStruct struct) {
        this.context = context;
        this.struct = struct;
        this.builders = ArrayListMultimap.create();
        for (VertexMapping mapping : struct.vertices()) {
            this.builders.put(ElemType.VERTEX,
                              new VertexBuilder(context, struct, mapping));
        }
        for (EdgeMapping mapping : struct.edges()) {
            this.builders.put(ElemType.EDGE,
                              new EdgeBuilder(context, struct, mapping));
        }
    }

    public InputStruct struct() {
        return this.struct;
    }

    public List<ParseTask> build(List<Line> lines) {
        final int batchSize = this.context.options().batchSize;
        List<ParseTask> tasks = new ArrayList<>();
        for (ElementBuilder builder : this.builders.values()) {
            ParseTask task = new ParseTask(builder.mapping(), () -> {
                List<Record> records = new ArrayList<>(batchSize);
                ElementMapping mapping = builder.mapping();
                LoadMetrics metrics = this.context.summary().metrics(mapping);
                for (Line line : lines) {
                    String rawLine = line.rawLine();
                    Map<String, Object> keyValues = line.keyValues();
                    try {
                        // NOTE: don't remove entry in keyValues
                        GraphElement element = builder.build(keyValues);
                        metrics.increaseParseSuccess();
                        records.add(new Record(rawLine, element));
                    } catch (IllegalArgumentException e) {
                        metrics.increaseParseFailure();
                        ParseException pe = new ParseException(rawLine, e);
                        this.handleParseFailure(mapping, pe);
                    }
                }
                return records;
            });
            tasks.add(task);
        }
        return tasks;
    }

    private void handleParseFailure(ElementMapping mapping, ParseException e) {
        LoadOptions options = this.context.options();
        if (options.testMode) {
            throw e;
        }

        LOG.error("Parse {} error", mapping.type(), e);
        // Write to current mapping's parse failure log
        FailureLogger logger = this.context.failureLogger(this.struct);
        logger.write(e);

        long failures = this.context.summary().totalParseFailures();
        if (failures >= options.maxParseErrors) {
            Printer.printError("More than %s %s parsing error, stop parsing " +
                               "and waiting all insert tasks finished",
                               options.maxParseErrors, mapping.type().string());
            this.context.stopLoading();
        }
    }

    public static class ParseTask implements Supplier<List<Record>> {

        private final ElementMapping mapping;
        private final Supplier<List<Record>> supplier;

        public ParseTask(ElementMapping mapping,
                         Supplier<List<Record>> supplier) {
            this.mapping = mapping;
            this.supplier = supplier;
        }

        public ElementMapping mapping() {
            return this.mapping;
        }

        @Override
        public List<Record> get() {
            return this.supplier.get();
        }
    }
}
