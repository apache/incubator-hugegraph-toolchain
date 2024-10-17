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
import org.apache.hadoop.fs.FileSystem;
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
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.DistributedLoadMetrics;
import org.apache.hugegraph.loader.util.HugeClientHolder;
import org.apache.hugegraph.serializer.GraphElementSerializer;
import org.apache.hugegraph.serializer.direct.HBaseSerializer;
import org.apache.hugegraph.serializer.direct.struct.Directions;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HBaseDirectLoader extends AbstractDirectLoader<ImmutableBytesWritable, KeyValue> {
    private SinkToHBase sinkToHBase;
    private static final int RANDOM_VALUE1;
    private static final short RANDOM_VALUE2;
    private static final AtomicInteger NEXT_COUNTER;

    public static final Logger LOG = Log.logger(HBaseDirectLoader.class);

    static {
        try {
            SecureRandom secureRandom = new SecureRandom();
            RANDOM_VALUE1 = secureRandom.nextInt(0x01000000);
            RANDOM_VALUE2 = (short) secureRandom.nextInt(0x00008000);
            NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte int3(final int x) {
        return (byte) (x >> 24);
    }

    private static byte int2(final int x) {
        return (byte) (x >> 16);
    }

    private static byte int1(final int x) {
        return (byte) (x >> 8);
    }

    private static byte int0(final int x) {
        return (byte) (x);
    }

    private static byte short1(final short x) {
        return (byte) (x >> 8);
    }

    private static byte short0(final short x) {
        return (byte) (x);
    }

    public static String fileID() {
        long timeStamp = System.currentTimeMillis() / 1000;
        ByteBuffer byteBuffer = ByteBuffer.allocate(12);

        byteBuffer.put(int3((int) timeStamp));
        byteBuffer.put(int2((int) timeStamp));
        byteBuffer.put(int1((int) timeStamp));
        byteBuffer.put(int0((int) timeStamp));

        byteBuffer.put(int2(RANDOM_VALUE1));
        byteBuffer.put(int1(RANDOM_VALUE1));
        byteBuffer.put(int0(RANDOM_VALUE1));
        byteBuffer.put(short1(RANDOM_VALUE2));
        byteBuffer.put(short0(RANDOM_VALUE2));

        byteBuffer.put(int2(NEXT_COUNTER.incrementAndGet()));
        byteBuffer.put(int1(NEXT_COUNTER.incrementAndGet()));
        byteBuffer.put(int0(NEXT_COUNTER.incrementAndGet()));

        return Bytes.toHex(byteBuffer.array());
    }

    @Override
    public JavaPairRDD<ImmutableBytesWritable, KeyValue> buildVertexAndEdge(Dataset<Row> ds, Directions directions) {
        LOG.info("Start build vertexes and edges");
        return ds.toJavaRDD().mapPartitionsToPair(
                (PairFlatMapFunction<Iterator<Row>, ImmutableBytesWritable, KeyValue>) rowIter -> {
                    HBaseSerializer ser = new HBaseSerializer(HugeClientHolder.create(loadOptions), loadOptions.vertexPartitions, loadOptions.edgePartitions);
                    LoadContext loaderContext = new LoadContext(super.loadOptions);
                    loaderContext.init(struct);// 准备好elementBuilder以及schema数据的更新
                    List<ElementBuilder> buildersForGraphElement = getElementBuilders(loaderContext);
                    List<Tuple2<ImmutableBytesWritable, KeyValue>> result = new LinkedList<>();
                    while (rowIter.hasNext()) {
                        Row row = rowIter.next();
                        List<Tuple2<ImmutableBytesWritable, KeyValue>> serList;
                        serList = buildAndSer(ser, row, buildersForGraphElement,directions);
                        result.addAll(serList);
                    }
                    ser.close();
                    return result.iterator();
                }
        );
    }

    @Override
    public String generateFiles(JavaPairRDD<ImmutableBytesWritable, KeyValue> buildAndSerRdd) {
        LOG.info("Start to generate hfile");
        try {
            Tuple2<SinkToHBase.IntPartitioner, TableDescriptor> tuple = sinkToHBase.getPartitionerByTableName(getTablePartitions(), getTableName());
            Partitioner partitioner = tuple._1;
            TableDescriptor tableDescriptor = tuple._2;

            JavaPairRDD<ImmutableBytesWritable, KeyValue> repartitionedRdd = buildAndSerRdd.repartitionAndSortWithinPartitions(partitioner);
            Configuration conf = sinkToHBase.getHBaseConfiguration().get();
            Job job = Job.getInstance(conf);
            HFileOutputFormat2.configureIncrementalLoadMap(job, tableDescriptor);
            conf.set("hbase.mapreduce.hfileoutputformat.table.name", tableDescriptor.getTableName().getNameAsString());
            String path = getHFilePath(job.getConfiguration());
            repartitionedRdd.saveAsNewAPIHadoopFile(path, ImmutableBytesWritable.class, KeyValue.class, HFileOutputFormat2.class, conf);
            LOG.info("Saved HFiles to: '{}'", path);
            flushPermission(conf, path);
            return path;
        } catch (IOException e) {
            LOG.error("Failed to generate files", e);
        }
        return Constants.EMPTY_STR;
    }

    @Override
    public void loadFiles(String path,Directions directions) {
        try {
            sinkToHBase.loadHfiles(path, getTableName());
        } catch (Exception e) {
            LOG.error("Failed to load hfiles", e);
        }
    }

    private String getTableName() {
        return struct.edges().size() > 0 ? loadOptions.edgeTableName : loadOptions.vertexTableName;
    }

    private Integer getTablePartitions() {
        return struct.edges().size() > 0 ? loadOptions.edgePartitions : loadOptions.vertexPartitions;
    }

    private String getHFilePath(Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        String fileID = fileID();
        String pathStr = fs.getWorkingDirectory().toString() + "/hfile-gen" + "/" + fileID + "/";
        Path hfileGenPath = new Path(pathStr);
        if (fs.exists(hfileGenPath)) {
            LOG.info("\n Delete the path where the hfile is generated,path {} ", pathStr);
            fs.delete(hfileGenPath, true);
        }
        return pathStr;
    }

    public HBaseDirectLoader(LoadOptions loadOptions, InputStruct struct, DistributedLoadMetrics loadDistributeMetrics) {
        super(loadOptions, struct, loadDistributeMetrics);
        this.sinkToHBase = new SinkToHBase(loadOptions);
    }

    public HBaseDirectLoader(LoadOptions loadOptions, InputStruct struct) {
        super(loadOptions, struct);
        this.sinkToHBase = new SinkToHBase(loadOptions);
    }

    @Override
    protected Tuple2<ImmutableBytesWritable, KeyValue> vertexSerialize(GraphElementSerializer serializer, Vertex vertex) {
        LOG.debug("vertex start serialize {}", vertex.toString());
        byte[] rowkey = serializer.getKeyBytes(vertex, null)._1;
        byte[] values = serializer.getValueBytes(vertex);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey, Bytes.toBytes(Constants.HBASE_COL_FAMILY), Bytes.toBytes(Constants.EMPTY_STR), values);
        return new Tuple2<>(rowKey, keyValue);
    }

    @Override
    protected Tuple2<ImmutableBytesWritable, KeyValue> edgeSerialize(GraphElementSerializer serializer, Edge edge,Directions direction) {
        LOG.debug("edge start serialize {}", edge.toString());
        byte[] rowkey = serializer.getKeyBytes(edge, direction)._1;
        byte[] values = serializer.getValueBytes(edge);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey, Bytes.toBytes(Constants.HBASE_COL_FAMILY), Bytes.toBytes(Constants.EMPTY_STR), values);
        return new Tuple2<>(rowKey, keyValue);
    }
}
