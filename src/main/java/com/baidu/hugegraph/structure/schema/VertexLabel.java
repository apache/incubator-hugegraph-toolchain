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
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.util.CollectionUtil;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VertexLabel extends SchemaLabel {

    @JsonProperty("id_strategy")
    private IdStrategy idStrategy;
    @JsonProperty("primary_keys")
    private List<String> primaryKeys;

    @JsonCreator
    public VertexLabel(@JsonProperty("name") String name) {
        super(name);
        this.idStrategy = IdStrategy.DEFAULT;
        this.primaryKeys = new CopyOnWriteArrayList<>();
    }

    @Override
    public String type() {
        return HugeType.VERTEX_LABEL.string();
    }

    public IdStrategy idStrategy() {
        return this.idStrategy;
    }

    public List<String> primaryKeys() {
        return this.primaryKeys;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, idStrategy=%s, primaryKeys=%s, " +
                             "nullableKeys=%s, properties=%s}",
                             this.name, this.idStrategy, this.primaryKeys,
                             this.nullableKeys, this.properties);
    }

    public interface Builder extends SchemaBuilder<VertexLabel> {

        Builder useAutomaticId();

        Builder usePrimaryKeyId();

        Builder useCustomizeStringId();

        Builder useCustomizeNumberId();

        Builder properties(String... properties);

        Builder primaryKeys(String... keys);

        Builder nullableKeys(String... keys);

        Builder enableLabelIndex(boolean enable);

        Builder userdata(String key, Object val);

        Builder ifNotExist();
    }

    public static class BuilderImpl implements Builder {

        private VertexLabel vertexLabel;
        private SchemaManager manager;

        public BuilderImpl(String name, SchemaManager manager) {
            this.vertexLabel = new VertexLabel(name);
            this.manager = manager;
        }

        @Override
        public VertexLabel build() {
            return this.vertexLabel;
        }

        @Override
        public VertexLabel create() {
            return this.manager.addVertexLabel(this.vertexLabel);
        }

        @Override
        public VertexLabel append() {
            return this.manager.appendVertexLabel(this.vertexLabel);
        }

        @Override
        public VertexLabel eliminate() {
            return this.manager.eliminateVertexLabel(this.vertexLabel);
        }

        @Override
        public void remove() {
            this.manager.removeVertexLabel(this.vertexLabel.name);
        }

        @Override
        public Builder useAutomaticId() {
            this.checkIdStrategy();
            this.vertexLabel.idStrategy = IdStrategy.AUTOMATIC;
            return this;
        }

        @Override
        public Builder usePrimaryKeyId() {
            this.checkIdStrategy();
            this.vertexLabel.idStrategy = IdStrategy.PRIMARY_KEY;
            return this;
        }

        @Override
        public Builder useCustomizeStringId() {
            this.checkIdStrategy();
            this.vertexLabel.idStrategy = IdStrategy.CUSTOMIZE_STRING;
            return this;
        }

        @Override
        public Builder useCustomizeNumberId() {
            this.checkIdStrategy();
            this.vertexLabel.idStrategy = IdStrategy.CUSTOMIZE_NUMBER;
            return this;
        }

        @Override
        public Builder properties(String... properties) {
            this.vertexLabel.properties.addAll(Arrays.asList(properties));
            return this;
        }

        @Override
        public Builder primaryKeys(String... keys) {
            E.checkArgument(this.vertexLabel.primaryKeys.isEmpty(),
                            "Not allowed to assign primary keys multi times");
            List<String> primaryKeys = Arrays.asList(keys);
            E.checkArgument(CollectionUtil.allUnique(primaryKeys),
                            "Invalid primary keys %s, which contains some " +
                            "duplicate properties", primaryKeys);
            this.vertexLabel.primaryKeys.addAll(primaryKeys);
            return this;
        }

        @Override
        public Builder nullableKeys(String... keys) {
            this.vertexLabel.nullableKeys.addAll(Arrays.asList(keys));
            return this;
        }

        @Override
        public Builder enableLabelIndex(boolean enable) {
            this.vertexLabel.enableLabelIndex = enable;
            return this;
        }

        @Override
        public Builder userdata(String key, Object val) {
            E.checkArgumentNotNull(key, "The user data key can't be null");
            E.checkArgumentNotNull(val, "The user data value can't be null");
            this.vertexLabel.userdata.put(key, val);
            return this;
        }

        @Override
        public Builder ifNotExist() {
            this.vertexLabel.checkExist = false;
            return this;
        }

        private void checkIdStrategy() {
            E.checkArgument(this.vertexLabel.idStrategy == IdStrategy.DEFAULT,
                            "Not allowed to change id strategy for " +
                            "vertex label '%s'", this.vertexLabel.name);
        }
    }
}