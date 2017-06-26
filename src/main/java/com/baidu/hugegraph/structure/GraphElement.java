package com.baidu.hugegraph.structure;

import java.util.Map;

import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by liningrui on 2017/5/23.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Vertex.class, name = "vertex"),
        @JsonSubTypes.Type(value = Edge.class, name = "edge")})
public abstract class GraphElement extends Element {

    @JsonProperty
    protected String id;
    @JsonProperty
    protected String label;
    @JsonProperty
    protected String type;
    @JsonProperty
    protected Map<String, Object> properties;

    public String id() {
        return this.id;
    }

    public void id(String id) {
        this.id = id;
    }

    public String label() {
        return this.label;
    }

    public String type() {
        return type;
    }

    public abstract GraphElement property(String key, Object value);

    public Map<String, Object> properties() {
        return properties;
    }
}
