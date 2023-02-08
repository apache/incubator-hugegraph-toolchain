/*
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

package org.apache.hugegraph.structure.schema;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.IndexType;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.NotSupportException;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"properties"})
public class IndexLabel extends SchemaElement {

    @JsonProperty("base_type")
    protected HugeType baseType;
    @JsonProperty("base_value")
    protected String baseValue;
    @JsonProperty("index_type")
    protected IndexType indexType;
    @JsonProperty("fields")
    protected List<String> fields;
    @JsonProperty("rebuild")
    protected boolean rebuild = true;

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
        return this.baseType;
    }

    public String baseValue() {
        return this.baseValue;
    }

    public IndexType indexType() {
        return this.indexType;
    }

    public List<String> indexFields() {
        return this.fields;
    }

    public boolean rebuild() {
        return this.rebuild;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, baseType=%s, baseValue=%s, " +
                             "indexType=%s, fields=%s, status=%s}",
                             this.name, this.baseType, this.baseValue,
                             this.indexType, this.fields, this.status);
    }

    public IndexLabelV49 switchV49() {
        return new IndexLabelV49(this);
    }

    public IndexLabelV56 switchV56() {
        return new IndexLabelV56(this);
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

        Builder rebuild(boolean rebuild);
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

        @Override
        public Builder rebuild(boolean rebuild) {
            this.indexLabel.rebuild = rebuild;
            return this;
        }
    }

    public static class IndexLabelWithTask {

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

    @JsonIgnoreProperties({"properties", "rebuild"})
    public static class IndexLabelV56 extends IndexLabel {

        public IndexLabelV56(IndexLabel indexLabel) {
            super(indexLabel.name);
            E.checkArgument(indexLabel.rebuild,
                            "The rebuild of indexlabel must be true");
            this.id = indexLabel.id();
            this.baseType = indexLabel.baseType;
            this.baseValue = indexLabel.baseValue;
            this.indexType = indexLabel.indexType;
            this.fields = indexLabel.fields;
        }

        @Override
        public boolean rebuild() {
            throw new NotSupportException("rebuild for index label");
        }
    }

    @JsonIgnoreProperties({"properties", "user_data", "rebuild"})
    public static class IndexLabelV49 extends IndexLabel {

        public IndexLabelV49(IndexLabel indexLabel) {
            super(indexLabel.name);
            E.checkArgument(indexLabel.userdata.isEmpty(),
                            "The userdata of indexlabel must be empty");
            E.checkArgument(indexLabel.rebuild,
                            "The rebuild of indexlabel must be true");
            this.id = indexLabel.id();
            this.baseType = indexLabel.baseType;
            this.baseValue = indexLabel.baseValue;
            this.indexType = indexLabel.indexType;
            this.fields = indexLabel.fields;
        }

        @Override
        public Map<String, Object> userdata() {
            throw new NotSupportException("user data for index label");
        }
    }
}
