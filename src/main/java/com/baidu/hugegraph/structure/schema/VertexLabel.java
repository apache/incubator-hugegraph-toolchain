package com.baidu.hugegraph.structure.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/5/11.
 */
public class VertexLabel extends SchemaElement {

    @JsonProperty
    private Set<String> primaryKeys;
    @JsonProperty
    protected Set<String> indexNames;

    @JsonCreator
    public VertexLabel(@JsonProperty("name") String name) {
        super(name);
        this.primaryKeys = new HashSet<>();
        this.indexNames = new HashSet<>();
    }

    @Override
    public String type() {
        return HugeType.VERTEX_LABEL.string();
    }

    public Set<String> primaryKeys() {
        return this.primaryKeys;
    }

    public VertexLabel primaryKeys(String... primaryKeys) {
        this.primaryKeys.addAll(Arrays.asList(primaryKeys));
        return this;
    }

    public Set<String> indexNames() {
        return this.indexNames;
    }

    public VertexLabel indexNames(String... indexNames) {
        this.indexNames.addAll(Arrays.asList(indexNames));
        return this;
    }

    public VertexLabel properties(String... properties) {
        this.properties.addAll(Arrays.asList(properties));
        return this;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, primaryKeys=%s, "
                        + "indexNames=%s, properties=%s}",
                this.name,
                this.primaryKeys,
                this.indexNames,
                this.properties);
    }

    public static class Builder {

        private VertexLabel vertexLabel;
        private SchemaManager manager;

        public Builder(String name, SchemaManager manager) {
            this.vertexLabel = new VertexLabel(name);
            this.manager = manager;
        }

        public VertexLabel create() {
            this.manager.addVertexLabel(this.vertexLabel);
            return this.vertexLabel;
        }

        public Builder properties(String... properties) {
            this.vertexLabel.properties.addAll(Arrays.asList(properties));
            return this;
        }

        public Builder primaryKeys(String... primaryKeys) {
            this.vertexLabel.primaryKeys.addAll(Arrays.asList(primaryKeys));
            return this;
        }
    }
}