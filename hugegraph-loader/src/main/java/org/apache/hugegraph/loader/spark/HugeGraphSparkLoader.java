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

package org.apache.hugegraph.loader.spark;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.direct.loader.HBaseDirectLoader;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.metrics.LoadDistributeMetrics;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;
import org.apache.hugegraph.loader.util.Printer;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.hugegraph.loader.parser.TextLineParser;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.file.Compression;
import org.apache.hugegraph.loader.source.file.FileFilter;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.file.SkippedLine;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.UpdateStrategy;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.util.LongAccumulator;
import org.slf4j.Logger;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import scala.collection.JavaConverters;

public class HugeGraphSparkLoader implements Serializable {

    public static final Logger LOG = Log.logger(HugeGraphSparkLoader.class);

    private final LoadOptions loadOptions;
    private final transient ExecutorService executor;

    public static void main(String[] args) {
        HugeGraphSparkLoader loader;
        try {
            loader = new HugeGraphSparkLoader(args);
            loader.load();
        } catch (Throwable e) {
            Printer.printError("Failed to start loading", e);
            System.exit(1);
        }
    }

    public HugeGraphSparkLoader(String[] args) {
        checkDeployment(new SparkConf());
        this.loadOptions = LoadOptions.parseOptions(args);
        this.executor = Executors.newCachedThreadPool();
    }

    private void registerKryoClasses(SparkConf conf) {
        try {
            conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrationRequired", "true")
                .registerKryoClasses(new Class[]{
                    ImmutableBytesWritable.class,
                    KeyValue.class,
                    org.apache.spark.sql.types.StructType.class,
                    StructField[].class,
                    StructField.class,
                    org.apache.spark.sql.types.LongType$.class,
                    org.apache.spark.sql.types.Metadata.class,
                    org.apache.spark.sql.types.StringType$.class,
                    org.apache.spark.sql.catalyst.InternalRow.class,
                    org.apache.spark.sql.catalyst.InternalRow[].class,
                    Class.forName("org.apache.spark.internal.io." +
                                      "FileCommitProtocol$TaskCommitMessage"),
                    Class.forName("scala.collection.immutable.Set$EmptySet$"),
                    Class.forName("org.apache.spark.sql.types.DoubleType$")
                });
        } catch (ClassNotFoundException e) {
            LOG.error("spark kryo serialized registration failed");
            throw new LoadException("spark kryo serialized registration failed", e);
        }
    }

    static void checkDeployment(SparkConf conf) {
        if ("cluster".equals(conf.get("spark.submit.deployMode", "client")) &&
            conf.get("spark.master", "").startsWith("spark://")) {
            throw new LoadException("Standalone cluster deploy mode is not supported; " +
                                    "use client deploy mode");
        }
    }

    public void load() throws ExecutionException, InterruptedException {
        checkLoadOptions(this.loadOptions);
        LoadMapping mapping = LoadMapping.of(this.loadOptions.file);
        List<InputStruct> structs = mapping.structs();
        checkHeaders(structs);
        SparkConf conf = new SparkConf();
        boolean sinkType = this.loadOptions.sinkType;
        //if (!sinkType) {
        //    this.loadOptions.copyBackendStoreInfo(mapping.getBackendStoreInfo());
        //}

        registerKryoClasses(conf);
        SparkSession session = SparkSession.builder().config(conf).getOrCreate();
        SparkContext sc = session.sparkContext();
        LOG.info("Spark Loader runtime: Java {}, default charset {}",
                 System.getProperty("java.version"), Charset.defaultCharset().name());

        try {
            LongAccumulator totalInsertSuccess = sc.longAccumulator("totalInsertSuccess");
            List<Future<?>> futures = new ArrayList<>(structs.size());

            for (InputStruct struct : structs) {
                Future<?> future = this.executor.submit(() -> {
                    LOG.info("\n Initializes the accumulator corresponding to the  {} ",
                             struct.input().asFileSource().path());
                    LoadDistributeMetrics loadDistributeMetrics = new LoadDistributeMetrics(struct);
                    loadDistributeMetrics.init(sc);
                    LOG.info("\n  Start to load data, data info is: \t {} ",
                             struct.input().asFileSource().path());
                    Dataset<Row> ds = read(session, struct);
                    if (sinkType) {
                        LOG.info("\n  Start to load data using spark apis  \n");
                        ds.foreachPartition(new PartitionWriter(this.loadOptions, struct));

                    } else {
                        LOG.info("\n Start to load data using spark bulkload \n");
                        // gen-hfile
                        HBaseDirectLoader directLoader = new HBaseDirectLoader(loadOptions, struct,
                                                                               loadDistributeMetrics);
                        directLoader.bulkload(ds);

                    }
                    collectLoadMetrics(loadDistributeMetrics, totalInsertSuccess);
                    LOG.info("\n Finished  load {}  data ", struct.input().asFileSource().path());
                });
                futures.add(future);
            }
            for (Future<?> future : futures) {
                future.get();
            }

            Long totalInsertSuccessCnt = totalInsertSuccess.value();
            LOG.info("\n ------------The data load task is complete-------------------\n" +
                     "\n insertSuccessCnt:\t {} \n ---------------------------------------------\n",
                     totalInsertSuccessCnt);
        } finally {
            this.executor.shutdownNow();
            session.stop();
        }
    }

