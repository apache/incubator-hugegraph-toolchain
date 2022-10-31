package com.baidu.hugegraph.loader.mapping;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"edge_tablename", "vertex_tablename", "hbase_zookeeper_quorum", "hbase_zookeeper_property_clientPort", "zookeeper_znode_parent"})
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
    @JsonProperty("target_hdfs_hfile_path")
    private String targetHdfsHfilePath;

    public String getTargetHdfsHfilePath() {
        return targetHdfsHfilePath;
    }

    public void setTargetHdfsHfilePath(String targetHdfsHfilePath) {
        this.targetHdfsHfilePath = targetHdfsHfilePath;
    }




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
