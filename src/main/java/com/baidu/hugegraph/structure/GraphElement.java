package com.baidu.hugegraph.structure;

import java.util.Map;

import com.baidu.hugegraph.structure.constant.HugeType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/5/23.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
