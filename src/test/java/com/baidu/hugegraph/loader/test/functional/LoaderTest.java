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

package com.baidu.hugegraph.loader.test.functional;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.driver.TaskManager;
import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;

public class LoaderTest {

    private static final Charset GBK = Charset.forName("GBK");

    private static final String PATH_PREFIX = "src/test/resources";
    private static final String url = "http://127.0.0.1:8080";
    private static final String graph = "hugegraph";
    private static final HugeClient client = new HugeClient(url, graph);

    @BeforeClass
    public static void setUp() {
        clearFileData();
        clearServerData();
    }

    @Before
    public void init() {
        FileUtil.append(path("vertex_person.csv"), "name,age,city");
        FileUtil.append(path("vertex_software.csv"), GBK, "name,lang,price");
        FileUtil.append(path("edge_knows.csv"),
                        "source_name,target_name,date,weight");
        FileUtil.append(path("edge_created.csv"),
                        "source_name,target_name,date,weight");
    }

    @After
    public void clear() {
        clearFileData();
        clearServerData();
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(path("vertex_person.csv"));
        FileUtil.delete(path("vertex_software.csv"));
        FileUtil.delete(path("edge_knows.csv"));
        FileUtil.delete(path("edge_created.csv"));
    }

    private static void clearFileData() {
        FileUtil.clear(path("vertex_person.csv"));
        FileUtil.clear(path("vertex_software.csv"));
        FileUtil.clear(path("edge_knows.csv"));
        FileUtil.clear(path("edge_created.csv"));
    }

