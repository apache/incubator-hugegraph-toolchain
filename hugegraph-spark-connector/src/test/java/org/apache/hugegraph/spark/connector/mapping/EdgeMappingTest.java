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

package org.apache.hugegraph.spark.connector.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class EdgeMappingTest {

    @Test
    public void testBasicEdgeMapping() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");

        Assert.assertArrayEquals(new String[]{"id", "name", "age"}, sourceFields.toArray());
        Assert.assertArrayEquals(new String[]{"id", "ISBN", "price"}, targetFields.toArray());

        Assert.assertEquals("edge", edgeMapping.type().dataType());
        Assert.assertEquals("edge-mapping(label=buy)", edgeMapping.toString());
    }

    @Test
    public void testEdgeMappingSourceIsNull() {
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(null, targetFields);
        edgeMapping.label("buy");
        testEdgeMappingException(edgeMapping);
    }

    @Test
    public void testEdgeMappingSourceIsEmpty() {
        List<String> sourceFields = new ArrayList<>();
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");
        testEdgeMappingException(edgeMapping);
    }

    @Test
    public void testEdgeMappingTargetIsNull() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, null);
        edgeMapping.label("buy");
        testEdgeMappingException(edgeMapping);
    }

    @Test
    public void testEdgeMappingTargetIsEmpty() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = new ArrayList<>();
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");
        testEdgeMappingException(edgeMapping);
    }

    private void testEdgeMappingException(EdgeMapping edgeMapping) {
        Assert.assertThrows(IllegalArgumentException.class, edgeMapping::check);
    }

    @Test
    public void testAdvancedEdgeMapping() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");
        edgeMapping.batchSize(100);
        Assert.assertEquals(100, edgeMapping.batchSize());

        edgeMapping.selectedFields(new HashSet<>(Arrays.asList("id", "name", "lang", "price")));
        edgeMapping.ignoredFields(new HashSet<>(Arrays.asList("ISBN", "age")));

        String[] selectFields = edgeMapping.selectedFields().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"price", "name", "id", "lang"}, selectFields);
        String[] ignoredFields = edgeMapping.ignoredFields().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"ISBN", "age"}, ignoredFields);

        HashMap<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("source_name", "name");
        fieldMapping.put("target_name", "name");
        edgeMapping.mappingFields(fieldMapping);
    }

    @Test
    public void testMappingFieldValueNotExists() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");

        HashMap<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("source_name", "name");
        edgeMapping.mappingFields(fieldMapping);

        Assert.assertEquals("new_source_name", edgeMapping.mappingField("new_source_name"));
    }

    @Test
    public void testMappingFieldValueIsNull() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");

        HashMap<String, String> fieldMapping = new HashMap<>();
        // The value in field_mapping can't be null
        fieldMapping.put("source_name", null);
        edgeMapping.mappingFields(fieldMapping);

        Assert.assertThrows(IllegalArgumentException.class, edgeMapping::check);
    }

    @Test
    public void testMappingValue() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");

        Map<String, Object> cityMap = new HashMap<>();
        cityMap.put("1", "Beijing");
        cityMap.put("2", "Shanghai");
        Map<String, Object> ageMap = new HashMap<>();
        ageMap.put("young", 18);
        ageMap.put("middle", 35);

        Map<String, Map<String, Object>> valueMapping = new HashMap<>();
        valueMapping.put("city", cityMap);
        valueMapping.put("age", ageMap);

        edgeMapping.mappingValues(valueMapping);

        Assert.assertEquals("Beijing", edgeMapping.mappingValue("city", "1"));
        Assert.assertEquals("Shanghai", edgeMapping.mappingValue("city", "2"));
        Assert.assertEquals(18, edgeMapping.mappingValue("age", "young"));
        Assert.assertEquals(35, edgeMapping.mappingValue("age", "middle"));

        // not exist raw value
        Assert.assertEquals("Wuhan", edgeMapping.mappingValue("city", "Wuhan"));
        Assert.assertEquals("old", edgeMapping.mappingValue("age", "old"));
    }

    @Test
    public void testMappingValueIsNull() {
        List<String> sourceFields = Arrays.asList("id", "name", "age");
        List<String> targetFields = Arrays.asList("id", "ISBN", "price");
        EdgeMapping edgeMapping = new EdgeMapping(sourceFields, targetFields);
        edgeMapping.label("buy");

        Map<String, Object> cityMap = new HashMap<>();
        // The value in value_mapping can't be null
        cityMap.put("1", null);
        cityMap.put("2", "Shanghai");

        Map<String, Map<String, Object>> valueMapping = new HashMap<>();
        valueMapping.put("city", cityMap);
        edgeMapping.mappingValues(valueMapping);

        Assert.assertThrows(IllegalArgumentException.class, edgeMapping::check);
    }
}
