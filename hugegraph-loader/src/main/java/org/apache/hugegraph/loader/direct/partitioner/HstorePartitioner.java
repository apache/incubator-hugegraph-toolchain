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
