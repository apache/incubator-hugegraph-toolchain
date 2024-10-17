package org.apache.hugegraph.loader.direct.loader;


import lombok.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.direct.outputformat.SSTFileOutputFormat;
import org.apache.hugegraph.loader.direct.partitioner.HstorePartitioner;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.DistributedLoadMetrics;
import org.apache.hugegraph.rest.RestClientConfig;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.serializer.GraphElementSerializer;
import org.apache.hugegraph.serializer.direct.struct.Directions;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import scala.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.hugegraph.serializer.direct.HStoreSerializer.processAddresses;

public class HStoreDirectLoader extends AbstractDirectLoader<Tuple2<byte[], Integer>, byte[]> {

    public HStoreDirectLoader(LoadOptions loadOptions, InputStruct struct, DistributedLoadMetrics loadDistributeMetrics) {
        super(loadOptions, struct, loadDistributeMetrics);
    }

    public HStoreDirectLoader(LoadOptions loadOptions, InputStruct struct) {
        super(loadOptions, struct);
    }

    @Override
    public JavaPairRDD<Tuple2<byte[], Integer>, byte[]> buildVertexAndEdge(Dataset<Row> ds, Directions directions) {
        LOG.info("Start build vertexes and edges");
        return ds.toJavaRDD().mapPartitionsToPair(
                (PairFlatMapFunction<Iterator<Row>, Tuple2<byte[], Integer>, byte[]>) rowIter -> {
                    LoadContext loaderContext = new LoadContext(super.loadOptions);
                    loaderContext.init(struct);
                    List<ElementBuilder> buildersForGraphElement = getElementBuilders(loaderContext);
                    List<Tuple2<Tuple2<byte[], Integer>, byte[]>> result = new LinkedList<>();
                    while (rowIter.hasNext()) {
                        Row row = rowIter.next();
                        List<Tuple2<Tuple2<byte[], Integer>, byte[]>> serList;
                        serList = buildAndSer(loaderContext.getSerializer(), row, buildersForGraphElement,directions);
                        result.addAll(serList);
                    }
                    return result.iterator();
                }
        );
    }

    @Override
    public String generateFiles(JavaPairRDD<Tuple2<byte[], Integer>, byte[]> buildAndSerRdd) {
        LOG.info("bulkload start execute>>>");
        try {
            JavaPairRDD<Tuple2<byte[], Integer>, byte[]> tuple2JavaPairRDD = buildAndSerRdd.partitionBy(new HstorePartitioner(loadOptions.vertexPartitions));
            // abort partId
            JavaPairRDD<byte[], byte[]> javaPairRDD = tuple2JavaPairRDD.mapToPair(tuple2 -> new Tuple2<>(tuple2._1._1, tuple2._2));
            JavaPairRDD<byte[], byte[]> sortedRdd = javaPairRDD.mapPartitionsToPair(iterator -> {
                List<Tuple2<byte[], byte[]>> partitionData = new ArrayList<>();
                iterator.forEachRemaining(partitionData::add);
                Collections.sort(partitionData, new HStoreDirectLoader.TupleComparator());
                return partitionData.iterator();
            });
            Configuration hadoopConf = new Configuration();
            String sstFilePath = getSSTFilePath(hadoopConf);
            LOG.info("SSTFile生成的hdfs路径:{}", sstFilePath);
            sortedRdd.saveAsNewAPIHadoopFile(
                    sstFilePath,
                    byte[].class,
                    byte[].class,
                    SSTFileOutputFormat.class,
                    hadoopConf
            );
            flushPermission(hadoopConf, sstFilePath);
            return sstFilePath;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    public void loadFiles(String sstFilePath,Directions directions) {
        RestClientConfig config = RestClientConfig.builder()
                .connectTimeout(5 * 1000)  // 连接超时时间 5s
                .readTimeout(60 * 60 * 1000) // 读取超时时间 1h
                .maxConns(10) // 最大连接数
                .build();
        BulkloadInfo bulkloadInfo = new BulkloadInfo(loadOptions.graph,
                replaceClusterName(sstFilePath, loadOptions.hdfsUri),
                getBulkloadType(),
                directions,
                loadOptions.maxDownloadRate
        );
        String[] urls = processAddresses(loadOptions.pdAddress, loadOptions.pdRestPort);

        for (String url : urls) {
            LOG.info("submit bulkload task to {}, bulkloadInfo:{}", url, bulkloadInfo);
            RestClient client = null;
            try {
                client = new RestClient(url, config);
                RestResult restResult = client.post("v1/task/bulkload", bulkloadInfo);
                Map resMap = restResult.readObject(Map.class);
                LOG.info("Response :{} ", resMap);
                break;
            } catch (Exception e) {
                LOG.error("Failed to submit bulkload task", e);
                break;
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        LOG.error("Failed to close RestClient", e);
                    }
                }
            }
        }
    }