    private static void clearServerData() {
        SchemaManager schema = client.schema();
        GraphManager graph = client.graph();
        TaskManager task = client.task();
        // Clear edge
        graph.listEdges().forEach(e -> graph.removeEdge(e.id()));
        // Clear vertex
        graph.listVertices().forEach(v -> graph.removeVertex(v.id()));

        // Clear schema
        List<Long> taskIds = new ArrayList<>();
        schema.getIndexLabels().forEach(il -> {
            taskIds.add(schema.removeIndexLabelAsync(il.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getEdgeLabels().forEach(el -> {
            taskIds.add(schema.removeEdgeLabelAsync(el.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getVertexLabels().forEach(vl -> {
            taskIds.add(schema.removeVertexLabelAsync(vl.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getPropertyKeys().forEach(pk -> {
            schema.removePropertyKey(pk.name());
        });
    }

    /**
     * NOTE: Unsupport auto create schema
     */
    //@Test
    public void testLoadWithAutoCreateSchema() {
        String[] args = new String[]{"-f", "example/struct.json",
                                     "-g", "hugegraph",
                                     "--num-threads", "2"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<PropertyKey> propertyKeys = client.schema().getPropertyKeys();
        propertyKeys.forEach(pkey -> {
            Assert.assertEquals(DataType.TEXT, pkey.dataType());
        });

        List<Vertex> vertices = client.graph().listVertices();
        List<Edge> edges = client.graph().listEdges();

        Assert.assertEquals(7, vertices.size());
        Assert.assertEquals(6, edges.size());

        boolean interestedVertex = false;
        for (Vertex vertex : vertices) {
            Assert.assertEquals(String.class, vertex.id().getClass());
            if (((String) vertex.id()).contains("li,nary")) {
                interestedVertex = true;
                Assert.assertEquals("26", vertex.property("age"));
                Assert.assertEquals("Wu,han", vertex.property("city"));
            }
        }
        Assert.assertTrue(interestedVertex);

        boolean interestedEdge = false;
        for (Edge edge : edges) {
            Assert.assertEquals(String.class, edge.source().getClass());
            Assert.assertEquals(String.class, edge.target().getClass());
            if (((String) edge.source()).contains("marko") &&
                ((String) edge.target()).contains("vadas")) {
                interestedEdge = true;
                Assert.assertEquals("20160110", edge.property("date"));
                Assert.assertEquals("0.5", edge.property("weight"));
            }
        }
        Assert.assertTrue(interestedEdge);
    }

    @Test
    public void testLoadWithCustomizedSchema() {
        FileUtil.append(path("vertex_person.csv"),
                        "marko,29,Beijing",
                        "vadas,27,Hongkong",
                        "josh,32,Beijing",
                        "peter,35,Shanghai",
                        "\"li,nary\",26,\"Wu,han\"");

        FileUtil.append(path("vertex_software.csv"),
                        "lop,java,328",
                        "ripple,java,199");

        FileUtil.append(path("edge_knows.csv"),
                        "marko,vadas,20160110,0.5",
                        "marko,josh,20130220,1.0");

        FileUtil.append(path("edge_created.csv"),
                        "marko,lop,20171210,0.4",
                        "josh,lop,20091111,0.4",
                        "josh,ripple,20171210,1.0",
                        "peter,lop,20170324,0.2");

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        List<Edge> edges = client.graph().listEdges();

        Assert.assertEquals(7, vertices.size());
        Assert.assertEquals(6, edges.size());

        boolean interestedVertex = false;
        for (Vertex vertex : vertices) {
            Assert.assertEquals(String.class, vertex.id().getClass());
            if (((String) vertex.id()).contains("li,nary")) {
                interestedVertex = true;
                Assert.assertEquals(26, vertex.property("age"));
                Assert.assertEquals("Wu,han", vertex.property("city"));
            }
        }
        Assert.assertTrue(interestedVertex);

        boolean interestedEdge = false;
        for (Edge edge : edges) {
            Assert.assertEquals(String.class, edge.source().getClass());
            Assert.assertEquals(String.class, edge.target().getClass());
            if (((String) edge.source()).contains("marko") &&
                ((String) edge.target()).contains("vadas")) {
                interestedEdge = true;
                Assert.assertEquals("20160110", edge.property("date"));
                Assert.assertEquals(0.5, edge.property("weight"));
            }
        }
        Assert.assertTrue(interestedEdge);
    }

    @Test
    public void testVertexIdExceedLimit() {
        Integer[] array = new Integer[129];
        Arrays.fill(array, 1);
        String tooLongId = StringUtils.join(array);
        String line = FileUtil.newCSVLine(tooLongId, 29, "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testLoadWithIdExceedLimitLengthInBytes() {
        String pk = "ecommerce__color__极光银翻盖上盖+" +
                    "琥珀啡翻盖下盖+咖啡金翻盖上盖装饰片+" +
                    "香槟金主镜片+深咖啡色副镜片+琥珀>" +
                    "啡前壳+极光银后壳+浅灰电池扣+极光银电池组件+深灰天线";
        assert pk.length() < 128;
        String line = FileUtil.newCSVLine(pk, "中文", 328);
        FileUtil.append(path("vertex_software.csv"), GBK, line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};
        // Bytes encoded in utf-8 exceed 128
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexTooManyColumns() {
        String line = FileUtil.newCSVLine("marko", 29, "Beijing", "Extra");
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexTooFewColumns() {
        String line = FileUtil.newCSVLine("marko", 29);
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testUnmatchedPropertyDataType() {
        String line = FileUtil.newCSVLine("marko", "Should be number",
                                          "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexPkContainsSpecicalSymbol() {
        String line = FileUtil.newCSVLine("mar:ko!", 29, "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals(String.class, vertex.id().getClass());
        Assert.assertTrue(((String) vertex.id()).contains(":mar`:ko`!"));
        Assert.assertEquals(29, vertex.property("age"));
        Assert.assertEquals("Beijing", vertex.property("city"));
    }

    @Test
    public void testLoadWithUnmatchedEncodingCharset() {
        String line = FileUtil.newCSVLine("lop", "中文", 328);
        FileUtil.append(path("vertex_software.csv"), GBK, line);

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-g", "hugegraph",
                                     "-s", path("schema.groovy"),
                                     "--num-threads", "2",
                                     "--test-mode", "true"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals("lop", vertex.property("name"));
        Assert.assertNotEquals("中文", vertex.property("lang"));
        Assert.assertEquals(328.0, vertex.property("price"));
    }

    @Test
    public void testLoadWithMatchedEncodingCharset() {
        String line = FileUtil.newCSVLine("lop", "中文", 328);
        FileUtil.append(path("vertex_software.csv"), GBK, line);

        String[] args = new String[]{"-f", path("struct_gbk.json"),
                                     "-g", "hugegraph",
                                     "-s", path("schema.groovy"),
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals("lop", vertex.property("name"));
        Assert.assertEquals("中文", vertex.property("lang"));
        Assert.assertEquals(328.0, vertex.property("price"));
    }

    @Test
    public void testLoadWithValueListPorpertyInJsonFile() {
        String line = FileUtil.newCSVLine("marko", 29, "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        line = FileUtil.newCSVLine("lop", "中文", 328);
        FileUtil.append(path("vertex_software.csv"), GBK, line);

        line = "{\"person_name\": \"marko\", \"software_name\": \"lop\", " +
               "\"feel\": [\"so so\", \"good\", \"good\"]}";
        FileUtil.append(path("edge_use.json"), line);

        String[] args = new String[]{"-f", path("struct_edge_use.json"),
                                    "-g", "hugegraph",
                                    "-s", path("schema.groovy"),
                                    "--num-threads", "2",
                                    "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            FileUtil.delete(path("edge_use.json"));
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Edge> edges = client.graph().listEdges();
        Assert.assertEquals(1, edges.size());
        Edge edge = edges.get(0);

        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(ImmutableList.of("so so", "good", "good"),
                            edge.property("feel"));

        FileUtil.delete(path("edge_use.json"));
    }

    @Test
    public void testLoadWithValueSetPorpertyInJsonFile() {
        String line = FileUtil.newCSVLine("marko", 29, "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        line = FileUtil.newCSVLine("lop", "中文", 328);
        FileUtil.append(path("vertex_software.csv"), GBK, line);

        line = "{\"person_name\": \"marko\", \"software_name\": \"lop\", " +
                "\"time\": [\"20171210\", \"20180101\"]}";
        FileUtil.append(path("edge_use.json"), line);

        String[] args = new String[]{"-f", path("struct_edge_use.json"),
                                     "-g", "hugegraph",
                                     "-s", path("schema.groovy"),
                                     "--num-threads", "2",
                                     "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            FileUtil.delete(path("edge_use.json"));
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Edge> edges = client.graph().listEdges();
        Assert.assertEquals(1, edges.size());
        Edge edge = edges.get(0);

        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        /*
         * NOTE: Although the cardinality of the property is set in schema
         * declaration, client will deserialize it to list type in default.
         */
        Assert.assertEquals(ImmutableList.of("20171210", "20180101"),
                            edge.property("time"));

        FileUtil.delete(path("edge_use.json"));
    }

    @Test
    public void testLoadWithCustomizedNumberId() {
        FileUtil.append(path("vertex_person_number_id.csv"),
                        "1,marko,29,Beijing",
                        "2,vadas,27,Hongkong");
        FileUtil.append(path("edge_knows.csv"), "1,2,20160110,0.5");
        String[] args = new String[]{"-f", path("struct_number_id.json"),
                                     "-g", "hugegraph",
                                     "-s", path("schema_number_id.groovy"),
                                     "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            FileUtil.delete(path("vertex_person_number_id.csv"));
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        List<Edge> edges = client.graph().listEdges();
        Assert.assertEquals(1, edges.size());

        FileUtil.delete(path("vertex_person_number_id.csv"));
    }

    @Test
    public void testLoadVerticesWithJointPrimaryKeys() {
        String line = FileUtil.newCSVLine("marko", 29, "Beijing");
        FileUtil.append(path("vertex_person.csv"), line);

        String[] args = new String[]{"-f", path("struct_joint_pk.json"),
                                     "-s", path("schema_joint_pk.groovy"),
                                     "-g", "hugegraph",
                                     "--test-mode", "true"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();

        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);

        Assert.assertTrue(vertex.id().toString().contains("marko!Beijing"));
        Assert.assertEquals("person", vertex.label());
        Assert.assertEquals("marko", vertex.property("name"));
        Assert.assertEquals(29, vertex.property("age"));
        Assert.assertEquals("Beijing", vertex.property("city"));
    }

    @Test
    public void testLoadWithIgnoreLastRedundantEmptyColumn() {
        // Has a redundant seperator at the end of line
        FileUtil.append(path("vertex_person.csv"), "marko,29,Beijing,");

        String[] args = new String[]{"-f", path("struct.json"),
                                     "-s", path("schema.groovy"),
                                     "-g", "hugegraph",
                                     "--test-mode", "true"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();

        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals(3, vertex.properties().size());
    }

    @Test
    public void testLoadWithIgnoreNullValueColumns() {
        FileUtil.append(path("vertex_person.csv"),
                        "marko,NULL,null",
                        "vadas,NULL,",
                        "josh,,null");

        String[] args = new String[]{"-f", path("struct_null_value.json"),
                                     "-s", path("schema_null_value.groovy"),
                                     "-g", "hugegraph",
                                     "--test-mode", "true"};
        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }

        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(3, vertices.size());

        for (Vertex vertex : vertices) {
            Assert.assertNull(vertex.property("age"));
            Assert.assertNull(vertex.property("city"));
        }
    }

    @Test
    public void testLoadWithFileHasCommentLine() {
        FileUtil.append(path("vertex_person.csv"),
                        "# This is a comment",
                        "marko,29,Beijing",
                        "// This is also a comment",
                        "# This is still a comment",
                        "vadas,27,Hongkong");

        String[] args = new String[] {"-f", path("struct_comment_symbol.json"),
                                      "-s", path("schema_joint_pk.groovy"),
                                      "-g", "hugegraph",
                                      "--test-mode", "true"};

        try {
            HugeGraphLoader.main(args);
        } catch (Exception e) {
            Assert.fail("Should not throw exception, but throw " + e);
        }
        List<Vertex> vertices = client.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    private static String path(String fileName) {
        return Paths.get(PATH_PREFIX, fileName).toString();
    }
}
