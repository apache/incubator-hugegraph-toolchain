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

import java.util.List;
import java.util.function.Function;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableList;

public class IndexLabelApiTest extends BaseApiTest {

    private static Function<IndexLabel, IndexLabel> fillIndexLabel =
            (indexLabel) -> {
                indexLabel.baseType(HugeType.VERTEX_LABEL);
                indexLabel.baseValue("person");
                indexLabel.indexType(IndexType.SEARCH);
                indexLabel.indexFields(ImmutableList.of("age"));
                return indexLabel;
            };

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @After
    public void teardown() throws Exception {
        indexLabelAPI.list().forEach(il -> indexLabelAPI.delete(il.name()));
    }

    @Test
    public void testCreate() {
        IndexLabel indexLabel = new IndexLabel("personByAge");
        indexLabel = indexLabelAPI.create(fillIndexLabel.apply(indexLabel));

        Assert.assertEquals("personByAge", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("person", indexLabel.baseValue());
        Assert.assertEquals(IndexType.SEARCH, indexLabel.indexType());
        List<String> fields = ImmutableList.of("age");
        Assert.assertTrue(fields.size() == indexLabel.indexFields().size());
        Assert.assertTrue(fields.containsAll(indexLabel.indexFields()));
    }

    @Test
    public void testCreateWithInvalidName() {
        Assert.assertResponse(400, () -> {
            IndexLabel indexLabel = new IndexLabel("");
            indexLabelAPI.create(fillIndexLabel.apply(indexLabel));
        });
        Assert.assertResponse(400, () -> {
            IndexLabel indexLabel = new IndexLabel(" ");
            indexLabelAPI.create(fillIndexLabel.apply(indexLabel));
        });
        Assert.assertResponse(400, () -> {
            IndexLabel indexLabel = new IndexLabel("    ");
            indexLabelAPI.create(fillIndexLabel.apply(indexLabel));
        });
    }

    @Test
    public void testCreateExistedVertexLabel() {
        IndexLabel indexLabel1 = new IndexLabel("personByAge");
        indexLabelAPI.create(fillIndexLabel.apply(indexLabel1));

        Assert.assertResponse(400, () -> {
            IndexLabel indexLabel2 = new IndexLabel("personByAge");
            indexLabelAPI.create(fillIndexLabel.apply(indexLabel2));
        });
    }

    @Test
    public void testCreateOnUndefinedSchemaLabel() {

        IndexLabel indexLabel1 = new IndexLabel("authorByAge");
        indexLabel1.baseType(HugeType.VERTEX_LABEL);
        indexLabel1.baseValue("author");
        indexLabel1.indexType(IndexType.SEARCH);
        indexLabel1.indexFields(ImmutableList.of("age"));
        Assert.assertResponse(400, () -> {
            indexLabelAPI.create(indexLabel1);
        });

        IndexLabel indexLabel2 = new IndexLabel("writeByDate");
        indexLabel2.baseType(HugeType.EDGE_LABEL);
        indexLabel2.baseValue("write");
        indexLabel2.indexType(IndexType.SECONDARY);
        indexLabel2.indexFields(ImmutableList.of("date"));
        Assert.assertResponse(400, () -> {
            indexLabelAPI.create(indexLabel2);
        });
    }

    @Test
    public void testCreateSearchIndexOnMultiProperties() {
        IndexLabel indexLabel = new IndexLabel("personByAgeAndCity");
        indexLabel.baseType(HugeType.VERTEX_LABEL);
        indexLabel.baseValue("person");
        indexLabel.indexType(IndexType.SEARCH);
        indexLabel.indexFields(ImmutableList.of("age", "city"));
        Assert.assertResponse(400, () -> {
            indexLabelAPI.create(indexLabel);
        });
    }

    @Test
    public void testCreateSearchIndexOnNotNumberProperty() {
        IndexLabel indexLabel = new IndexLabel("personByCity");
        indexLabel.baseType(HugeType.VERTEX_LABEL);
        indexLabel.baseValue("person");
        indexLabel.indexType(IndexType.SEARCH);
        indexLabel.indexFields(ImmutableList.of("city"));
        Assert.assertResponse(400, () -> {
            indexLabelAPI.create(indexLabel);
        });
    }

    @Test
    public void testGet() {
        IndexLabel indexLabel1 = new IndexLabel("personByAge");
        indexLabel1.baseType(HugeType.VERTEX_LABEL);
        indexLabel1.baseValue("person");
        indexLabel1.indexType(IndexType.SEARCH);
        indexLabel1.indexFields(ImmutableList.of("age"));

        indexLabel1 = indexLabelAPI.create(indexLabel1);

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
        Assert.assertResponse(404, () -> {
            indexLabelAPI.get("not-exist-il");
        });
    }

    @Test
    public void testList() {
        IndexLabel indexLabel1 = new IndexLabel("personByAge");
        indexLabel1 = indexLabelAPI.create(fillIndexLabel.apply(indexLabel1));

        IndexLabel indexLabel2 = new IndexLabel("personByCity");
        indexLabel2.baseType(HugeType.VERTEX_LABEL);
        indexLabel2.baseValue("person");
        indexLabel2.indexType(IndexType.SECONDARY);
        indexLabel2.indexFields(ImmutableList.of("city"));
        indexLabel2 = indexLabelAPI.create(indexLabel2);

        List<IndexLabel> indexLabels = indexLabelAPI.list();
        Assert.assertEquals(2, indexLabels.size());
        assertContains(indexLabels, indexLabel1);
        assertContains(indexLabels, indexLabel2);
    }

    @Test
    public void testDelete() {
        IndexLabel indexLabel = new IndexLabel("personByAge");
        indexLabelAPI.create(fillIndexLabel.apply(indexLabel));

        indexLabelAPI.delete("personByAge");

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            indexLabelAPI.get("personByAge");
        });
    }

    @Test
    public void testDeleteNotExist() {
        indexLabelAPI.delete("not-exist-il");
    }

    private static void assertContains(List<IndexLabel> indexLabels,
                                       IndexLabel indexLabel) {
        Assert.assertTrue(Utils.contains(indexLabels, indexLabel));
    }
}
