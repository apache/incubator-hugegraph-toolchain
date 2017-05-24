package com.baidu.hugegraph.structure.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.EdgeLink;
import com.baidu.hugegraph.structure.constant.Frequency;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/5/11.
 */
public class EdgeLabel extends SchemaElement {

    @JsonProperty
    private Frequency frequency;
    @JsonProperty
    private Set<EdgeLink> links;
    @JsonProperty
    private Set<String> sortKeys;
    @JsonProperty
    private Set<String> indexNames;

    @JsonCreator
    public EdgeLabel(@JsonProperty("name") String name) {
        super(name);
        this.frequency = Frequency.SINGLE;
        this.links = new HashSet<>();
        this.sortKeys = new HashSet<>();
        this.indexNames = new HashSet<>();
    }

    @Override
    public String type() {
        return HugeType.EDGE_LABEL.string();
    }

    public Frequency frequency() {
        return this.frequency;
    }

    public EdgeLabel frequency(Frequency frequency) {
        this.frequency = frequency;
        return this;
    }

    public Set<EdgeLink> links() {
        return this.links;
    }

    public EdgeLabel links(Set<EdgeLink> links) {
        this.links = links;
        return this;
    }

    public Set<String> sortKeys() {
        return this.sortKeys;
    }

    public EdgeLabel sortKeys(String... sortKeys) {
        this.sortKeys.addAll(Arrays.asList(sortKeys));
        return this;
    }

    public Set<String> indexNames() {
        return this.indexNames;
    }

    public EdgeLabel indexNames(String... indexNames) {
        this.indexNames.addAll(Arrays.asList(indexNames));
        return this;
    }

    public EdgeLabel properties(String... properties) {
        this.properties.addAll(Arrays.asList(properties));
        return this;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, links=%s, sortKeys=%s, "
                        + "indexNames=%s, properties=%s}",
                this.name,
                this.links,
                this.sortKeys,
                this.indexNames,
                this.properties);
    }

    public static class Builder {

        private EdgeLabel edgeLabel;
        private SchemaManager manager;

        public Builder(String name, SchemaManager manager) {
            this.edgeLabel = new EdgeLabel(name);
            this.manager = manager;
        }

        public EdgeLabel create() {
            this.manager.addEdgeLabel(this.edgeLabel);
            return this.edgeLabel;
        }

        public Builder properties(String... properties) {
            this.edgeLabel.properties.addAll(Arrays.asList(properties));
            return this;
        }

        public Builder sortKeys(String... sortKeys) {
            this.edgeLabel.sortKeys.addAll(Arrays.asList(sortKeys));
            return this;
        }

        public Builder indexNames(String... indexNames) {
            this.edgeLabel.indexNames.addAll(Arrays.asList(indexNames));
            return this;
        }

        public Builder link(String src, String tgt) {
            this.edgeLabel.links.add(EdgeLink.of(src, tgt));
            return this;
        }

        public Builder singleTime() {
            this.edgeLabel.frequency = Frequency.SINGLE;
            return this;
        }

        public Builder multiTimes() {
            this.edgeLabel.frequency = Frequency.MULTIPLE;
            return this;
        }
    }
}
