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

package org.apache.hugegraph.serializer;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.serializer.config.SerializerConfig;
import org.apache.hugegraph.serializer.direct.HBaseSerializer;
import org.apache.hugegraph.serializer.direct.HStoreSerializer;

public class SerializerFactory {

    public static GraphElementSerializer getSerializer(HugeClient client, SerializerConfig config) {
        switch (config.getBackendStoreType()) {
            case "hstore":
                return new HStoreSerializer(client, config.getVertexPartitions(),config.getGraphName(),config.getPdAddress(),config.getPdRestPort());
            case "hbase":
                return new HBaseSerializer(client, config.getVertexPartitions(),config.getEdgePartitions());
            default:
                throw new IllegalArgumentException("Unsupported serializer backend type: " + config.getBackendStoreType());
        }
    }
}
