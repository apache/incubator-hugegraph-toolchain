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
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;

import com.baidu.hugegraph.loader.direct.loader.HBaseDirectLoader;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.metrics.LoadDistributeMetrics;
import com.baidu.hugegraph.loader.metrics.LoadMetrics;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.loader.source.file.*;

import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.UpdateStrategy;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.BatchEdgeRequest;
import com.baidu.hugegraph.structure.graph.BatchVertexRequest;
import com.baidu.hugegraph.util.Log;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.DataFrameReader;
import org.slf4j.Logger;
import java.io.Serializable;
import java.util.*;


public class HugeGraphSparkLoader implements Serializable {

    public static final Logger LOG = Log.logger(HugeGraphSparkLoader.class);

    private final LoadOptions loadOptions;
    private final Map<ElementBuilder, List<GraphElement>> builders;



    public static void main(String[] args) throws Exception {
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
        LOG.info("host: " + this.loadOptions.host + " port: " + this.loadOptions.port + " graph: " + this.loadOptions.graph);
        LOG.info("sink mode: " + this.loadOptions.sinkType);
    }




    public void load() throws Exception {

        LoadMapping mapping = LoadMapping.of(this.loadOptions.file);
        this.loadOptions.copyBackendStoreInfo(mapping.getBackendStoreInfo());
        List<InputStruct> structs = mapping.structs();
        boolean sinkType = this.loadOptions.sinkType;


        SparkConf conf = new SparkConf()
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")// kryo序列化
                .set("spark.kryo.registrationRequired", "true")
                .setAppName("spark-gen-hfile")

                .setMaster("local[*]");

        conf.registerKryoClasses(
                new Class[]{org.apache.hadoop.hbase.io.ImmutableBytesWritable.class,
                        org.apache.hadoop.hbase.KeyValue.class,
                        org.apache.spark.sql.types.StructType.class,
                        org.apache.spark.sql.types.StructField[].class,
                        org.apache.spark.sql.types.StructField.class,
                        org.apache.spark.sql.types.LongType$.class,
                        org.apache.spark.sql.types.Metadata.class,
                        org.apache.spark.sql.types.StringType$.class,
                        Class.forName("org.apache.spark.internal.io.FileCommitProtocol$TaskCommitMessage"),
                        Class.forName("scala.reflect.ClassTag$$anon$1"),
                        Class.forName("scala.collection.immutable.Set$EmptySet$"),
                        Class.forName("org.apache.spark.sql.types.DoubleType$")
                });
        SparkSession session = SparkSession.builder()
                .config(conf)
//                .enableHiveSupport()
                .getOrCreate();

        SparkContext sc = session.sparkContext();

        Long totalInsertSuccess=0L;
        for (InputStruct struct : structs) {
            // 初始化metric
            LOG.info("\n init"+ struct.input().asFileSource().path()+" distribute metrics---- \n");
            LoadDistributeMetrics loadDistributeMetrics = new LoadDistributeMetrics(struct);
            loadDistributeMetrics.init(sc);
            LOG.info("    \n   load dat info: \t"+struct.input().asFileSource().path() +"\n start load data ; \n" );
            Dataset<Row> ds = read(session, struct);
            if(sinkType){
                LOG.info("\n ------ spark api start load data ------ \n");
                ds.foreachPartition((Iterator<Row> p) -> {
                    LoadContext context = initPartition(this.loadOptions, struct);
                    p.forEachRemaining((Row row) -> {
                        loadRow(struct, row, p, context);
                    });
                    context.close();
                });

            }else {
                LOG.info("\n        spark bulkload gen hfile start     \n");
                // gen-hfile
                HBaseDirectLoader directLoader = new HBaseDirectLoader(loadOptions, struct,loadDistributeMetrics);
                directLoader.bulkload(ds);

            }
            collectLoadMetrics(loadDistributeMetrics,totalInsertSuccess);
            LOG.info("    \n   load data info : \t"+struct.input().asFileSource().path() +"\n load data finish!!!; \n" );
        }

        LOG.info("\n ---------导入数据整体任务结束----------------------\n" +
                "\n  insertSuccess cnt:\t"+totalInsertSuccess+"     \n"+
                "\n ---------------------------------------------\n"
        );


        sc.stop();
        session.stop();
        session.close();

    }


    private void collectLoadMetrics(LoadDistributeMetrics loadMetrics,Long totalInsertSuccess){

        Long edgeInsertSuccess = loadMetrics.readEdgeInsertSuccess();
        Long vertexInsertSuccess = loadMetrics.readVertexInsertSuccess();
        totalInsertSuccess+=edgeInsertSuccess;
        totalInsertSuccess+=vertexInsertSuccess;

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
            if (graphElements.size() > elementMapping.batchSize() ||
                    (!p.hasNext() && graphElements.size() > 0)) {
                flush(builderMap, context.client().graph(),
                        this.loadOptions.checkVertex);
            }
        }
    }

    private Dataset<Row> read(SparkSession ss, InputStruct struct) {
        InputSource input = struct.input();
        String charset = input.charset();
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

        DataFrameReader reader = ss.read();
        Dataset<Row> ds;
        switch (input.type()) {
            case FILE:
            case HDFS:
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
                // TODO: implement jdbc
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
                //TODO: implement jdbc
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
