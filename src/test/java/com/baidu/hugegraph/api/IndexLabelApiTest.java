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

package com.baidu.hugegraph.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableList;

public class IndexLabelApiTest extends BaseApiTest {

    private static Function<String, IndexLabel> fillIndexLabel =
            (name) -> schema().indexLabel(name)
                              .onV("person")
                              .by("age")
                              .range()
                              .build();

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @After
    public void teardown() throws Exception {
        List<Long> taskIds = new ArrayList<>();
        indexLabelAPI.list().forEach(il -> {
            taskIds.add(indexLabelAPI.delete(il.name()));
        });
        taskIds.forEach(taskId -> waitUntilTaskCompleted(taskId));
    }

    @Test
    public void testCreate() {
        IndexLabel indexLabel = indexLabelAPI.create(
                                fillIndexLabel.apply("personByAge"))
                                .indexLabel();

        Assert.assertEquals("personByAge", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("person", indexLabel.baseValue());
        Assert.assertEquals(IndexType.RANGE, indexLabel.indexType());
        List<String> fields = ImmutableList.of("age");
        Assert.assertTrue(fields.size() == indexLabel.indexFields().size());
        Assert.assertTrue(fields.containsAll(indexLabel.indexFields()));
    }

    @Test
    public void testCreateWithInvalidName() {
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(fillIndexLabel.apply(""));
        });
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(fillIndexLabel.apply(" "));
        });
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(fillIndexLabel.apply("    "));
        });
    }

    @Test
    public void testCreateExistedIndexLabel() {
        indexLabelAPI.create(fillIndexLabel.apply("personByAge"));

        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(fillIndexLabel.apply("personByAge"));
        });
    }

    @Test
    public void testCreateOnUndefinedSchemaLabel() {

        IndexLabel indexLabel1 = schema().indexLabel("authorByAge")
                                         .onV("author")
                                         .by("age")
                                         .range()
                                         .build();
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(indexLabel1);
        });

        IndexLabel indexLabel2 = schema().indexLabel("writeByDate")
                                         .onE("write")
                                         .by("date")
                                         .secondary()
                                         .build();
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(indexLabel2);
        });
    }

    @Test
    public void testCreateSearchIndexOnMultiProperties() {
        IndexLabel indexLabel = schema().indexLabel("personByAgeAndCity")
                                        .onV("person")
                                        .by("age", "city")
                                        .search()
                                        .build();
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(indexLabel);
        });
    }

    @Test
    public void testCreateSecondaryIndexOnMultiPropertiesOverrideExist() {
        String personByCity = "personByCity";
        String personByCityAndAge = "personByCityAndAge";
        schema().indexLabel("personByCity")
                .onV("person")
                .by("city")
                .secondary()
                .create();
        indexLabelAPI.get(personByCity);
        Assert.assertThrows(ServerException.class, () -> {
            indexLabelAPI.get(personByCityAndAge);
        }, e -> {
            String expect = String.format("index label with name '%s' does " +
                                          "not exist", personByCityAndAge);
            Assert.assertTrue(e.toString(), e.getMessage().contains(expect));
        });
        schema().indexLabel(personByCityAndAge)
                .onV("person")
                .by("city", "age")
                .secondary()
                .create();
        Assert.assertThrows(ServerException.class, () -> {
            indexLabelAPI.get(personByCity);
        }, e -> {
            String expect = String.format("index label with name '%s' does " +
                                          "not exist", personByCity);
            Assert.assertTrue(e.toString(), e.getMessage().contains(expect));
        });
        indexLabelAPI.get(personByCityAndAge);
    }

    @Test
    public void testCreateShardIndexOnMultiPropertiesOverrideExist() {
        String personByCity = "personByCity";
        String personByCityAndAge = "personByCityAndAge";
        schema().indexLabel(personByCity)
                .onV("person")
                .by("city")
                .secondary()
                .create();
        indexLabelAPI.get(personByCity);
        Assert.assertThrows(ServerException.class, () -> {
            indexLabelAPI.get(personByCityAndAge);
        }, e -> {
            String expect = String.format("index label with name '%s' does " +
                                          "not exist", personByCityAndAge);
            Assert.assertTrue(e.toString(), e.getMessage().contains(expect));
        });
        schema().indexLabel(personByCityAndAge)
                .onV("person")
                .by("city", "age")
                .shard()
                .create();
        Assert.assertThrows(ServerException.class, () -> {
            indexLabelAPI.get(personByCity);
        }, e -> {
            String expect = String.format("index label with name '%s' does " +
                                          "not exist", personByCity);
            Assert.assertTrue(e.toString(), e.getMessage().contains(expect));
        });
        indexLabelAPI.get(personByCityAndAge);
    }

    @Test
    public void testCreateRangeIndexOnNotNumberProperty() {
        IndexLabel indexLabel = schema().indexLabel("personByCity")
                                        .onV("person")
                                        .by("city")
                                        .range()
                                        .build();
        Utils.assertResponseError(400, () -> {
            indexLabelAPI.create(indexLabel);
        });
    }

    @Test
    public void testGet() {
        IndexLabel indexLabel1 = schema().indexLabel("personByAge")
                                         .onV("person")
                                         .by("age")
                                         .range()
                                         .build();

        indexLabel1 = indexLabelAPI.create(indexLabel1).indexLabel();

        IndexLabel indexLabel2 = indexLabelAPI.get("personByAge");

        Assert.assertEquals(indexLabel1.name(), indexLabel2.name());
        Assert.assertEquals(indexLabel1.baseType(), indexLabel2.baseType());
        Assert.assertEquals(indexLabel1.baseValue(), indexLabel2.baseValue());
        Assert.assertEquals(indexLabel1.indexType(), indexLabel2.indexType());
        Assert.assertEquals(indexLabel1.indexFields(),
                            indexLabel2.indexFields());
    }

    @Test
    public void testGetNotExist() {
        Utils.assertResponseError(404, () -> {
            indexLabelAPI.get("not-exist-il");
        });
    }

    @Test
    public void testList() {
        IndexLabel indexLabel1 = indexLabelAPI.create(
                                 fillIndexLabel.apply("personByAge"))
                                 .indexLabel();

        IndexLabel indexLabel2 = schema().indexLabel("personByCity")
                                         .onV("person")
                                         .by("city")
                                         .secondary()
                                         .build();
        indexLabel2 = indexLabelAPI.create(indexLabel2).indexLabel();

        List<IndexLabel> indexLabels = indexLabelAPI.list();
        Assert.assertEquals(2, indexLabels.size());
        assertContains(indexLabels, indexLabel1);
        assertContains(indexLabels, indexLabel2);
    }

    @Test
    public void testDelete() {
        String name = "personByAge";
        indexLabelAPI.create(fillIndexLabel.apply(name));

        long taskId = indexLabelAPI.delete(name);
        waitUntilTaskCompleted(taskId);

        Assert.assertThrows(ServerException.class, () -> {
            indexLabelAPI.get(name);
        }, e -> {
            String expect = String.format("index label with name '%s' does " +
                                          "not exist", name);
            Assert.assertTrue(e.toString(), e.getMessage().contains(expect));
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(404, () -> {
            indexLabelAPI.delete("not-exist-il");
        });
    }

    private static void assertContains(List<IndexLabel> indexLabels,
                                       IndexLabel indexLabel) {
        Assert.assertTrue(Utils.contains(indexLabels, indexLabel));
    }
}
