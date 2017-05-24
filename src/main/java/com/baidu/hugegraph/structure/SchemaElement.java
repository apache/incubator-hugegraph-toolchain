package com.baidu.hugegraph.structure;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/5/22.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SchemaElement extends Element {

    @JsonProperty
    protected String name;
    @JsonProperty
    protected Set<String> properties;
    @JsonIgnore
    protected boolean checkExits;

    public SchemaElement(String name) {
        this.name = name;
        this.properties = new HashSet<String>();
        this.checkExits = false;
    }

    public String name() {
        return this.name;
    }

    public Set<String> properties() {
        return this.properties;
    }

    public boolean checkExits() {
        return this.checkExits;
    }

}
