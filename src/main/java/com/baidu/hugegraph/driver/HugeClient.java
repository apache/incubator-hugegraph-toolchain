package com.baidu.hugegraph.driver;

/**
 * Created by liningrui on 2017/5/16.
 */
public class HugeClient {

    private SchemaManager schema;
    private GraphManager graph;
    private GremlinManager gremlin;

    public HugeClient(String url, String graph) {
        this.schema = new SchemaManager(url, graph);
        this.graph = new GraphManager(url, graph);
        this.gremlin = new GremlinManager(url);
    }

    public static HugeClient open(String url, String name) {
        return new HugeClient(url, name);
    }

    public SchemaManager schema() {
        return this.schema;
    }

    public GraphManager graph() {
        return this.graph;
    }

    public GremlinManager gremlin() {
        return this.gremlin;
    }

}
