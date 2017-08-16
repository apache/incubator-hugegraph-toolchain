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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class VertexLabel extends Indexable {

    @JsonProperty
    private IdStrategy idStrategy;
    @JsonProperty
    private List<String> primaryKeys;

    @JsonCreator
    public VertexLabel(@JsonProperty("name") String name) {
        super(name);
        this.idStrategy = IdStrategy.DAFAULT;
        this.primaryKeys = new ArrayList<>();
    }

    @Override
    public String type() {
        return HugeType.VERTEX_LABEL.string();
    }

    public IdStrategy idStrategy() {
        return this.idStrategy;
    }

    public void idStrategy(IdStrategy idStrategy) {
        E.checkArgument(this.idStrategy == IdStrategy.DAFAULT ||
                        this.idStrategy == idStrategy,
                        "Not allowed to change id strategy for " +
                        "vertex label '%s'", this.name);
        this.idStrategy = idStrategy;
    }

    public List<String> primaryKeys() {
        return this.primaryKeys;
    }

    public VertexLabel primaryKeys(String... primaryKeys) {
        for (String primaryKey : primaryKeys) {
            if (!this.primaryKeys.contains(primaryKey)) {
                this.primaryKeys.add(primaryKey);
            }
        }
        return this;
    }

    public VertexLabel properties(String... properties) {
        this.properties.addAll(Arrays.asList(properties));
        return this;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, primaryKeys=%s, " +
                             "indexNames=%s, properties=%s}",
                             this.name, this.primaryKeys,
                             this.indexNames, this.properties);
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

        public void append() {
            this.manager.appendVertexLabel(this.vertexLabel);
        }

        public void eliminate() {
            this.manager.eliminateVertexLabel(this.vertexLabel);
        }

        public void remove() {
            this.manager.removeVertexLabel(this.vertexLabel.name);
        }

        public Builder useAutomaticId() {
            this.vertexLabel.idStrategy(IdStrategy.AUTOMATIC);
            return this;
        }

        public Builder useCustomizeId() {
            this.vertexLabel.idStrategy(IdStrategy.CUSTOMIZE);
            return this;
        }

        public Builder usePrimaryKeyId() {
            this.vertexLabel.idStrategy(IdStrategy.PRIMARY_KEY);
            return this;
        }

        public Builder properties(String... properties) {
            this.vertexLabel.properties(properties);
            return this;
        }

        public Builder primaryKeys(String... keys) {
            this.vertexLabel.primaryKeys(keys);
            return this;
        }

        public Builder ifNotExist() {
            this.vertexLabel.checkExist = false;
            return this;
        }
    }
}