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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.direct.util.SinkToHBase;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.LoadDistributeMetrics;
import org.apache.hugegraph.loader.util.HugeClientHolder;
import org.apache.hugegraph.serializer.direct.HBaseSerializer;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;

import scala.Tuple2;

public class HBaseDirectLoader extends DirectLoader<ImmutableBytesWritable, KeyValue> {

    private SinkToHBase sinkToHBase;
    private LoadDistributeMetrics loadDistributeMetrics;

    public static final Logger LOG = Log.logger(HBaseDirectLoader.class);

    public HBaseDirectLoader(LoadOptions loadOptions,
                             InputStruct struct,
                             LoadDistributeMetrics loadDistributeMetrics) {
        super(loadOptions, struct);
        this.loadDistributeMetrics = loadDistributeMetrics;
        this.sinkToHBase = new SinkToHBase(loadOptions);

    }

    public String getTableName() {

        String tableName = null;
        if (struct.edges().size() > 0) {
            tableName = this.loadOptions.edgeTablename;

        } else if (struct.vertices().size() > 0) {
            tableName = this.loadOptions.vertexTablename;

        }
        return tableName;
    }

    public Integer getTablePartitions() {
        return struct.edges().size() > 0 ?
               loadOptions.edgePartitions : loadOptions.vertexPartitions;
    }

    @Override
    public JavaPairRDD<ImmutableBytesWritable, KeyValue> buildVertexAndEdge(Dataset<Row> ds) {
        LOG.info("Start build vertexes and edges");
        JavaPairRDD<ImmutableBytesWritable, KeyValue> tuple2KeyValueJavaPairRDD;
        tuple2KeyValueJavaPairRDD = ds.toJavaRDD().mapPartitionsToPair(
                (PairFlatMapFunction<Iterator<Row>, ImmutableBytesWritable, KeyValue>) rowIter -> {
                    HBaseSerializer ser;
                    ser = new HBaseSerializer(HugeClientHolder.create(loadOptions),
                                              loadOptions.vertexPartitions,
                                              loadOptions.edgePartitions);
                    List<ElementBuilder> buildersForGraphElement = getElementBuilders();
                    List<Tuple2<ImmutableBytesWritable, KeyValue>> result = new LinkedList<>();
                    while (rowIter.hasNext()) {
                        Row row = rowIter.next();
                        List<Tuple2<ImmutableBytesWritable, KeyValue>> serList;
                        serList = buildAndSer(ser, row, buildersForGraphElement);
                        result.addAll(serList);
                    }
                    ser.close();
                    return result.iterator();
                }
        );
        return tuple2KeyValueJavaPairRDD;
    }

    @Override
    String generateFiles(JavaPairRDD<ImmutableBytesWritable, KeyValue> buildAndSerRdd) {
        LOG.info("Start to generate hfile");
        try {
            Tuple2<SinkToHBase.IntPartitioner, TableDescriptor> tuple =
                    sinkToHBase.getPartitionerByTableName(getTablePartitions(), getTableName());
            Partitioner partitioner = tuple._1;
            TableDescriptor tableDescriptor = tuple._2;

            JavaPairRDD<ImmutableBytesWritable, KeyValue> repartitionedRdd =
                    buildAndSerRdd.repartitionAndSortWithinPartitions(partitioner);
            Configuration conf = sinkToHBase.getHBaseConfiguration().get();
            Job job = Job.getInstance(conf);
            HFileOutputFormat2.configureIncrementalLoadMap(job, tableDescriptor);
            conf.set("hbase.mapreduce.hfileoutputformat.table.name",
                     tableDescriptor.getTableName().getNameAsString());
            String path = getHFilePath(job.getConfiguration());
            repartitionedRdd.saveAsNewAPIHadoopFile(path, ImmutableBytesWritable.class,
                                                    KeyValue.class, HFileOutputFormat2.class,
                                                    conf);
            LOG.info("Saved HFiles to: '{}'", path);
            flushPermission(conf, path);
            return path;
        } catch (IOException e) {
            LOG.error("Failed to generate files", e);
        }
        return Constants.EMPTY_STR;
    }

    public String getHFilePath(Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        long timeStr = System.currentTimeMillis();
        String pathStr = fs.getWorkingDirectory().toString() + "/hfile-gen" + "/" + timeStr + "/";
        Path hfileGenPath = new Path(pathStr);
        if (fs.exists(hfileGenPath)) {
            LOG.info("\n Delete the path where the hfile is generated,path {} ", pathStr);
            fs.delete(hfileGenPath, true);
        }
        return pathStr;
    }

    @Override
    public void loadFiles(String path) {
        try {
            // BulkLoad HFile to HBase
            sinkToHBase.loadHfiles(path, getTableName());
        } catch (Exception e) {
            LOG.error(" Failed to load hfiles", e);
        }
    }

    private void flushPermission(Configuration conf, String path) {
        FsShell shell = new FsShell(conf);
        try {
            LOG.info("Chmod hfile directory permission");
            shell.run(new String[]{"-chmod", "-R", "777", path});
            shell.close();
        } catch (Exception e) {
            LOG.error("Couldn't change the file permissions " + e + " Please run command:" +
                      "hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles " + path +
                      " '" + "test" + "'\n" + " to load generated HFiles into HBase table");
        }
    }

    List<Tuple2<ImmutableBytesWritable, KeyValue>> buildAndSer(HBaseSerializer serializer, Row row,
                                                               List<ElementBuilder> builders) {
        List<GraphElement> elementsElement;
        List<Tuple2<ImmutableBytesWritable, KeyValue>> result = new LinkedList<>();

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
                    Tuple2<ImmutableBytesWritable, KeyValue> tuple2 =
                            vertexSerialize(serializer, vertex);
                    loadDistributeMetrics.increaseDisVertexInsertSuccess(builder.mapping());
                    result.add(tuple2);
                }
            } else {
                for (Edge edge : (List<Edge>) (Object) elementsElement) {
                    LOG.debug("edge already build done {}", edge.toString());
                    Tuple2<ImmutableBytesWritable, KeyValue> tuple2 =
                            edgeSerialize(serializer, edge);
                    loadDistributeMetrics.increaseDisEdgeInsertSuccess(builder.mapping());
                    result.add(tuple2);

                }
            }
        }
        return result;
    }

    private Tuple2<ImmutableBytesWritable, KeyValue> edgeSerialize(HBaseSerializer serializer,
                                                                   Edge edge) {
        LOG.debug("edge start serialize {}", edge.toString());
        byte[] rowkey = serializer.getKeyBytes(edge);
        byte[] values = serializer.getValueBytes(edge);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey, Bytes.toBytes(Constants.HBASE_COL_FAMILY),
                                         Bytes.toBytes(Constants.EMPTY_STR), values);
        return new Tuple2<>(rowKey, keyValue);
    }

    private Tuple2<ImmutableBytesWritable, KeyValue> vertexSerialize(HBaseSerializer serializer,
                                                                     Vertex vertex) {
        LOG.debug("vertex start serialize {}", vertex.toString());
        byte[] rowkey = serializer.getKeyBytes(vertex);
        byte[] values = serializer.getValueBytes(vertex);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey, Bytes.toBytes(Constants.HBASE_COL_FAMILY),
                                         Bytes.toBytes(Constants.EMPTY_STR), values);
        return new Tuple2<>(rowKey, keyValue);
    }
}
