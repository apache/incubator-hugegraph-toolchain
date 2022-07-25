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

package com.baidu.hugegraph.loader.spark;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.source.file.FileFilter;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.file.SkippedLine;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.BatchEdgeRequest;
import com.baidu.hugegraph.structure.graph.BatchVertexRequest;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.UpdateStrategy;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;

import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import scala.collection.JavaConverters;

public class HugeGraphSparkLoader implements Serializable {

    public static final Logger LOG = Log.logger(HugeGraphSparkLoader.class);

    private final LoadOptions loadOptions;
    private final Map<ElementBuilder, List<GraphElement>> builders;

    public static void main(String[] args) {
        HugeGraphSparkLoader loader;
        try {
            loader = new HugeGraphSparkLoader(args);
        } catch (Throwable e) {
            Printer.printError("Failed to start loading", e);
            return;
        }
        loader.load();
    }

    public HugeGraphSparkLoader(String[] args) {
        this.loadOptions = LoadOptions.parseOptions(args);
        this.builders = new HashMap<>();
    }

    public void load() {
        LoadMapping mapping = LoadMapping.of(this.loadOptions.file);
        List<InputStruct> structs = mapping.structs();

        SparkSession session = SparkSession.builder().getOrCreate();
        for (InputStruct struct : structs) {
            Dataset<Row> ds = read(session, struct);
            ds.foreachPartition((Iterator<Row> p) -> {
                LoadContext context = initPartition(this.loadOptions, struct);
                p.forEachRemaining((Row row) -> {
                    loadRow(struct, row, p, context);
                });
                context.close();
            });
        }
        session.close();
        session.stop();
    }

    private LoadContext initPartition(
            LoadOptions loadOptions, InputStruct struct) {
        LoadContext context = new LoadContext(loadOptions);
        for (VertexMapping vertexMapping : struct.vertices()) {
            this.builders.put(
                    new VertexBuilder(context, struct, vertexMapping),
                    new ArrayList<>());
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            this.builders.put(new EdgeBuilder(context, struct, edgeMapping),
                              new ArrayList<>());
        }
        return context;
    }

    private void loadRow(InputStruct struct, Row row, Iterator<Row> p,
                         LoadContext context) {
        for (Map.Entry<ElementBuilder, List<GraphElement>> builderMap :
                this.builders.entrySet()) {
            ElementMapping elementMapping = builderMap.getKey().mapping();
            // Parse
            if (elementMapping.skip()) {
                continue;
            }
            parse(row, builderMap, struct);

            // Insert
            List<GraphElement> graphElements = builderMap.getValue();
            if (graphElements.size() >= elementMapping.batchSize() ||
                (!p.hasNext() && graphElements.size() > 0)) {
                flush(builderMap, context.client().graph(), this.loadOptions.checkVertex);
            }
        }
    }

    private Dataset<Row> read(SparkSession ss, InputStruct struct) {
        InputSource input = struct.input();
        String charset = input.charset();
        DataFrameReader reader = ss.read();
        Dataset<Row> ds;
        switch (input.type()) {
            case FILE:
            case HDFS:
                FileSource fileSource = input.asFileSource();
                String[] header = fileSource.header();
                String delimiter = fileSource.delimiter();
                String path = fileSource.path();
                FileFilter filter = fileSource.filter();
                FileFormat format = fileSource.format();
                String dateFormat = fileSource.dateFormat();
                String timeZone = fileSource.timeZone();
                SkippedLine skippedLine = fileSource.skippedLine();
                Compression compression = fileSource.compression();
                int batchSize = fileSource.batchSize();
                switch (format) {
                    case TEXT:
                        ds = reader.text(path);
                        break;
                    case JSON:
                        ds = reader.json(path);
                        break;
                    case CSV:
                        ds = reader.csv(path);
                        break;
                    default:
                        throw new IllegalStateException(
                                  "Unexpected format value: " + format);
                }
                break;
            case JDBC:
                JDBCSource jdbcSource = (JDBCSource) struct.input();
                String url = jdbcSource.url() + "/" + jdbcSource.database();
                String table = jdbcSource.table();
                String username = jdbcSource.username();
                String password = jdbcSource.password();
                Properties properties = new Properties();
                properties.put("user", username);
                properties.put("password", password);
                ds = reader.jdbc(url, table, properties);
                break;
            default:
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", input.type()));
        }
        return ds;
    }

    private void parse(Row row,
                       Map.Entry<ElementBuilder, List<GraphElement>> builderMap,
                       InputStruct struct) {
        ElementBuilder builder = builderMap.getKey();
        List<GraphElement> graphElements = builderMap.getValue();
        if ("".equals(row.mkString())) {
            return;
        }
        List<GraphElement> elements;
        switch (struct.input().type()) {
            case FILE:
            case HDFS:
                FileSource fileSource = struct.input().asFileSource();
                elements = builder.build(fileSource.header(),
                                         row.mkString()
                                            .split(fileSource.delimiter()));
                break;
            case JDBC:
                Object[] structFields = JavaConverters.asJavaCollection(row.schema().toList())
                                                      .toArray();
                int len = row.schema().length();
                String[] headers = new String[len];
                Object[] values = new Object[len];
                for (int i = 0; i < len; i++) {
                    headers[i] = ((StructField) structFields[i]).name();
                    values[i] = row.get(i);
                }
                elements = builder.build(headers, values);
                break;
            default:
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'",
                          struct.input().type()));
        }
        graphElements.addAll(elements);
    }

    private void flush(Map.Entry<ElementBuilder, List<GraphElement>> builderMap,
                       GraphManager g, boolean isCheckVertex) {
        ElementBuilder builder = builderMap.getKey();
        ElementMapping elementMapping = builder.mapping();
        List<GraphElement> graphElements = builderMap.getValue();
        boolean isVertex = builder.mapping().type().isVertex();
        Map<String, UpdateStrategy> updateStrategyMap = elementMapping.updateStrategies();
        if (updateStrategyMap.isEmpty()) {
            if (isVertex) {
                g.addVertices((List<Vertex>) (Object) graphElements);
            } else {
                g.addEdges((List<Edge>) (Object) graphElements);
            }
        } else {
            // CreateIfNotExist dose not support false now
            if (isVertex) {
                BatchVertexRequest.Builder req =
                        new BatchVertexRequest.Builder();
                req.vertices((List<Vertex>) (Object) graphElements)
                   .updatingStrategies(updateStrategyMap)
                   .createIfNotExist(true);
                g.updateVertices(req.build());
            } else {
                BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
                req.edges((List<Edge>) (Object) graphElements)
                   .updatingStrategies(updateStrategyMap)
                   .checkVertex(isCheckVertex)
                   .createIfNotExist(true);
                g.updateEdges(req.build());
            }
        }
        graphElements.clear();
    }
}
