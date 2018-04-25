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
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.structure.schema;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.constant.Frequency;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.util.CollectionUtil;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EdgeLabel extends SchemaLabel {

    @JsonProperty("frequency")
    private Frequency frequency;
    @JsonProperty("source_label")
    private String sourceLabel;
    @JsonProperty("target_label")
    private String targetLabel;
    @JsonProperty("sort_keys")
    private List<String> sortKeys;

    @JsonCreator
    public EdgeLabel(@JsonProperty("name") String name) {
        super(name);
        this.frequency = Frequency.DEFAULT;
        this.sortKeys = new CopyOnWriteArrayList<>();
    }

    @Override
    public String type() {
        return HugeType.EDGE_LABEL.string();
    }

    public Frequency frequency() {
        return this.frequency;
    }

    public String sourceLabel() {
        return this.sourceLabel;
    }

    public String targetLabel() {
        return this.targetLabel;
    }

    public List<String> sortKeys() {
        return this.sortKeys;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, sourceLabel=%s, targetLabel=%s, " +
                             "sortKeys=%s, nullableKeys=%s, properties=%s}",
                             this.name, this.sourceLabel, this.targetLabel,
                             this.sortKeys, this.nullableKeys, this.properties);
    }

    public interface Builder extends SchemaBuilder<EdgeLabel> {

        Builder properties(String... properties);

        Builder sortKeys(String... keys);

        Builder nullableKeys(String... keys);

        Builder link(String sourceLabel, String targetLabel);

        Builder sourceLabel(String label);

        Builder targetLabel(String label);

        Builder singleTime();

        Builder multiTimes();

        Builder enableLabelIndex(boolean enable);

        Builder userdata(String key, Object val);

        Builder ifNotExist();
    }

    public static class BuilderImpl implements Builder {

        private EdgeLabel edgeLabel;
        private SchemaManager manager;

        public BuilderImpl(String name, SchemaManager manager) {
            this.edgeLabel = new EdgeLabel(name);
            this.manager = manager;
        }

        @Override
        public EdgeLabel build() {
            return this.edgeLabel;
        }

        @Override
        public EdgeLabel create() {
            return this.manager.addEdgeLabel(this.edgeLabel);
        }

        @Override
        public EdgeLabel append() {
            return this.manager.appendEdgeLabel(this.edgeLabel);
        }

        @Override
        public EdgeLabel eliminate() {
            return this.manager.eliminateEdgeLabel(this.edgeLabel);
        }

        @Override
        public void remove() {
            this.manager.removeEdgeLabel(this.edgeLabel.name);
        }

        @Override
        public Builder properties(String... properties) {
            this.edgeLabel.properties.addAll(Arrays.asList(properties));
            return this;
        }

        @Override
        public Builder sortKeys(String... keys) {
            E.checkArgument(this.edgeLabel.sortKeys.isEmpty(),
                            "Not allowed to assign sort keys multi times");
            List<String> sortKeys = Arrays.asList(keys);
            E.checkArgument(CollectionUtil.allUnique(sortKeys),
                            "Invalid sort keys %s, which contains some " +
                            "duplicate properties", sortKeys);
            this.edgeLabel.sortKeys.addAll(sortKeys);
            return this;
        }

        @Override
        public Builder nullableKeys(String... keys) {
            this.edgeLabel.nullableKeys.addAll(Arrays.asList(keys));
            return this;
        }

        @Override
        public Builder link(String sourceLabel, String targetLabel) {
            this.edgeLabel.sourceLabel = sourceLabel;
            this.edgeLabel.targetLabel = targetLabel;
            return this;
        }

        @Override
        public Builder sourceLabel(String label) {
            this.edgeLabel.sourceLabel = label;
            return this;
        }

        @Override
        public Builder targetLabel(String label) {
            this.edgeLabel.targetLabel = label;
            return this;
        }

        @Override
        public Builder singleTime() {
            this.checkFrequency();
            this.edgeLabel.frequency = Frequency.SINGLE;
            return this;
        }

        @Override
        public Builder multiTimes() {
            this.checkFrequency();
            this.edgeLabel.frequency = Frequency.MULTIPLE;
            return this;
        }

        @Override
        public Builder enableLabelIndex(boolean enable) {
            this.edgeLabel.enableLabelIndex = enable;
            return this;
        }

        @Override
        public Builder userdata(String key, Object val) {
            E.checkArgumentNotNull(key, "The user data key can't be null");
            E.checkArgumentNotNull(val, "The user data value can't be null");
            this.edgeLabel.userdata.put(key, val);
            return this;
        }

        @Override
        public Builder ifNotExist() {
            this.edgeLabel.checkExist = false;
            return this;
        }

        private void checkFrequency() {
            E.checkArgument(this.edgeLabel.frequency == Frequency.DEFAULT,
                            "Not allowed to change frequency for " +
                            "edge label '%s'", this.edgeLabel.name);
        }
    }
}
