/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.structure.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.constant.EdgeLink;
import com.baidu.hugegraph.structure.constant.Frequency;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class EdgeLabel extends Indexable {

    @JsonProperty
    private Frequency frequency;
    @JsonProperty
    private Set<EdgeLink> links;
    @JsonProperty
    private List<String> sortKeys;

    @JsonCreator
    public EdgeLabel(@JsonProperty("name") String name) {
        super(name);
        this.frequency = Frequency.SINGLE;
        this.links = new HashSet<>();
        this.sortKeys = new ArrayList<>();
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

    public List<String> sortKeys() {
        return this.sortKeys;
    }

    public EdgeLabel sortKeys(String... sortKeys) {
        for (String sortKey : sortKeys) {
            if (!this.sortKeys.contains(sortKey)) {
                this.sortKeys.add(sortKey);
            }
        }
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

        public void append() {
            this.manager.appendEdgeLabel(this.edgeLabel);
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

        public Builder ifNotExist() {
            this.edgeLabel.checkExist = false;
            return this;
        }
    }
}
