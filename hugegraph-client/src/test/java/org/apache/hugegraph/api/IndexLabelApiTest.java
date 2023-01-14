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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.hugegraph.exception.NotSupportException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.IndexType;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.apache.hugegraph.util.VersionUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class IndexLabelApiTest extends BaseApiTest {

    private static final Function<String, IndexLabel> fillIndexLabel =
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

    @Override
    @After
    public void teardown() throws Exception {
        List<Long> taskIds = new ArrayList<>();
        indexLabelAPI.list().forEach(il -> {
            taskIds.add(indexLabelAPI.delete(il.name()));
        });
        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
    }

    @Test
    public void testCreate() {
        IndexLabel indexLabel = indexLabelAPI.create(fillIndexLabel.apply("personByAge"))
                                             .indexLabel();

        Assert.assertEquals("personByAge", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("person", indexLabel.baseValue());
        Assert.assertEquals(IndexType.RANGE, indexLabel.indexType());
        List<String> fields = ImmutableList.of("age");
        Assert.assertEquals(fields.size(), indexLabel.indexFields().size());
        Assert.assertTrue(fields.containsAll(indexLabel.indexFields()));
    }

    @Test
    public void testCreateWithIndexType() {
        IndexLabel indexLabel = schema().indexLabel("personByAge")
                                        .onV("person")
                                        .by("age")
                                        .indexType(IndexType.RANGE)
                                        .build();

        Assert.assertEquals("personByAge", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("person", indexLabel.baseValue());
        Assert.assertEquals(IndexType.RANGE, indexLabel.indexType());
        List<String> fields = ImmutableList.of("age");
        Assert.assertEquals(fields.size(), indexLabel.indexFields().size());
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
            Assert.assertContains(expect, e.getMessage());
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
            Assert.assertContains(expect, e.getMessage());
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
            Assert.assertContains(expect, e.getMessage());
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
            Assert.assertContains(expect, e.getMessage());
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
    public void testCreateUniqueIndex() {
        IndexLabel indexLabel = schema().indexLabel("personByCity")
                                        .onV("person")
                                        .by("city")
                                        .unique()
                                        .create();
        Assert.assertEquals("personByCity", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("person", indexLabel.baseValue());
        Assert.assertEquals(IndexType.UNIQUE, indexLabel.indexType());
        List<String> fields = ImmutableList.of("city");
        Assert.assertEquals(fields.size(), indexLabel.indexFields().size());
        Assert.assertTrue(fields.containsAll(indexLabel.indexFields()));
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
        IndexLabel indexLabel1 = indexLabelAPI.create(fillIndexLabel.apply("personByAge"))
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
    public void testListByNames() {
        IndexLabel personByAge = fillIndexLabel.apply("personByAge");
        personByAge = indexLabelAPI.create(personByAge).indexLabel();

        IndexLabel personByCity = schema().indexLabel("personByCity")
                                          .onV("person")
                                          .by("city")
                                          .secondary()
                                          .build();
        personByCity = indexLabelAPI.create(personByCity).indexLabel();

        List<IndexLabel> indexLabels;

        indexLabels = indexLabelAPI.list(ImmutableList.of("personByAge"));
        Assert.assertEquals(1, indexLabels.size());
        assertContains(indexLabels, personByAge);

        indexLabels = indexLabelAPI.list(ImmutableList.of("personByCity"));
        Assert.assertEquals(1, indexLabels.size());
        assertContains(indexLabels, personByCity);

        indexLabels = indexLabelAPI.list(ImmutableList.of("personByAge",
                                                          "personByCity"));
        Assert.assertEquals(2, indexLabels.size());
        assertContains(indexLabels, personByAge);
        assertContains(indexLabels, personByCity);
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
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(404, () -> {
            indexLabelAPI.delete("not-exist-il");
        });
    }

    @Test
    public void testAddIndexLabelWithUserData() {
        IndexLabel personByAge = schema().indexLabel("personByAge")
                                         .onV("person")
                                         .by("age")
                                         .range()
                                         .userdata("min", 0)
                                         .userdata("max", 100)
                                         .build();
        personByAge = indexLabelAPI.create(personByAge).indexLabel();
        Assert.assertEquals(3, personByAge.userdata().size());
        Assert.assertEquals(0, personByAge.userdata().get("min"));
        Assert.assertEquals(100, personByAge.userdata().get("max"));
        String time = (String) personByAge.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        IndexLabel personByCity = schema().indexLabel("personByCity")
                                          .onV("person")
                                          .by("city")
                                          .secondary()
                                          .userdata("length", 15)
                                          .userdata("length", 18)
                                          .build();
        personByCity = indexLabelAPI.create(personByCity).indexLabel();
        // The same key user data will be overwritten
        Assert.assertEquals(2, personByCity.userdata().size());
        Assert.assertEquals(18, personByCity.userdata().get("length"));
        time = (String) personByCity.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        IndexLabel bookByName = schema().indexLabel("bookByName")
                                        .onV("book")
                                        .by("name")
                                        .secondary()
                                        .userdata("option",
                                                  ImmutableList.of("xx", "yy"))
                                        .build();
        bookByName = indexLabelAPI.create(bookByName).indexLabel();
        Assert.assertEquals(2, bookByName.userdata().size());
        Assert.assertEquals(ImmutableList.of("xx", "yy"),
                            bookByName.userdata().get("option"));
        time = (String) bookByName.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);
    }

    @Test
    public void testAddIndexLabelWithUserDataWithApiVersion49() {
        VersionUtil.Version originApiVersion = client().apiVersion();
        VersionUtil.Version apiVersion = VersionUtil.Version.of("0.49");
        client().apiVersion(apiVersion);

        IndexLabel personByAge = schema().indexLabel("personByAge")
                                         .onV("person")
                                         .by("age")
                                         .range()
                                         .build();
        personByAge = indexLabelAPI.create(personByAge).indexLabel();
        Assert.assertEquals(1, personByAge.userdata().size());

        IndexLabel personByAge1 = schema().indexLabel("personByAge1")
                                          .onV("person")
                                          .by("age")
                                          .range()
                                          .userdata("min", 0)
                                          .userdata("max", 100)
                                          .build();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            indexLabelAPI.create(personByAge1).indexLabel();
        }, (e) -> {
            String msg = "Not support userdata of index label until api " +
                         "version 0.50";
            Assert.assertContains(msg, e.getMessage());
        });

        IndexLabel personByAge2 = schema().indexLabel("personByAge2")
                                          .onV("person")
                                          .by("age")
                                          .userdata("min", 0)
                                          .build();
        Assert.assertThrows(NotSupportException.class, () -> {
            indexLabelAPI.append(personByAge2);
        }, (e) -> {
            Assert.assertContains("action append on index label",
                                  e.getMessage());
        });

        IndexLabel personByAge3 = schema().indexLabel("personByAge3")
                                          .onV("person")
                                          .by("age")
                                          .userdata("min", 0)
                                          .build();
        Assert.assertThrows(NotSupportException.class, () -> {
            indexLabelAPI.eliminate(personByAge3);
        }, (e) -> {
            Assert.assertContains("action eliminate on index label",
                                  e.getMessage());
        });

        client().apiVersion(originApiVersion);
    }

    @Test
    public void testAddIndexLabelWithRebuildFalse() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        vertex = vertexAPI.create(vertex);

        IndexLabel personByAge = schema().indexLabel("personByAge")
                                         .onV("person")
                                         .by("city")
                                         .secondary()
                                         .rebuild(false)
                                         .build();
        IndexLabel.IndexLabelWithTask il = indexLabelAPI.create(personByAge);
        IndexLabel created = il.indexLabel();
        Assert.assertEquals(personByAge.name(), created.name());
        Assert.assertEquals(personByAge.baseType(), created.baseType());
        Assert.assertEquals(personByAge.baseValue(), created.baseValue());
        Assert.assertEquals(personByAge.indexType(), created.indexType());
        Assert.assertEquals(Task.TASK_ID_NULL, il.taskId());

        Map<String, Object> properties = ImmutableMap.of("city", "Beijing");
        List<Vertex> vertices = vertexAPI.list("person", properties, 0,
                                               null, 10).results();
        Assert.assertEquals(0, vertices.size());

        long taskId = rebuildAPI.rebuild(personByAge);
        waitUntilTaskCompleted(taskId);

        vertices = vertexAPI.list("person", properties, 0, null, 10)
                            .results();
        Assert.assertEquals(1, vertices.size());
        Assert.assertEquals(vertex, vertices.get(0));
    }
}
