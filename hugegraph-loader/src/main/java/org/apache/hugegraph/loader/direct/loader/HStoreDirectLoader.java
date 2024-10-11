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

import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.DistributedLoadMetrics;
import org.apache.hugegraph.serializer.GraphElementSerializer;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class HStoreDirectLoader extends DirectLoader<Tuple2<byte[], Integer>,byte[]> {

    private DistributedLoadMetrics loadDistributeMetrics;


    public static final Logger LOG = Log.logger(HStoreDirectLoader.class);


    public HStoreDirectLoader(LoadOptions loadOptions,
                              InputStruct struct,
                              DistributedLoadMetrics loadDistributeMetrics) {
        super(loadOptions, struct);
        this.loadDistributeMetrics = loadDistributeMetrics;
    }


    @Override
    public JavaPairRDD<Tuple2<byte[], Integer>, byte[]> buildVertexAndEdge(Dataset<Row> ds) {

        LOG.info("Start build vertexes and edges");
        JavaPairRDD<Tuple2<byte[], Integer>, byte[]> tuple2JavaPairRDD = ds.toJavaRDD().mapPartitionsToPair(
                (PairFlatMapFunction<Iterator<Row>, Tuple2<byte[], Integer>, byte[]>) rowIter -> {
                    // 完成了schema数据的准备，以及pdclient的每个分区的创建
                    LoadContext loaderContext = new LoadContext(super.loadOptions);
                    loaderContext.init(struct);// 准备好elementBuilder以及schema数据的更新
                    List<ElementBuilder> buildersForGraphElement = getElementBuilders(loaderContext);
                    List<Tuple2<Tuple2<byte[], Integer>, byte[]>> result = new LinkedList<>();
                    while (rowIter.hasNext()) {
                        Row row = rowIter.next();
                        List<Tuple2<Tuple2<byte[], Integer>, byte[]>> serList;
                        serList = buildAndSer(loaderContext.getSerializer(), row, buildersForGraphElement);
                        result.addAll(serList);
                    }
                    return result.iterator();
                }
        );

        return tuple2JavaPairRDD;
    }

    @Override
    protected String generateFiles(JavaPairRDD<Tuple2<byte[], Integer>, byte[]> buildAndSerRdd) {
        return "";
    }





    @Override
    protected void loadFiles(String path) {

    }


    List<Tuple2<Tuple2<byte[], Integer>, byte[]>> buildAndSer(GraphElementSerializer serializer, Row row,
                                                              List<ElementBuilder> builders) {
        List<GraphElement> elementsElement;
        List<Tuple2<Tuple2<byte[], Integer>, byte[]>> result = new LinkedList<>();

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
                    throw new AssertionError(String.format("Unsupported input source '%s'",
                            struct.input().type()));
            }

            boolean isVertex = builder.mapping().type().isVertex();
            if (isVertex) {
                for (Vertex vertex : (List<Vertex>) (Object) elementsElement) {
                    LOG.debug("vertex already build done {} ", vertex.toString());
                    Tuple2<Tuple2<byte[], Integer>, byte[]> tuple2 =vertexSerialize(serializer, vertex);
                    //mapping.label();
                    String label = builder.mapping().label();
                    loadDistributeMetrics.vertexMetrics().get(builder.mapping().label()).plusDisParseSuccess(1L);
                    loadDistributeMetrics.vertexMetrics().get(builder.mapping().label()).plusDisInsertSuccess(1L);

                    result.add(tuple2);
                }
            } else {
                for (Edge edge : (List<Edge>) (Object) elementsElement) {
                    LOG.debug("edge already build done {}", edge.toString());
                    Tuple2<Tuple2<byte[], Integer>, byte[]> tuple2 =edgeSerialize(serializer, edge);
                    loadDistributeMetrics.edgeMetrics().get(builder.mapping().label()).plusDisParseSuccess(1L);
                    loadDistributeMetrics.edgeMetrics().get(builder.mapping().label()).plusDisInsertSuccess(1L);
                    result.add(tuple2);

                }
            }
        }
        return result;
    }

    private Tuple2<Tuple2<byte[], Integer>, byte[]> edgeSerialize(GraphElementSerializer serializer, Edge edge) {
        LOG.debug("edge start serialize {}", edge.toString());
        Tuple2<byte[], Integer> keyBytes = serializer.getKeyBytes(edge);
        byte[] values = serializer.getValueBytes(edge);

        return new Tuple2<>(keyBytes, values);
    }

    private Tuple2<Tuple2<byte[], Integer>, byte[]> vertexSerialize(GraphElementSerializer serializer,
                                                                     Vertex vertex) {
        LOG.debug("vertex start serialize {}", vertex.toString());
        Tuple2<byte[], Integer> keyBytes = serializer.getKeyBytes(vertex);
        byte[] values = serializer.getValueBytes(vertex);
        return new Tuple2<>(keyBytes, values);
    }


    static class TupleComparator implements Comparator<Tuple2<byte[], byte[]>>, Serializable {
        @Override
        public int compare(Tuple2<byte[], byte[]> a, Tuple2<byte[], byte[]> b) {
            return compareByteArrays(a._1, b._1);
        }

        private int compareByteArrays(byte[] a, byte[] b) {
            for (int i = 0, j = 0; i < a.length && j < b.length; i++, j++) {
                int cmp = Byte.compare(a[i], b[j]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(a.length, b.length);
        }
    }
}
