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

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyKey extends SchemaElement {

    @JsonProperty("data_type")
    private DataType dataType;
    @JsonProperty("cardinality")
    private Cardinality cardinality;

    @JsonCreator
    public PropertyKey(@JsonProperty("name") String name) {
        super(name);
        this.dataType = DataType.TEXT;
        this.cardinality = Cardinality.SINGLE;
    }

    @Override
    public String type() {
        return HugeType.PROPERTY_KEY.string();
    }

    public DataType dataType() {
        return this.dataType;
    }

    public Cardinality cardinality() {
        return this.cardinality;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, cardinality=%s, " +
                             "dataType=%s, properties=%s}",
                             this.name, this.cardinality,
                             this.dataType, this.properties);
    }

    public interface Builder extends SchemaBuilder<PropertyKey> {

        Builder asText();

        Builder asInt();

        Builder asDate();

        Builder asUuid();

        Builder asBoolean();

        Builder asByte();

        Builder asBlob();

        Builder asDouble();

        Builder asFloat();

        Builder asLong();

        Builder valueSingle();

        Builder valueList();

        Builder valueSet();

        Builder userdata(String key, Object val);

        Builder ifNotExist();
    }

    public static class BuilderImpl implements Builder {

        private PropertyKey propertyKey;
        private SchemaManager manager;

        public BuilderImpl(String name, SchemaManager manager) {
            this.propertyKey = new PropertyKey(name);
            this.manager = manager;
        }

        @Override
        public PropertyKey build() {
            return this.propertyKey;
        }

        @Override
        public PropertyKey create() {
            return this.manager.addPropertyKey(this.propertyKey);
        }

        @Override
        public PropertyKey append() {
            return this.manager.appendPropertyKey(this.propertyKey);
        }

        @Override
        public PropertyKey eliminate() {
            return this.manager.eliminatePropertyKey(this.propertyKey);
        }

        @Override
        public void remove() {
            this.manager.removePropertyKey(this.propertyKey.name);
        }

        public Builder asText() {
            this.propertyKey.dataType = DataType.TEXT;
            return this;
        }

        public Builder asInt() {
            this.propertyKey.dataType = DataType.INT;
            return this;
        }

        public Builder asDate() {
            this.propertyKey.dataType = DataType.DATE;
            return this;
        }

        public Builder asUuid() {
            this.propertyKey.dataType = DataType.UUID;
            return this;
        }

        public Builder asBoolean() {
            this.propertyKey.dataType = DataType.BOOLEAN;
            return this;
        }

        public Builder asByte() {
            this.propertyKey.dataType = DataType.BYTE;
            return this;
        }

        public Builder asBlob() {
            this.propertyKey.dataType = DataType.BLOB;
            return this;
        }

        public Builder asDouble() {
            this.propertyKey.dataType = DataType.DOUBLE;
            return this;
        }

        public Builder asFloat() {
            this.propertyKey.dataType = DataType.FLOAT;
            return this;
        }

        public Builder asLong() {
            this.propertyKey.dataType = DataType.LONG;
            return this;
        }

        public Builder valueSingle() {
            this.propertyKey.cardinality = Cardinality.SINGLE;
            return this;
        }

        public Builder valueList() {
            this.propertyKey.cardinality = Cardinality.LIST;
            return this;
        }

        public Builder valueSet() {
            this.propertyKey.cardinality = Cardinality.SET;
            return this;
        }

        @Override
        public Builder userdata(String key, Object val) {
            E.checkArgumentNotNull(key, "The user data key can't be null");
            E.checkArgumentNotNull(val, "The user data value can't be null");
            this.propertyKey.userdata.put(key, val);
            return this;
        }

        public Builder ifNotExist() {
            this.propertyKey.checkExist = false;
            return this;
        }
    }
}
