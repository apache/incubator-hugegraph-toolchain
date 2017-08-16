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
import java.util.List;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties({"id", "properties"})
public class IndexLabel extends SchemaElement {

    @JsonProperty
    private HugeType baseType;
    @JsonProperty
    private String baseValue;
    @JsonProperty
    private IndexType indexType;
    @JsonProperty
    private List<String> fields;

    @JsonCreator
    public IndexLabel(@JsonProperty("name") String name) {
        super(name);
        this.indexType = IndexType.SECONDARY;
        this.fields = new ArrayList<>();
    }

    @Override
    public String type() {
        return HugeType.INDEX_LABEL.string();
    }

    public HugeType baseType() {
        return baseType;
    }

    public IndexLabel baseType(HugeType baseType) {
        this.baseType = baseType;
        return this;
    }

    public String baseValue() {
        return baseValue;
    }

    public IndexLabel baseValue(String baseValue) {
        this.baseValue = baseValue;
        return this;
    }

    public IndexType indexType() {
        return indexType;
    }

    public IndexLabel indexType(IndexType indexType) {
        this.indexType = indexType;
        return this;
    }

    public List<String> indexFields() {
        return fields;
    }

    public IndexLabel indexFields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, baseType=%s, baseValue=%s, "
                        + "indexType=%s, fields=%s}",
                this.name,
                this.baseType,
                this.baseValue,
                this.indexType,
                this.fields);
    }

    public static class Builder {

        private IndexLabel indexLabel;
        private SchemaManager manager;

        public Builder(String name, SchemaManager manager) {
            this.indexLabel = new IndexLabel(name);
            this.manager = manager;
        }

        public IndexLabel create() {
            this.manager.addIndexLabel(this.indexLabel);
            return this.indexLabel;
        }

        public void remove() {
            this.manager.removeIndexLabel(this.indexLabel.name);
        }

        public Builder onV(String baseValue) {
            this.indexLabel.baseType = HugeType.VERTEX_LABEL;
            this.indexLabel.baseValue = baseValue;
            return this;
        }

        public Builder onE(String baseValue) {
            this.indexLabel.baseType = HugeType.EDGE_LABEL;
            this.indexLabel.baseValue = baseValue;
            return this;
        }

        public Builder by(String... indexFields) {
            for (String field : indexFields) {
                if (!this.indexLabel.fields.contains(field)) {
                    this.indexLabel.fields.add(field);
                }
            }
            return this;
        }

        public Builder secondary() {
            this.indexLabel.indexType = IndexType.SECONDARY;
            return this;
        }

        public Builder search() {
            this.indexLabel.indexType = IndexType.SEARCH;
            return this;
        }

        public Builder ifNotExist() {
            this.indexLabel.checkExist = false;
            return this;
        }
    }
}