    @Override
    protected Tuple2<Tuple2<byte[], Integer>, byte[]> vertexSerialize(GraphElementSerializer serializer, Vertex vertex) {
        LOG.debug("vertex start serialize {}", vertex.toString());
        Tuple2<byte[], Integer> keyBytes = serializer.getKeyBytes(vertex, null);
        byte[] values = serializer.getValueBytes(vertex);
        return new Tuple2<>(keyBytes, values);
    }

    @Override
    protected Tuple2<Tuple2<byte[], Integer>, byte[]> edgeSerialize(GraphElementSerializer serializer, Edge edge,Directions direction) {
        Tuple2<byte[], Integer> keyBytes = serializer.getKeyBytes(edge,direction);
        byte[] values = serializer.getValueBytes(edge);
        return new Tuple2<>(keyBytes, values);
    }

    private BulkloadInfo.LoadType getBulkloadType() {
        return struct.edges().size() > 0 ? BulkloadInfo.LoadType.EDGE : BulkloadInfo.LoadType.VERTEX;
    }

    private String getSSTFilePath(Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        long timeStr = System.currentTimeMillis();
        String pathStr = fs.getWorkingDirectory().toString() + "/hg-1_5/gen-sstfile" + "/" + timeStr + "/";//sstFile 存储路径
        org.apache.hadoop.fs.Path hfileGenPath = new Path(pathStr);
        if (fs.exists(hfileGenPath)) {
            LOG.info("\n delete sstFile path \n");
            fs.delete(hfileGenPath, true);
        }
        return pathStr;
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

    @Data
    static class BulkloadInfo implements Serializable {
        String graphName;
        String tableName;
        String hdfsPath;
        Integer maxDownloadRate;

        public BulkloadInfo(String graphName, String path, LoadType loadType,Directions directions,int maxDownloadRate) {
            this.graphName = processGraphName(graphName);
            this.tableName = processTableName(loadType,directions);
            this.hdfsPath = path;
            this.maxDownloadRate = maxDownloadRate;
        }

        private String processGraphName(String graphName) {
            return graphName + "/g";
        }

        private String processTableName( LoadType loadType,Directions directions) {
            if (loadType == LoadType.VERTEX) {
                return "g+v";
            } else if ( null ==directions && loadType == LoadType.EDGE ) {
                return "g+oe";
            } else if ( directions == Directions.IN && loadType == LoadType.EDGE ) {
                return "g+ie";
            }else {
                throw new IllegalArgumentException("Invalid loadType: " + loadType);
            }
        }


        @Override
        public String toString() {
            return "BulkloadInfo{" +
                    "graphName='" + graphName + '\'' +
                    ", tableName='" + tableName + '\'' +
                    ", hdfsPath='" + hdfsPath + '\'' +
                    '}';
        }

        enum LoadType {
            VERTEX,
            EDGE
        }

    }


    public static String replaceClusterName(String originalPath, String replacement) {
        String regex = "(hdfs://)([^/]+)(/.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(originalPath);

        if (matcher.matches()) {
            return matcher.group(1) + replacement + matcher.group(3);
        } else {
            throw new IllegalArgumentException("The path does not match the expected format.");
        }
    }
}
