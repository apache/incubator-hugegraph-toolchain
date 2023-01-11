/*
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

package org.apache.hugegraph.entity.load;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.common.Mergeable;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ElementMapping implements Mergeable {

    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private String id;

    @MergeProperty
    @JsonProperty("label")
    private String label;

    @MergeProperty
    @JsonProperty("field_mapping")
    private List<FieldMappingItem> fieldMappings;

    @MergeProperty
    @JsonProperty("value_mapping")
    private List<ValueMappingItem> valueMappings;

    @MergeProperty
    @JsonProperty("null_values")
    private NullValues nullValues;

    public Map<String, String> fieldMappingToMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (this.fieldMappings == null) {
            return map;
        }
        for (FieldMappingItem item : this.fieldMappings) {
            map.put(item.getColumnName(), item.getMappedName());
        }
        return map;
    }

    public Map<String, Map<String, Object>> valueMappingToMap() {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        if (this.valueMappings == null) {
            return map;
        }
        for (ValueMappingItem item : this.valueMappings) {
            String columnName = item.getColumnName();
            Map<String, Object> valueMap = new LinkedHashMap<>();
            List<ValueMappingItem.ValueItem> values = item.getValues();
            for (ValueMappingItem.ValueItem valueItem : values) {
                valueMap.put(valueItem.getColumnValue(),
                             valueItem.getMappedValue());
            }
            map.put(columnName, valueMap);
        }
        return map;
    }
}
