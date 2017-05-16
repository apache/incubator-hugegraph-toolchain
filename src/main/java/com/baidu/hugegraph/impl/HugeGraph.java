package com.baidu.hugegraph.impl;

import com.baidu.hugegraph.api.graph.Graph;
import com.baidu.hugegraph.api.gremlin.GremlinExecutor;
import com.baidu.hugegraph.api.schema.Schema;

/**
 * Created by jishilei on 2017/5/16.
 */
public class HugeGraph {

    private String url;
    private String name;

    public String name() {
        return this.name;
    }

    public String url() {
        return this.url;
    }

    public HugeGraph(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public static HugeGraph open(String url, String name) {
        return null;
    }

    public GremlinExecutor gremlin() {
        return null;
    }

    public Schema schema() {
        return null;
    }

    public Graph graph() {
        return null;
    }


}