    static void checkLoadOptions(LoadOptions options) {
        if (options.incrementalMode || options.failureMode) {
            throw new LoadException("Spark does not support --incremental-mode or --failure-mode; " +
                                    "see docs/spark-java17.md");
        }
    }

    static void checkHeaders(List<InputStruct> structs) {
        for (InputStruct struct : structs) {
            switch (struct.input().type()) {
                case FILE:
                case HDFS:
                    FileSource source = struct.input().asFileSource();
                    if ((source.format() == FileFormat.TEXT ||
                         source.format() == FileFormat.CSV ||
                         source.format() == FileFormat.JSON) &&
                        !StandardCharsets.UTF_8.equals(Charset.forName(source.charset()))) {
                        throw new LoadException("Spark %s input '%s' requires UTF-8 charset; " +
                                                "see docs/spark-java17.md",
                                                source.format(), source.path());
                    }
                    if (source.format() != FileFormat.CSV &&
                        source.format() != FileFormat.TEXT) {
                        break;
                    }
                    if (source.header() == null || Boolean.TRUE.equals(source.hasHeader())) {
                        throw new LoadException("Spark %s input '%s' requires an explicit mapping " +
                                                "header and headerless data (has_header must not " +
                                                "be true); see docs/spark-java17.md",
                                                source.format(), source.path());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void collectLoadMetrics(LoadDistributeMetrics loadMetrics,
                                    LongAccumulator totalInsertSuccess) {
        Long edgeInsertSuccess = loadMetrics.readEdgeInsertSuccess();
        Long vertexInsertSuccess = loadMetrics.readVertexInsertSuccess();
        totalInsertSuccess.add(edgeInsertSuccess);
        totalInsertSuccess.add(vertexInsertSuccess);
    }

    private static void initPartition(LoadContext context, InputStruct struct,
                                      Map<ElementBuilder, List<GraphElement>> builders) {
        context.updateSchemaCache();
        for (VertexMapping vertexMapping : struct.vertices()) {
            builders.put(new VertexBuilder(context, struct, vertexMapping),
                              new ArrayList<>());
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            builders.put(new EdgeBuilder(context, struct, edgeMapping),
                              new ArrayList<>());
        }
    }

    static void loadRow(InputStruct struct, Row row, LoadContext context,
                                Map<ElementBuilder, List<GraphElement>> builders,
                                boolean checkVertex) {
        for (Map.Entry<ElementBuilder, List<GraphElement>> builderMap :
                builders.entrySet()) {
            ElementMapping elementMapping = builderMap.getKey().mapping();
            // Parse
            if (elementMapping.skip()) {
                continue;
            }
            parse(row, builderMap, struct);

            // Insert
            List<GraphElement> graphElements = builderMap.getValue();
            if (context.options().dryRun) {
                graphElements.clear();
            } else {
                flush(builderMap, context.client().graph(), checkVertex);
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
                        throw new IllegalStateException("Unexpected format value: " + format);
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
                throw new AssertionError(String.format("Unsupported input source '%s'",
                                                       input.type()));
        }
        return ds;
    }

    private static void parse(Row row, Map.Entry<ElementBuilder, List<GraphElement>> builderMap,
                       InputStruct struct) {
        ElementBuilder builder = builderMap.getKey();
        List<GraphElement> graphElements = builderMap.getValue();
        List<GraphElement> elements;
        switch (struct.input().type()) {
            case FILE:
            case HDFS:
                FileSource fileSource = struct.input().asFileSource();
                if (fileSource.format() == FileFormat.TEXT) {
                    if (row.isNullAt(0) || "".equals(row.getString(0))) {
                        return;
                    }
                    Line line = new TextLineParser(fileSource.delimiter())
                                .parse(fileSource.header(), row.getString(0));
                    elements = builder.build(line.names(), line.values());
                } else {
                    String[] names = fileSource.format() == FileFormat.CSV ?
                                     fileSource.header() : row.schema().fieldNames();
                    if (names.length != row.size()) {
                        throw new LoadException("Input column count %s does not match header count %s",
                                                row.size(), names.length);
                    }
                    Object[] values = new Object[row.size()];
                    for (int i = 0; i < row.size(); i++) {
                        values[i] = row.get(i);
                    }
                    elements = builder.build(names, values);
                }
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
                throw new AssertionError(String.format("Unsupported input source '%s'",
                                                       struct.input().type()));
        }
        graphElements.addAll(elements);
    }

    private static void flush(Map.Entry<ElementBuilder, List<GraphElement>> builderMap,
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
            // CreateIfNotExist doesn't support false now
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

    static final class PartitionWriter implements ForeachPartitionFunction<Row> {

        final String optionsJson;
        final String structJson;

        PartitionWriter(LoadOptions options, InputStruct struct) {
            // Spark uses Java serialization for closures, independently of its data serializer.
            this.optionsJson = JsonUtil.toJson(options);
            this.structJson = JsonUtil.toJson(struct);
        }

        @Override
        public void call(Iterator<Row> rows) {
            LoadOptions options = JsonUtil.fromJson(this.optionsJson, LoadOptions.class);
            InputStruct struct = JsonUtil.fromJson(this.structJson, InputStruct.class);
            Map<ElementBuilder, List<GraphElement>> builders = new HashMap<>();
            LoadContext context = new LoadContext(options);
            try {
                initPartition(context, struct, builders);
                rows.forEachRemaining(row -> loadRow(struct, row, context, builders,
                                                     options.checkVertex));
            } finally {
                context.close();
            }
        }
    }

}
