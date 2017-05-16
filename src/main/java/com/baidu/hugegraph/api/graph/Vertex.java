package com.baidu.hugegraph.api.graph;

import java.util.Map;

/**
 * Created by jishilei on 2017/5/11.
 */
public class Vertex {

    private String label;
    private Map<String, Object> keys;
    private Map<String, Object> properties;

    public Vertex(String label, Map<String, Object> keys, Map<String, Object> properties) {
        this.label = label;
        this.keys = keys;
        this.properties = properties;
    }

    public String label() {
        return label;
    }

    public Map<String, Object> keys() {
        return keys;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    @Override
    public String toString() {
        return "vertex: label=" + label + " ,keys=" + keys.toString() + " ,properties=" + properties.toString();
    }
}
