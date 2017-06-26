package com.baidu.hugegraph.structure.graph;

import java.util.List;

import com.baidu.hugegraph.structure.GraphElement;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/6/25.
 */
public class Path {

    @JsonProperty
    private List<Object> labels;
    @JsonProperty
    private List<GraphElement> objects;

    public List<Object> labels() {
        return labels;
    }

    public List<GraphElement> objects() {
        return objects;
    }

    @Override
    public String toString() {
        return String.format("{labels=%s, objects=%s}",
                this.labels,
                this.objects);
    }
}
