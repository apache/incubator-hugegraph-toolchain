package com.baidu.hugegraph.structure.constant;

public enum HugeType {

    // schema
    VERTEX_LABEL(1, "vertexlabels"),
    EDGE_LABEL(2, "edgelabels"),
    PROPERTY_KEY(3, "propertykeys"),
    INDEX_LABEL(4, "indexlabels"),

    // data
    VERTEX(101, "vertices"),
    EDGE(120, "edges"),

    // gremlin
    GREMLIN(201, "gremlin");

    private int code;
    private String name = null;

    private HugeType(int code, String name) {
        assert code < 256;
        this.code = code;
        this.name = name;
    }

    public int code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }
}
