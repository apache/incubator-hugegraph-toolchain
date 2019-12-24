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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.exception.NotSupportException;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.baidu.hugegraph.util.CollectionUtil;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"properties"})
public class IndexLabel extends SchemaElement {

    @JsonProperty("base_type")
    private HugeType baseType;
    @JsonProperty("base_value")
    private String baseValue;
    @JsonProperty("index_type")
    private IndexType indexType;
    @JsonProperty("fields")
    private List<String> fields;

    @JsonCreator
    public IndexLabel(@JsonProperty("name") String name) {
        super(name);
        this.indexType = null;
        this.fields = new CopyOnWriteArrayList<>();
    }

    @Override
    public String type() {
        return HugeType.INDEX_LABEL.string();
    }

    public HugeType baseType() {
        return baseType;
    }

    public String baseValue() {
        return baseValue;
    }

    public IndexType indexType() {
        return indexType;
    }

    public List<String> indexFields() {
        return fields;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, baseType=%s, baseValue=%s, " +
                             "indexType=%s, fields=%s}",
                             this.name, this.baseType, this.baseValue,
                             this.indexType, this.fields);
    }

    public interface Builder extends SchemaBuilder<IndexLabel> {

        Builder on(boolean isVertex, String baseValue);

        Builder onV(String baseValue);

        Builder onE(String baseValue);

        Builder by(String... fields);

        Builder indexType(IndexType indexType);

        Builder secondary();

        Builder range();

        Builder search();

        Builder shard();

        Builder unique();

        Builder userdata(String key, Object val);

        Builder ifNotExist();
    }

    public static class BuilderImpl implements Builder {

        private IndexLabel indexLabel;
        private SchemaManager manager;

        public BuilderImpl(String name, SchemaManager manager) {
            this.indexLabel = new IndexLabel(name);
            this.manager = manager;
        }

        @Override
        public IndexLabel build() {
            return this.indexLabel;
        }

        @Override
        public IndexLabel create() {
            return this.manager.addIndexLabel(this.indexLabel);
        }

        @Override
        public IndexLabel append() {
            return this.manager.appendIndexLabel(this.indexLabel);
        }

        @Override
        public IndexLabel eliminate() {
            return this.manager.eliminateIndexLabel(this.indexLabel);
        }

        @Override
        public void remove() {
            this.manager.removeIndexLabel(this.indexLabel.name);
        }

        @Override
        public Builder on(boolean isVertex, String baseValue) {
            return isVertex ? this.onV(baseValue) : this.onE(baseValue);
        }

        @Override
        public Builder onV(String baseValue) {
            this.indexLabel.baseType = HugeType.VERTEX_LABEL;
            this.indexLabel.baseValue = baseValue;
            return this;
        }

        @Override
        public Builder onE(String baseValue) {
            this.indexLabel.baseType = HugeType.EDGE_LABEL;
            this.indexLabel.baseValue = baseValue;
            return this;
        }

        @Override
        public Builder by(String... fields) {
            E.checkArgument(this.indexLabel.fields.isEmpty(),
                            "Not allowed to assign index fields multi times");
            List<String> indexFields = Arrays.asList(fields);
            E.checkArgument(CollectionUtil.allUnique(indexFields),
                            "Invalid index fields %s, which contains some " +
                            "duplicate properties", indexFields);
            this.indexLabel.fields.addAll(indexFields);
            return this;
        }

        @Override
        public Builder indexType(IndexType indexType) {
            this.indexLabel.indexType = indexType;
            return this;
        }

        @Override
        public Builder secondary() {
            this.indexLabel.indexType = IndexType.SECONDARY;
            return this;
        }

        @Override
        public Builder range() {
            this.indexLabel.indexType = IndexType.RANGE;
            return this;
        }

        @Override
        public Builder search() {
            this.indexLabel.indexType = IndexType.SEARCH;
            return this;
        }

        @Override
        public Builder shard() {
            this.indexLabel.indexType = IndexType.SHARD;
            return this;
        }

        @Override
        public Builder unique() {
            this.indexLabel.indexType = IndexType.UNIQUE;
            return this;
        }

        @Override
        public Builder userdata(String key, Object val) {
            E.checkArgumentNotNull(key, "The user data key can't be null");
            E.checkArgumentNotNull(val, "The user data value can't be null");
            this.indexLabel.userdata.put(key, val);
            return this;
        }

        @Override
        public Builder ifNotExist() {
            this.indexLabel.checkExist = false;
            return this;
        }
    }

    public static class CreatedIndexLabel {

        @JsonProperty("index_label")
        private IndexLabel indexLabel;

        @JsonProperty("task_id")
        private long taskId;

        public IndexLabel indexLabel() {
            return this.indexLabel;
        }

        public long taskId() {
            return this.taskId;
        }
    }
}
