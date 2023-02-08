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

package org.apache.hugegraph.loader.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"edge_tablename", "vertex_tablename", "hbase_zookeeper_quorum",
                    "hbase_zookeeper_property_clientPort", "zookeeper_znode_parent"})
public class BackendStoreInfo {

    @JsonProperty("edge_tablename")
    private String edgeTablename;
    @JsonProperty("vertex_tablename")
    private String vertexTablename;
    @JsonProperty("hbase_zookeeper_quorum")
    private String hbaseZKQuorum;
    @JsonProperty("hbase_zookeeper_property_clientPort")
    private String hbaseZKPort;
    @JsonProperty("zookeeper_znode_parent")
    private String hbaseZKParent;

    public String getEdgeTablename() {
        return edgeTablename;
    }

    public void setEdgeTablename(String edgeTablename) {
        this.edgeTablename = edgeTablename;
    }

    public String isVertexTablename() {
        return vertexTablename;
    }

    public void setVertexTablename(String vertexTablename) {
        this.vertexTablename = vertexTablename;
    }

    public String getVertexTablename() {
        return vertexTablename;
    }

    public String getHbaseZKQuorum() {
        return hbaseZKQuorum;
    }

    public void setHbaseZKQuorum(String hbaseZKQuorum) {
        this.hbaseZKQuorum = hbaseZKQuorum;
    }

    public String getHbaseZKPort() {
        return hbaseZKPort;
    }

    public void setHbaseZKPort(String hbaseZKPort) {
        this.hbaseZKPort = hbaseZKPort;
    }

    public String getHbaseZKParent() {
        return hbaseZKParent;
    }

    public void setHbaseZKParent(String hbaseZKParent) {
        this.hbaseZKParent = hbaseZKParent;
    }
}
