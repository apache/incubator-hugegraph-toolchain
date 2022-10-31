package com.baidu.hugegraph.loader.direct.util;

import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.util.Log;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.tool.BulkLoadHFilesTool;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.spark.Partitioner;
import org.slf4j.Logger;
import scala.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;


public class SinkToHBase implements Serializable {
    private LoadOptions loadOptions;
    public static final Logger LOG = Log.logger(SinkToHBase.class);

    public SinkToHBase(LoadOptions loadOptions) {
        this.loadOptions=loadOptions;
    }


    public Optional<Configuration> getHBaseConfiguration(){
        Configuration baseConf = HBaseConfiguration.create();
        baseConf.set("hbase.zookeeper.quorum", this.loadOptions.hbaseZKQuorum);
        baseConf.set("hbase.zookeeper.property.clientPort", this.loadOptions.hbaseZKPort);
        baseConf.set("zookeeper.znode.parent", this.loadOptions.hbaseZKParent);

        return Optional.ofNullable(baseConf);
    }

    private Optional<Connection> getConnection(){
        Optional<Configuration> baseConf = getHBaseConfiguration();
        Connection conn=null;
        try {
            conn = ConnectionFactory.createConnection(baseConf.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(conn);
    }
    public Tuple2<IntPartitioner, TableDescriptor> getPartitionerByTableName (int numPartitions, String tableName) throws IOException {
        LOG.debug("getPartitionerByTableName 接收的tablename 》》》"+tableName);
        Optional<Connection> optionalConnection = getConnection();
        TableDescriptor descriptor = optionalConnection
                .get()
                .getTable(TableName.valueOf(tableName))
                .getDescriptor();
        LOG.debug("getPartitionerByTableName get TableDescriptor 》》》"+descriptor.getTableName());
        optionalConnection.get().close();
        return new Tuple2<IntPartitioner,TableDescriptor>(new IntPartitioner(numPartitions, tableName),descriptor);
    }

    public void loadHfiles (String path, String tableName) throws Exception {
        Connection conn = getConnection().get();

        Table table = conn.getTable(TableName.valueOf(tableName));
        Configuration conf = conn.getConfiguration();
        System.out.println("conf >>"+conf.get("dfs.client.failover.proxy.provider.hwyriskmgtcluster"));
        System.out.println("conf >>"+conf.get("dfs.ha.namenodes.hwyriskmgtcluster"));
        System.out.println("conf >>"+conf.get("dfs.namenode.http-address.hwyriskmgtcluster.nn1"));
        System.out.println("conf >>"+conf.get("dfs.namenode.http-address.hwyriskmgtcluster.nn2"));
        BulkLoadHFilesTool bulkLoadHFilesTool = new BulkLoadHFilesTool(conf);

        //TODO: load HFile to HBase
        bulkLoadHFilesTool.bulkLoad(table.getName(), new Path(path));
        table.close();
        conn.close();

    }


    //精髓 repartitionAndSort
    public class IntPartitioner extends Partitioner {
        private final int numPartitions;
        public Map<List<String>, Integer> rangeMap = new HashMap<>();
        private String tableName;

        public IntPartitioner(int numPartitions, String tableName) throws IOException {
            this.numPartitions = numPartitions;
            this.rangeMap = getRangeMap(tableName);
            this.tableName = tableName;
        }


        private Map<List<String>, Integer> getRangeMap(String tableName) throws IOException {
            Connection conn = getConnection().get();

            HRegionLocator locator = (HRegionLocator) conn.getRegionLocator(TableName.valueOf(tableName));

            Pair<byte[][], byte[][]> startEndKeys = locator.getStartEndKeys();

            Map<List<String>, Integer> rangeMap = new HashMap<>();
            for (int i = 0; i < startEndKeys.getFirst().length; i++) {
                String startKey = Bytes.toString(startEndKeys.getFirst()[i]);
                String endKey = Bytes.toString(startEndKeys.getSecond()[i]);

                rangeMap.put(new ArrayList<>(Arrays.asList(startKey, endKey)), i);
            }
            conn.close();
            return rangeMap;
        }

        @Override
        public int numPartitions() {
            return numPartitions;
        }

        @Override
        public int getPartition(Object key) {
            if (key instanceof ImmutableBytesWritable) {

                try {
                    ImmutableBytesWritable immutableBytesWritableKey = (ImmutableBytesWritable) key;

                    if (rangeMap == null || rangeMap.isEmpty()) {
                        rangeMap = getRangeMap(this.tableName);
                    }

                    String keyString = Bytes.toString(immutableBytesWritableKey.get());
                    for (List<String> range : rangeMap.keySet()) {
                        if (keyString.compareToIgnoreCase(range.get(0)) >= 0
                                && ((keyString.compareToIgnoreCase(range.get(1)) < 0)
                                || range.get(1).equals(""))) {
                            return rangeMap.get(range);
                        }
                    }
                    LOG.error("Didn't find proper key in rangeMap, so returning 0 ...");
                    return 0;
                } catch (Exception e) {
                    LOG.error("When trying to get partitionID, "
                            + "encountered exception: " + e + "\t key = " + key);
                    return 0;
                }
            } else {
                LOG.error("key is NOT ImmutableBytesWritable type ...");
                return 0;
            }
        }
    }

}







