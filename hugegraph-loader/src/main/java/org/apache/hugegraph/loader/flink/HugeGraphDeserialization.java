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

package org.apache.hugegraph.loader.flink;

import java.util.List;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.cdc.debezium.DebeziumDeserializationSchema;

import io.debezium.data.Envelope;

public class HugeGraphDeserialization implements DebeziumDeserializationSchema<String> {

    private static final Logger LOG = Log.logger(HugeGraphDeserialization.class);

    @Override
    public void deserialize(SourceRecord sourceRecord, Collector<String> collector) {
        // Debezium can emit a tombstone after a delete. It has no graph mutation.
        if (sourceRecord.value() == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        Envelope.Operation operation = Envelope.operationFor(sourceRecord);
        String op = operation.code();
        Struct value = (Struct) sourceRecord.value();
        Struct data;
        switch (operation) {
            case DELETE:
                data = value.getStruct("before");
                break;
            case CREATE:
            case READ:
            case UPDATE:
                data = value.getStruct("after");
                break;
            default:
                throw new IllegalArgumentException(
                          "The type of `op` should be 'c' 'r' 'u' 'd' only");
        }
        result.set(Constants.CDC_DATA, row(mapper, data));
        if (operation == Envelope.Operation.UPDATE) {
            Struct before = value.getStruct("before");
            if (before == null) {
                throw new IllegalArgumentException("CDC UPDATE requires a full before image");
            }
            result.set("before", row(mapper, before));
        }
        result.put(Constants.CDC_OP, op);
        LOG.debug("Loaded CDC event: op={}, dataFields={}, beforeFields={}", op,
                  result.get(Constants.CDC_DATA).size(),
                  result.has("before") ? result.get("before").size() : 0);
        collector.collect(result.toString());
    }

    private static ObjectNode row(ObjectMapper mapper, Struct data) {
        ObjectNode rootNode = mapper.createObjectNode();
        if (data != null) {
            Schema afterSchema = data.schema();
            List<Field> afterFields = afterSchema.fields();
            for (Field field : afterFields) {
                Object afterValue = data.get(field);
                if (afterValue == null) {
                    rootNode.putNull(field.name());
                } else {
                    rootNode.put(field.name(), afterValue.toString());
                }
            }
        }

        return rootNode;
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return BasicTypeInfo.STRING_TYPE_INFO;
    }
}
