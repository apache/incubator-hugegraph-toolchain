package org.apache.hugegraph.loader.direct.partitioner;


import org.apache.spark.Partitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

public class HstorePartitioner extends Partitioner {
    private static final Logger LOG = LoggerFactory.getLogger(HstorePartitioner.class);

    private final int numPartitions;

    public HstorePartitioner(int numPartitions) {
        this.numPartitions = numPartitions;
    }


    @Override
    public int numPartitions() {
        return numPartitions;
    }

    @Override
    public int getPartition(Object key) {

            try {
                return  ((Tuple2<byte[], Integer>) key)._2;
            } catch (Exception e) {
                LOG.error("When trying to get partitionID, encountered exception: {} \t key = {}", e, key);
                return 0;
            }

    }
}
