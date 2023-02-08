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

package org.apache.hugegraph.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VariablesApiTest extends BaseApiTest {

    @Override
    @Before
    public void setup() {
        clearVariables();
    }

    @Override
    @After
    public void teardown() {
        clearVariables();
    }

    public void clearVariables() {
        variablesAPI.all().keySet().forEach(variableKey -> {
            variablesAPI.remove(variableKey);
        });
    }

    @Test
    public void testSetWithStringValue() {
        variablesAPI.set("version", "0.3.2");
        Map<String, Object> variable = variablesAPI.get("version");
        assertContains(variable, "version", "0.3.2");

        variablesAPI.set("graph number", 10);
        variable = variablesAPI.get("graph number");
        assertContains(variable, "graph number", 10);

        variablesAPI.set("graph weight", 1.0);
        variable = variablesAPI.get("graph weight");
        assertContains(variable, "graph weight", 1.0);

        List<String> sList = Arrays.asList("first", "second", "third");
        variablesAPI.set("graph list", sList);
        variable = variablesAPI.get("graph list");
        assertContains(variable, "graph list", sList);

        Set<Integer> iSet = new HashSet<>();
        iSet.add(1);
        iSet.add(2);
        iSet.add(3);
        variablesAPI.set("graph set", iSet);
        variable = variablesAPI.get("graph set");
        assertContains(variable, "graph set", iSet);
    }

    @Test
    public void testSetTwice() {
        variablesAPI.set("version", 3);
        variablesAPI.set("version", "0.3.2");

        Map<String, Object> variables = variablesAPI.all();
        Assert.assertEquals(1, variables.size());
    }

    @Test
    public void testAll() {
        variablesAPI.set("version", "0.3.2");
        variablesAPI.set("superUser", "Tom");
        variablesAPI.set("userNumber", 10);

        Map<String, Object> variables = variablesAPI.all();
        Assert.assertEquals(3, variables.size());
    }

    @Test
    public void testGet() {
        variablesAPI.set("version", "0.3.2");
        Map<String, Object> variable = variablesAPI.get("version");
        assertContains(variable, "version", "0.3.2");
    }

    @Test
    public void testGetNotExist() {
        Utils.assertResponseError(404, () -> {
            variablesAPI.get("not-exist-variable");
        });
    }

    @Test
    public void testRemove() {
        variablesAPI.set("version", "0.3.2");
        variablesAPI.remove("version");
        Utils.assertResponseError(404, () -> {
            variablesAPI.get("version");
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(404, () -> {
            vertexLabelAPI.delete("not-exist-variable");
        });
    }

    private static void assertContains(Map<String, Object> variables,
                                       String key, Object value) {
        Assert.assertTrue(variables.containsKey(key));

        if (variables.get(key) instanceof Collection) {
            Assert.assertTrue(value instanceof Collection);
            Collection<?> expect = (Collection<?>) value;
            Collection<?> actual = (Collection<?>) variables.get(key);
            Assert.assertEquals(expect.size(), actual.size());
            actual.forEach(elem -> {
                Assert.assertTrue((expect.contains(elem)));
            });
        } else {
            Assert.assertEquals(variables.get(key), value);
        }
    }
}
