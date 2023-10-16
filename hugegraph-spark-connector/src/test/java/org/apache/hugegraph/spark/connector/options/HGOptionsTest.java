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

package org.apache.hugegraph.spark.connector.options;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class HGOptionsTest {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    @Test
    public void testOptionDefaultValue() {
        Map<String, String> requiredConfigs = new HashMap<>();
        requiredConfigs.put("data-type", "vertex");
        requiredConfigs.put("label", "person");
        requiredConfigs.put("id", "name");

        HGOptions hgOptions = new HGOptions(requiredConfigs);
        Assert.assertEquals("localhost", hgOptions.host());
        Assert.assertEquals(8080, hgOptions.port());
        Assert.assertEquals("hugegraph", hgOptions.graph());
        Assert.assertEquals("http", hgOptions.protocol());
        Assert.assertNull(hgOptions.username());
        Assert.assertNull(hgOptions.token());
        Assert.assertEquals(60, hgOptions.timeout());
        Assert.assertEquals(CPUS * 4, hgOptions.maxConnection());
        Assert.assertEquals(CPUS * 2, hgOptions.maxConnectionPerRoute());
        Assert.assertNull(hgOptions.trustStoreFile());
        Assert.assertNull(hgOptions.trustStoreToken());

        Assert.assertEquals(500, hgOptions.batchSize());
        Assert.assertEquals(",", hgOptions.delimiter());
        Assert.assertEquals(0, hgOptions.selectedFields().size());
        Assert.assertEquals(0, hgOptions.ignoredFields().size());
    }

    @Test
    public void testMissingDataType() {
        Map<String, String> configs = new HashMap<>();
        configs.put("label", "person");
        configs.put("id", "name");
        testMissingRequiredOption(configs);
    }

    @Test
    public void testMissingLabel() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "vertex");
        configs.put("id", "name");
        testMissingRequiredOption(configs);
    }

    @Test
    public void testEdgeMissingSourceNames() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "edge");
        configs.put("label", "actIn");
        testMissingRequiredOption(configs);
    }

    @Test
    public void testEdgeMissingTargetNames() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "edge");
        configs.put("label", "actIn");
        configs.put("source-name", "person,book");
        testMissingRequiredOption(configs);
    }

    private void testMissingRequiredOption(Map<String, String> configs) {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new HGOptions(configs);
        });
    }

    @Test
    public void testSelectedFields() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "vertex");
        configs.put("label", "person");
        configs.put("id", "name");
        configs.put("selected-fields", "id,name,age");

        HGOptions hgOptions = new HGOptions(configs);
        Set<String> selectedFieldSet = hgOptions.selectedFields();
        String[] selectFields = selectedFieldSet.toArray(new String[0]);

        Assert.assertArrayEquals(new String[]{"name", "id", "age"}, selectFields);
    }

    @Test
    public void testIgnoredFields() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "vertex");
        configs.put("label", "person");
        configs.put("id", "name");
        configs.put("ignored-fields", "id,name,age");

        HGOptions hgOptions = new HGOptions(configs);
        Set<String> ignoreFieldSet = hgOptions.ignoredFields();
        String[] ignoreFields = ignoreFieldSet.toArray(new String[0]);

        Assert.assertArrayEquals(new String[]{"name", "id", "age"}, ignoreFields);
    }

    @Test
    public void testSelectedAndIgnoredFields() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "vertex");
        configs.put("label", "person");
        configs.put("id", "name");
        configs.put("selected-fields", "id,name,age");
        configs.put("ignored-fields", "price,ISBN");

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new HGOptions(configs);
        });
    }

    @Test
    public void testEdgeSourceAndTargetNames() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "edge");
        configs.put("label", "actIn");
        configs.put("source-name", "person,book");
        configs.put("target-name", "movie,competition");

        HGOptions hgOptions = new HGOptions(configs);

        String[] sourceNames = hgOptions.sourceName().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"person", "book"}, sourceNames);

        String[] targetNames = hgOptions.targetName().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"movie", "competition"}, targetNames);
    }
}
