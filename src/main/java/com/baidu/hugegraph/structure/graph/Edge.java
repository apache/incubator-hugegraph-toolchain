package com.baidu.hugegraph.structure.graph;

import java.util.HashMap;

import com.baidu.hugegraph.structure.GraphElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by liningrui on 2017/5/11.
 */
public class Edge extends GraphElement {

    private String source;
    private String target;
    @JsonProperty("outVLabel")
    private String sourceLabel;
    @JsonProperty("inVLabel")
    private String targetLabel;

    @JsonCreator
    public Edge(@JsonProperty("label") String label) {
        this.label = label;
        this.properties = new HashMap<>();
    }

    @JsonProperty("source")
    public String source() {
        return this.source;
    }

    @JsonProperty("outV")
    public void source(String source) {
        this.source = source;
    }

    @JsonProperty("target")
    public String target() {
        return this.target;
    }

    @JsonProperty("inV")
    public void target(String target) {
        this.target = target;
    }

    public String sourceLabel() {
        return sourceLabel;
    }

    public void sourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String targetLabel() {
        return targetLabel;
    }

    public void targetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    @Override
    public String toString() {
        return String.format("{source=%s, target=%s, label=%s, properties=%s}",
                this.source,
                this.target,
                this.label,
                this.properties);
    }
}
