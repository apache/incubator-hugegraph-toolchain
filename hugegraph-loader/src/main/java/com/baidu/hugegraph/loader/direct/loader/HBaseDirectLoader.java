package com.baidu.hugegraph.loader.direct.loader;

import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.constant.Constants;

import com.baidu.hugegraph.loader.direct.util.SinkToHBase;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.metrics.LoadDistributeMetrics;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.serializer.direct.HBaseSerializer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.StringUtil;
import org.apache.commons.lang.StringUtils;
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
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import scala.Tuple2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class HBaseDirectLoader extends DirectLoader<ImmutableBytesWritable, KeyValue> {

    private SinkToHBase sinkToHBase ;
    private LoadDistributeMetrics loadDistributeMetrics;;

    public static final Logger LOG = Log.logger(HBaseDirectLoader.class);

    public HBaseDirectLoader(LoadOptions loadOptions, InputStruct struct, LoadDistributeMetrics loadDistributeMetrics) {
        super(loadOptions,struct);
        this.loadDistributeMetrics=loadDistributeMetrics;
        this.sinkToHBase=new SinkToHBase(loadOptions);

    }

    public String getTableName(){

        String tableName = null;
        if (struct.edges().size() > 0) {//导入的是边  表名换成 出边表
            System.out.println("this is edge struct: " + struct.edges().size());
            tableName = this.loadOptions.edgeTablename;

        } else if (struct.vertices().size() > 0) {
            System.out.println("this is vertices struct: " + struct.vertices().size());
            tableName = this.loadOptions.vertexTablename;

        }
        return tableName;
    }

    public Integer getTablePartitions(){
        return   struct.edges().size() > 0 ? loadOptions.edgePartitions
                : loadOptions.vertexPartitions;
    }

    public JavaPairRDD<ImmutableBytesWritable, KeyValue> buildVertexAndEdge(Dataset<Row> ds) {
        LOG.info("buildAndSer start execute >>>>");
        JavaPairRDD<ImmutableBytesWritable, KeyValue> tuple2KeyValueJavaPairRDD = ds.toJavaRDD().mapPartitionsToPair(
                new PairFlatMapFunction<Iterator<Row>, ImmutableBytesWritable, KeyValue>() {

                    @Override
                    public Iterator<Tuple2<ImmutableBytesWritable, KeyValue>> call(Iterator<Row> rowIterator) throws Exception {

                        List<GraphElement> elementsElement;//存储返回的  GraphElement: Vertex/Edge

                        // 目标存储序列化器
                        HBaseSerializer serializer = new HBaseSerializer(HugeClientHolder.create(loadOptions),loadOptions.vertexPartitions,loadOptions.edgePartitions);
                        List<ElementBuilder> buildersForGraphElement = getElementBuilders();

                        /**
                         * 构建 vertex/edge --->  K-V
                         */
                        List<Tuple2<ImmutableBytesWritable,  KeyValue>> result = new LinkedList<>();
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            List<Tuple2<ImmutableBytesWritable, KeyValue>> serList = buildAndSer(serializer, row,buildersForGraphElement);
                            result.addAll(serList);
                        }
                        serializer.close();
                        return result.iterator();
                    }
                }
        );
        return tuple2KeyValueJavaPairRDD;
    }


    @Override
    String generateFiles(JavaPairRDD<ImmutableBytesWritable, KeyValue> buildAndSerRdd) {
        LOG.info("bulkload start execute>>>");
        try {
            Tuple2<SinkToHBase.IntPartitioner, TableDescriptor> tuple =
                    sinkToHBase.getPartitionerByTableName(getTablePartitions(), getTableName());
            Partitioner partitioner= (Partitioner) tuple._1;
            TableDescriptor tableDescriptor= (TableDescriptor) tuple._2;

            JavaPairRDD<ImmutableBytesWritable, KeyValue> repartitionedRdd =
                    buildAndSerRdd.repartitionAndSortWithinPartitions(partitioner);//精髓 partition内部做到有序
            Configuration conf = sinkToHBase.getHBaseConfiguration().get();
            Job job = Job.getInstance(conf);
            LOG.info("hbasedirectloader HFileOutputFormat2 tableDescriptor >>>"+tableDescriptor.toString());
            HFileOutputFormat2.configureIncrementalLoadMap(job, tableDescriptor);
            conf.set("hbase.mapreduce.hfileoutputformat.table.name", tableDescriptor.getTableName().getNameAsString());
            String path= getHFilePath(job.getConfiguration());

            /**
             * 准备bulkload相关配置信息
             */
            /**
             * 生成 HFile
             */
            LOG.info("hbasedirectloader HFileOutputFormat2 tablename >>>"+conf.get("hbase.mapreduce.hfileoutputformat.table.name"));
            repartitionedRdd.saveAsNewAPIHadoopFile(
                    path,
                    ImmutableBytesWritable.class,
                    KeyValue.class,
                    HFileOutputFormat2.class,
                    conf
            );
            LOG.info("Saved to HFiles to: " + path);
            flushPermission(conf,path);
            return path;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Constants.EMPTY_STR;

    }

    public String   getHFilePath(Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        long timeStr = System.currentTimeMillis();
        String pathStr = fs.getWorkingDirectory().toString() + "/hfile-gen" + "/" + timeStr+ "/";//HFile 存储路径
        Path hfileGenPath = new Path(pathStr);
        if(fs.exists(hfileGenPath)){
            LOG.info("\n delete hfile path \n");
            fs.delete(hfileGenPath,true);
        }
        return pathStr;
    }


    @Override
    public void loadFiles(String path) {
        long timeStr = System.currentTimeMillis();

        try {
            sinkToHBase.loadHfiles(path, getTableName());// BulkLoad HFile to HBase
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void flushPermission(Configuration conf, String path){
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

    // 构建点边然后序列化为目标存储的所需类型
    List<Tuple2<ImmutableBytesWritable,  KeyValue>> buildAndSer(HBaseSerializer serializer, Row row, List<ElementBuilder> buildersForGraphElement) {
        List<GraphElement> elementsElement;//存储返回的  GraphElement: Vertex/Edge

        List<Tuple2<ImmutableBytesWritable,  KeyValue>> result = new LinkedList<>();

        for (ElementBuilder builder : buildersForGraphElement) {   // 控制类型 点/边
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
                    throw new AssertionError(String.format(
                            "Unsupported input source '%s'",
                            struct.input().type()));
            }

            boolean isVertex = builder.mapping().type().isVertex();
            if (isVertex) {
                for (Vertex vertex : (List<Vertex>) (Object) elementsElement) {
                    LOG.info("vertex already build done >>>" + vertex.toString());
                    Tuple2<ImmutableBytesWritable, KeyValue> tuple2 = vertexSerialize(serializer,vertex);
                    loadDistributeMetrics.increaseDisVertexInsertSuccess(builder.mapping());
                    result.add(tuple2);
                }
            } else {
                for (Edge edge : (List<Edge>) (Object) elementsElement) {
                    LOG.info("edge already build done >>>" + edge.toString());
                    Tuple2<ImmutableBytesWritable, KeyValue> tuple2 = edgeSerialize(serializer,edge);
                    loadDistributeMetrics.increaseDisEdgeInsertSuccess(builder.mapping());
                    result.add(tuple2);

                }
            }
        }
        return result;
    }

    private Tuple2<ImmutableBytesWritable, KeyValue> edgeSerialize(HBaseSerializer serializer, Edge edge) {
        LOG.info("edge start serialize >>>" + edge.toString());
        byte[] rowkey = serializer.getKeyBytes(edge);
        byte[] values = serializer.getValueBytes(edge);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey,
                Bytes.toBytes(Constants.HBASE_COL_FAMILY),
                Bytes.toBytes(Constants.EMPTY_STR),
                values);
        return new Tuple2<>(rowKey,keyValue);
    }

    private Tuple2<ImmutableBytesWritable, KeyValue> vertexSerialize(HBaseSerializer serializer, Vertex vertex) {
        LOG.info("vertex start serialize >>>" + vertex.toString());
        byte[] rowkey = serializer.getKeyBytes(vertex);
        byte[] values = serializer.getValueBytes(vertex);
        ImmutableBytesWritable rowKey = new ImmutableBytesWritable();
        rowKey.set(rowkey);
        KeyValue keyValue = new KeyValue(rowkey,
                Bytes.toBytes(Constants.HBASE_COL_FAMILY),
                Bytes.toBytes(Constants.EMPTY_STR),
                values);
        return new Tuple2<>(rowKey,keyValue);
    }


}
