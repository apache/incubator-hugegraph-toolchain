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

package org.apache.hugegraph.loader.direct.loader;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsShell;
import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.hugegraph.loader.metrics.DistributedLoadMetrics;
import org.apache.hugegraph.serializer.GraphElementSerializer;
import org.apache.hugegraph.serializer.direct.struct.Directions;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import scala.Tuple2;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractDirectLoader<T, R> implements DirectLoader<T, R>, Serializable {

    public static final Logger LOG = Log.logger(AbstractDirectLoader.class);
    protected LoadOptions loadOptions;
    protected InputStruct struct;
    protected DistributedLoadMetrics loadDistributeMetrics;

    public AbstractDirectLoader(LoadOptions loadOptions, InputStruct struct, DistributedLoadMetrics loadDistributeMetrics) {
        this.loadOptions = loadOptions;
        this.struct = struct;
        this.loadDistributeMetrics = loadDistributeMetrics;
    }

    public AbstractDirectLoader(LoadOptions loadOptions, InputStruct struct) {
        this.loadOptions = loadOptions;
        this.struct = struct;
    }

    public void flushPermission(Configuration conf, String path) {
        FsShell shell = new FsShell(conf);
        try {
            LOG.info("shell start execute");
            shell.run(new String[]{"-chmod", "-R", "777", path});
            shell.close();
        } catch (Exception e) {
            LOG.error("Couldnt change the file permissions " + e
                    + " Please run command:"
                    + "hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles "
                    + path + " '"
                    + "test" + "'\n"
                    + " to load generated HFiles into HBase table");
        }
    }

    @Override
    public void bulkload(Dataset<Row> ds) {
        JavaPairRDD<T, R> javaPairRDD = buildVertexAndEdge(ds, null);
        String path = generateFiles(javaPairRDD);
        loadFiles(path,null);
    }

    protected List<ElementBuilder> getElementBuilders(LoadContext context) {
        context.schemaCache().updateAll();
        List<ElementBuilder> buildersForGraphElement = new LinkedList<>();
        for (VertexMapping vertexMapping : struct.vertices()) {
            buildersForGraphElement.add(new VertexBuilder(context, struct, vertexMapping));
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            buildersForGraphElement.add(new EdgeBuilder(context, struct, edgeMapping));
        }
        return buildersForGraphElement;
    }

    protected List<Tuple2<T, R>> buildAndSer(GraphElementSerializer serializer,
                                             Row row,
                                             List<ElementBuilder> builders, Directions directions) {
        List<GraphElement> elementsElement;
        List<Tuple2<T, R>> result = new LinkedList<>();

        for (ElementBuilder builder : builders) {
            ElementMapping elementMapping = builder.mapping();
            if (elementMapping.skip()) {
                continue;
            }
            if ("".equals(row.mkString())) {
                break;
            }
            switch (struct.input().type()) {
                case FILE:
                case HDFS:
                     elementsElement = builder.build(row);
                    break;
                default:
                    throw new AssertionError(String.format("Unsupported input source '%s'", struct.input().type()));
            }

            boolean isVertex = builder.mapping().type().isVertex();
            if (isVertex) {
                for (Vertex vertex : (List<Vertex>) (Object) elementsElement) {
                    Tuple2<T, R> tuple2 = vertexSerialize(serializer, vertex);
                    loadDistributeMetrics.vertexMetrics().get(builder.mapping().label()).plusDisParseSuccess(1L);
                    loadDistributeMetrics.vertexMetrics().get(builder.mapping().label()).plusDisInsertSuccess(1L);
                    result.add(tuple2);
                }
            } else {
                for (Edge edge : (List<Edge>) (Object) elementsElement) {
                    Tuple2<T, R> tuple2 = edgeSerialize(serializer, edge,directions);
                    loadDistributeMetrics.edgeMetrics().get(builder.mapping().label()).plusDisParseSuccess(1L);
                    loadDistributeMetrics.edgeMetrics().get(builder.mapping().label()).plusDisInsertSuccess(1L);
                    result.add(tuple2);
                }
            }
        }
        return result;
    }

    protected abstract Tuple2<T, R> vertexSerialize(GraphElementSerializer serializer, Vertex vertex);

    protected abstract Tuple2<T, R> edgeSerialize(GraphElementSerializer serializer, Edge edge, Directions direction);
}
