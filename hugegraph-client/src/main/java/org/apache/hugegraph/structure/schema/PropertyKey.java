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

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.constant.AggregateType;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyKey extends SchemaElement {

    @JsonProperty("data_type")
    private DataType dataType;
    @JsonProperty("cardinality")
    private Cardinality cardinality;
    @JsonProperty("aggregate_type")
    private AggregateType aggregateType;
    @JsonProperty("write_type")
    private WriteType writeType;

    @JsonCreator
    public PropertyKey(@JsonProperty("name") String name) {
        super(name);
        this.dataType = DataType.TEXT;
        this.cardinality = Cardinality.SINGLE;
        this.aggregateType = AggregateType.NONE;
        this.writeType = WriteType.OLTP;
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

    public AggregateType aggregateType() {
        return this.aggregateType;
    }

    public WriteType writeType() {
        return this.writeType;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, cardinality=%s, dataType=%s, " +
                             "aggregateType=%s, properties=%s, " +
                             "writeType=%s}",
                             this.name, this.cardinality, this.dataType,
                             this.aggregateType, this.properties,
                             this.writeType);
    }

    public PropertyKeyV46 switchV46() {
        return new PropertyKeyV46(this);
    }

    public PropertyKeyV58 switchV58() {
        return new PropertyKeyV58(this);
    }

    public interface Builder extends SchemaBuilder<PropertyKey> {

        Builder dataType(DataType dataType);

        Builder asText();

        Builder asInt();

        Builder asDate();

        Builder asUUID();

        Builder asBoolean();

        Builder asByte();

        Builder asBlob();

        Builder asDouble();

        Builder asFloat();

        Builder asLong();

        Builder cardinality(Cardinality cardinality);

        Builder valueSingle();

        Builder valueList();

        Builder valueSet();

        Builder aggregateType(AggregateType aggregateType);

        Builder writeType(WriteType writeType);

        Builder calcSum();

        Builder calcMax();

        Builder calcMin();

        Builder calcOld();

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

        @Override
        public Builder dataType(DataType dataType) {
            this.propertyKey.dataType = dataType;
            return this;
        }

        @Override
        public Builder asText() {
            this.propertyKey.dataType = DataType.TEXT;
            return this;
        }

        @Override
        public Builder asInt() {
            this.propertyKey.dataType = DataType.INT;
            return this;
        }

        @Override
        public Builder asDate() {
            this.propertyKey.dataType = DataType.DATE;
            return this;
        }

        @Override
        public Builder asUUID() {
            this.propertyKey.dataType = DataType.UUID;
            return this;
        }

        @Override
        public Builder asBoolean() {
            this.propertyKey.dataType = DataType.BOOLEAN;
            return this;
        }

        @Override
        public Builder asByte() {
            this.propertyKey.dataType = DataType.BYTE;
            return this;
        }

        @Override
        public Builder asBlob() {
            this.propertyKey.dataType = DataType.BLOB;
            return this;
        }

        @Override
        public Builder asDouble() {
            this.propertyKey.dataType = DataType.DOUBLE;
            return this;
        }

        @Override
        public Builder asFloat() {
            this.propertyKey.dataType = DataType.FLOAT;
            return this;
        }

        @Override
        public Builder asLong() {
            this.propertyKey.dataType = DataType.LONG;
            return this;
        }

        @Override
        public Builder cardinality(Cardinality cardinality) {
            this.propertyKey.cardinality = cardinality;
            return this;
        }

        @Override
        public Builder valueSingle() {
            this.propertyKey.cardinality = Cardinality.SINGLE;
            return this;
        }

        @Override
        public Builder valueList() {
            this.propertyKey.cardinality = Cardinality.LIST;
            return this;
        }

        @Override
        public Builder valueSet() {
            this.propertyKey.cardinality = Cardinality.SET;
            return this;
        }

        @Override
        public Builder aggregateType(AggregateType aggregateType) {
            this.propertyKey.aggregateType = aggregateType;
            return this;
        }

        @Override
        public Builder writeType(WriteType writeType) {
            this.propertyKey.writeType = writeType;
            return this;
        }

        @Override
        public Builder calcSum() {
            this.propertyKey.aggregateType = AggregateType.SUM;
            return this;
        }

        @Override
        public Builder calcMax() {
            this.propertyKey.aggregateType = AggregateType.MAX;
            return this;
        }

        @Override
        public Builder calcMin() {
            this.propertyKey.aggregateType = AggregateType.MIN;
            return this;
        }

        @Override
        public Builder calcOld() {
            this.propertyKey.aggregateType = AggregateType.OLD;
            return this;
        }

        @Override
        public Builder userdata(String key, Object val) {
            E.checkArgumentNotNull(key, "The user data key can't be null");
            E.checkArgumentNotNull(val, "The user data value can't be null");
            this.propertyKey.userdata.put(key, val);
            return this;
        }

        @Override
        public Builder ifNotExist() {
            this.propertyKey.checkExist = false;
            return this;
        }
    }

    public static class PropertyKeyWithTask {

        @JsonProperty("property_key")
        private PropertyKey propertyKey;

        @JsonProperty("task_id")
        private long taskId;

        @JsonCreator
        public PropertyKeyWithTask(@JsonProperty("property_key")
                                   PropertyKey propertyKey,
                                   @JsonProperty("task_id")
                                   long taskId) {
            this.propertyKey = propertyKey;
            this.taskId = taskId;
        }

        public PropertyKey propertyKey() {
            return this.propertyKey;
        }

        public long taskId() {
            return this.taskId;
        }
    }

    public static class PropertyKeyV46 extends SchemaElement {

        @JsonProperty("data_type")
        private DataType dataType;
        @JsonProperty("cardinality")
        private Cardinality cardinality;

        @JsonCreator
        public PropertyKeyV46(@JsonProperty("name") String name) {
            super(name);
            this.dataType = DataType.TEXT;
            this.cardinality = Cardinality.SINGLE;
        }

        private PropertyKeyV46(PropertyKey propertyKey) {
            super(propertyKey.name);
            this.dataType = propertyKey.dataType;
            this.cardinality = propertyKey.cardinality;
            this.id = propertyKey.id();
            this.properties = propertyKey.properties();
            this.userdata = propertyKey.userdata();
            this.checkExist = propertyKey.checkExist();
        }

        public DataType dataType() {
            return this.dataType;
        }

        public Cardinality cardinality() {
            return this.cardinality;
        }

        @Override
        public String toString() {
            return String.format("{name=%s, cardinality=%s, dataType=%s, " +
                                 "properties=%s}", this.name, this.cardinality,
                                 this.dataType, this.properties);
        }

        @Override
        public String type() {
            return HugeType.PROPERTY_KEY.string();
        }
    }

    public static class PropertyKeyV58 extends PropertyKeyV46 {

        @JsonProperty("aggregate_type")
        private AggregateType aggregateType;

        @JsonCreator
        public PropertyKeyV58(@JsonProperty("name") String name) {
            super(name);
            this.aggregateType = AggregateType.NONE;
        }

        private PropertyKeyV58(PropertyKey propertyKey) {
            super(propertyKey);
            this.aggregateType = propertyKey.aggregateType;
        }

        public AggregateType aggregateType() {
            return this.aggregateType;
        }

        @Override
        public String toString() {
            return String.format("{name=%s, cardinality=%s, dataType=%s, " +
                                 "aggregateType=%s, properties=%s}", this.name,
                                 this.cardinality(), this.dataType(),
                                 this.aggregateType, this.properties);
        }
    }
}
