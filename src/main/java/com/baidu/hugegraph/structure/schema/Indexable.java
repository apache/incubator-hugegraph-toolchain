package com.baidu.hugegraph.structure.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.baidu.hugegraph.structure.SchemaElement;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/6/5.
 */
public abstract class Indexable extends SchemaElement {

    @JsonProperty
    protected Set<String> indexNames;

    public Indexable(String name) {
        super(name);
        this.indexNames = new HashSet<>();
    }

    public Set<String> indexNames() {
        return this.indexNames;
    }

    public Indexable indexNames(String... indexNames) {
        this.indexNames.addAll(Arrays.asList(indexNames));
        return this;
    }

}
