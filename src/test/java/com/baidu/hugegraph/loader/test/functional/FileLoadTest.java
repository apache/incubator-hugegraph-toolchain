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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.util.DateUtil;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;

public class FileLoadTest extends LoadTest {

    private static final Charset GBK = Charset.forName("GBK");

    private static IOUtil ioUtil;

    static {
        String path = "/profile.properties";
        // Read properties defined in maven profile
        try (InputStream is = FileLoadTest.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(is);
            String sourceType = properties.getProperty("source_type");
            String storePath = properties.getProperty("store_path");
            if (sourceType.equals("file")) {
                ioUtil = new FileUtil(storePath);
            } else {
                assert sourceType.equals("hdfs");
                ioUtil = new HDFSUtil(storePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                      "Failed to read properties defined in maven profile", e);
        }
    }

    @BeforeClass
    public static void setUp() {
        clearFileData();
        clearServerData();
    }

    @AfterClass
    public static void tearDown() {
        ioUtil.close();
    }

    @Before
    public void init() {
    }

    @After
    public void clear() {
        clearFileData();
        clearServerData();
    }

    private static void clearFileData() {
        ioUtil.delete();
    }

    /**
     * NOTE: Unsupport auto create schema
     */
    //@Test
    public void testAutoCreateSchema() {
        String[] args = new String[]{
                "-f", "example/struct.json",
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2"
        };
        HugeGraphLoader.main(args);

        List<PropertyKey> propertyKeys = CLIENT.schema().getPropertyKeys();
        propertyKeys.forEach(pkey -> {
            Assert.assertEquals(DataType.TEXT, pkey.dataType());
        });

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

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
            Assert.assertEquals(String.class, edge.sourceId().getClass());
            Assert.assertEquals(String.class, edge.targetId().getClass());
            if (((String) edge.sourceId()).contains("marko") &&
                ((String) edge.targetId()).contains("vadas")) {
                interestedEdge = true;
                Assert.assertEquals("20160110", edge.property("date"));
                Assert.assertEquals("0.5", edge.property("weight"));
            }
        }
        Assert.assertTrue(interestedEdge);
    }

    @Test
    public void testCustomizedSchema() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     "lop,java,328",
                     "ripple,java,199");
        ioUtil.write("edge_knows.csv",
                     "source_name,target_name,date,weight",
                     "marko,vadas,20160110,0.5",
                     "marko,josh,20130220,1.0");
        ioUtil.write("edge_created.csv",
                     "source_name,target_name,date,weight",
                     "marko,lop,20171210,0.4",
                     "josh,lop,20091111,0.4",
                     "josh,ripple,20171210,1.0",
                     "peter,lop,20170324,0.2");

        String[] args = new String[]{
                "-f", configPath("customized_schema/struct.json"),
                "-s", configPath("customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

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
            Assert.assertEquals(String.class, edge.sourceId().getClass());
            Assert.assertEquals(String.class, edge.targetId().getClass());
            if (((String) edge.sourceId()).contains("marko") &&
                ((String) edge.targetId()).contains("vadas")) {
                interestedEdge = true;
                Assert.assertEquals("20160110", edge.property("date"));
                Assert.assertEquals(0.5, edge.property("weight"));
            }
        }
        Assert.assertTrue(interestedEdge);
    }

    @Test
    public void testNoSchemaFile() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("no_schema_file/struct.json"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(ServerException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexIdExceedLimit() {
        Integer[] array = new Integer[129];
        Arrays.fill(array, 1);
        String tooLongId = StringUtils.join(array);
        String line = StringUtils.join(tooLongId, 29, "Beijing");
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     line);

        String[] args = new String[]{
                "-f", configPath("vertex_id_exceed_limit/struct.json"),
                "-s", configPath("vertex_id_exceed_limit/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexIdExceedLimitInBytes() {
        String pk = "ecommerce__color__极光银翻盖上盖+" +
                    "琥珀啡翻盖下盖+咖啡金翻盖上盖装饰片+" +
                    "香槟金主镜片+深咖啡色副镜片+琥珀>" +
                    "啡前壳+极光银后壳+浅灰电池扣+极光银电池组件+深灰天线";
        Assert.assertTrue(pk.length() < 128);
        String line = StringUtils.join(new String[]{pk, "中文", "328"}, ",");
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     line);

        String[] args = new String[]{
                "-f", configPath("vertex_id_exceed_limit_in_bytes/struct.json"),
                "-s", configPath("vertex_id_exceed_limit_in_bytes/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        // Bytes encoded in utf-8 exceed 128
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testTooManyColumns() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing,Extra");

        String[] args = new String[]{
                "-f", configPath("too_many_columns/struct.json"),
                "-s", configPath("too_many_columns/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testUnmatchedPropertyDataType() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,Should be number,Beijing");

        String[] args = new String[]{
                "-f", configPath("unmatched_property_datatype/struct.json"),
                "-s", configPath("unmatched_property_datatype/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testVertexPkContainsSpecicalSymbol() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "mar:ko!,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("vertex_pk_contains_special_symbol/struct.json"),
                "-s", configPath("vertex_pk_contains_special_symbol/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals(String.class, vertex.id().getClass());
        Assert.assertTrue(((String) vertex.id()).contains(":mar`:ko`!"));
        Assert.assertEquals(29, vertex.property("age"));
        Assert.assertEquals("Beijing", vertex.property("city"));
    }

    @Test
    public void testUnmatchedEncodingCharset() {
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     "lop,中文,328");

        String[] args = new String[]{
                "-f", configPath("unmatched_encoding_charset/struct.json"),
                "-s", configPath("unmatched_encoding_charset/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals("lop", vertex.property("name"));
        Assert.assertNotEquals("中文", vertex.property("lang"));
        Assert.assertEquals(328.0, vertex.property("price"));
    }

    @Test
    public void testMatchedEncodingCharset() {
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     "lop,中文,328");

        String[] args = new String[]{
                "-f", configPath("matched_encoding_charset/struct.json"),
                "-s", configPath("matched_encoding_charset/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals("lop", vertex.property("name"));
        Assert.assertEquals("中文", vertex.property("lang"));
        Assert.assertEquals(328.0, vertex.property("price"));
    }

    @Test
    public void testCustomizedDelimiterInCsvFile() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko\t29\tBeijing");

        String[] args = new String[]{
                "-f", configPath("customized_delimiter_in_csv_file/struct.json"),
                "-s", configPath("customized_delimiter_in_csv_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    /**
     * TODO: the order of collection's maybe change
     * (such as time:["2019-05-02 13:12:44","2008-05-02 13:12:44"])
     */
    @Test
    public void testValueListPropertyInJsonFile() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing");
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     "lop,中文,328");
        ioUtil.write("edge_use.json",
                     "{\"person_name\": \"marko\", \"software_name\": " +
                     "\"lop\", \"feel\": [\"so so\", \"good\", \"good\"]}");

        String[] args = new String[]{
                "-f", configPath("value_list_property_in_json_file/struct.json"),
                "-s", configPath("value_list_property_in_json_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Edge> edges = CLIENT.graph().listEdges();
        Assert.assertEquals(1, edges.size());
        Edge edge = edges.get(0);

        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(ImmutableList.of("so so", "good", "good"),
                            edge.property("feel"));
    }

    @Test
    public void testValueListPropertyInTextFile()
                throws java.text.ParseException {
        ioUtil.write("vertex_person.txt", "jin\t29\tBeijing");
        ioUtil.write("vertex_software.txt", "tom\tChinese\t328");
        ioUtil.write("edge_use.txt",
                     "jin\ttom\t[4,1,5,6]\t[2019-05-02,2008-05-02]");

        String[] args = new String[]{
                "-f", configPath("value_list_property_in_text_file/struct.json"),
                "-s", configPath("value_list_property_in_text_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Edge> edges = CLIENT.graph().listEdges();
        Assert.assertEquals(1, edges.size());
        Edge edge = edges.get(0);

        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(ImmutableList.of(4, 1, 5, 6),
                            edge.property("feel"));
        List<Long> expectedTimes = ImmutableList.of(
                DateUtil.parse("2019-05-02", "yyyy-MM-dd").getTime(),
                DateUtil.parse("2008-05-02", "yyyy-MM-dd").getTime()
        );
        Assert.assertEquals(expectedTimes, edge.property("time"));
    }

    @Test
    public void testValueSetPropertyInTextFile()
                throws java.text.ParseException {
        ioUtil.write("vertex_person.txt", "jin\t29\tBeijing");
        ioUtil.write("vertex_software.txt", "tom\tChinese\t328");
        ioUtil.write("edge_use.txt",
                     "jin\ttom\t[4,1,5,6]\t[2019-05-02,2008-05-02]");

        String[] args = new String[]{
                "-f", configPath("value_set_property_in_text_file/struct.json"),
                "-s", configPath("value_set_property_in_text_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Edge> edges = CLIENT.graph().listEdges();
        Assert.assertEquals(1, edges.size());
        Edge edge = edges.get(0);

        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(ImmutableList.of(4, 1, 5, 6),
                            edge.property("feel"));
        Assert.assertEquals(ArrayList.class, edge.property("time").getClass());
        List<?> list = (List<?>) edge.property("time");
        Assert.assertTrue(list.contains(DateUtil.parse(
                                        "2019-05-02", "yyyy-MM-dd").getTime()));
        Assert.assertTrue(list.contains(DateUtil.parse(
                                        "2008-05-02", "yyyy-MM-dd").getTime()));
    }

    @Test
    public void testValueListPropertyInTextFileWithElemDelimiter() {
        ioUtil.write("vertex_person.txt",
                     "marko\t29\t[Beijing;Hongkong;Wuhan]");

        String[] args = new String[]{
                "-f", configPath(
                "value_list_property_in_text_file_with_elem_delimiter/struct.json"),
                "-s", configPath(
                "value_list_property_in_text_file_with_elem_delimiter/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);

        Assert.assertEquals("marko", vertex.property("name"));
        Assert.assertEquals(29, vertex.property("age"));
        Assert.assertEquals(ImmutableList.of("Beijing", "Hongkong", "Wuhan"),
                            vertex.property("city"));
    }

    @Test
    public void testValueListPropertyInTextFileWithSymbols() {
        ioUtil.write("vertex_person.txt",
                     "marko\t29\t<Beijing,Hongkong,Wuhan}");
        ioUtil.write("vertex_software.txt",
                     "lop\tjava\t,[128,228,328],");

        String[] args = new String[]{
                "-f", configPath(
                "value_list_property_in_text_file_with_symbols/struct.json"),
                "-s", configPath(
                "value_list_property_in_text_file_with_symbols/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        assertContains(vertices, "person", "name", "marko", "age", 29,
                       "city", ImmutableList.of("Beijing", "Hongkong", "Wuhan"));
        assertContains(vertices, "software", "name", "lop", "lang", "java",
                       "price", ImmutableList.of(128.0, 228.0, 328.0));
    }

    @Test
    public void testValueListPropertyInCSVFileWithSameDelimiter() {
        ioUtil.write("vertex_person.csv",
                     "marko,29,[Beijing,Hongkong,Wuhan]");

        String[] args = new String[]{
                "-f", configPath(
                "value_list_property_in_csv_file_with_same_delimiter/struct.json"),
                "-s", configPath(
                "value_list_property_in_csv_file_with_same_delimiter/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testValueSetPorpertyInJsonFile() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing");
        ioUtil.write("vertex_software.csv", GBK,
                     "name,lang,price",
                     "lop,中文,328");
        ioUtil.write("edge_use.json",
                     "{\"person_name\": \"marko\", \"software_name\": " +
                     "\"lop\", \"time\": [\"20171210\", \"20180101\"]}");

        String[] args = new String[]{
                "-f", configPath("value_set_property_in_json_file/struct.json"),
                "-s", configPath("value_set_property_in_json_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Edge> edges = CLIENT.graph().listEdges();
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
    }

    @Test
    public void testValidBooleanProperty() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,isMale",
                     // trim space
                     "marko,29,Beijing, true",
                     "vadas,27,Hongkong,True ",
                     "jack,25,Beijing,1",
                     "tom,30,Beijing,yes",
                     "cindy,26,Beijing, False ",
                     "mary,31,Shanghai,FALSE  ",
                     "lucy,22,Beijing,0",
                     "lindy,23,Beijing,no");

        String[] args = new String[]{
                "-f", configPath("value_boolean_property_in_file/struct.json"),
                "-s", configPath("value_boolean_property_in_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(8, vertices.size());
    }

    @Test
    public void testInvalidBooleanProperty() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,isMale",
                     "marko,29,Beijing,NotBoolean",
                     "vadas,27,Hongkong,666",
                     "tom,30,Beijing,T R U E");

        String[] args = new String[]{
                "-f", configPath("value_boolean_property_in_file/struct.json"),
                "-s", configPath("value_boolean_property_in_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            HugeGraphLoader.main(args);
        });

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(0, vertices.size());
    }

    @Test
    public void testValidUUIDProperty() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,no",
                     "marko,29,Beijing,91b8dedb-bfb2-49be-a53a-1180338dfc7e",
                     "vadas,27,Hongkong,5bfde4ca-4e51-4e92-91cd-047becf0fd39");

        String[] args = new String[]{
                "-f", configPath("value_uuid_property_in_file/struct.json"),
                "-s", configPath("value_uuid_property_in_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testInvalidUUIDProperty() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,no",
                     "marko,29,Beijing,invalid uuid",
                     "vadas,27,Hongkong,5bfde4ca_4e51+4e92-91cd-047becf0fd39");

        String[] args = new String[]{
                "-f", configPath("value_uuid_property_in_file/struct.json"),
                "-s", configPath("value_uuid_property_in_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            HugeGraphLoader.main(args);
        });

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(0, vertices.size());
    }

    @Test
    public void testCustomizedNumberId() {
        ioUtil.write("vertex_person_number_id.csv",
                     "1,marko,29,Beijing",
                     "2,vadas,27,Hongkong");
        ioUtil.write("edge_knows.csv", "1,2,20160110,0.5");

        String[] args = new String[]{
                "-f", configPath("customized_number_id/struct.json"),
                "-s", configPath("customized_number_id/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        List<Edge> edges = CLIENT.graph().listEdges();
        Assert.assertEquals(1, edges.size());
    }

    @Test
    public void testCustomizedLongId() {
        ioUtil.write("vertex_person_number_id.csv",
                     // trim space
                     " 11620311015184296736,marko,29,Beijing",
                     "11620311015184296737 ,vadas,27,Hongkong",
                     "-1, josh,30,Wuhan",
                     // unsigned long max value, will be parsed to -1
                     "18446744073709551615,lop,31,HongKong");

        String[] args = new String[]{
                "-f", configPath("customized_long_id/struct.json"),
                "-s", configPath("customized_long_id/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(3, vertices.size());
    }

    @Test
    public void testLongProperty() {
        ioUtil.write("vertex_long_property.csv",
                     // trim space
                     "marko,29,Beijing, 11620311015184296736",
                     "vadas,27,Hongkong,11620311015184296737 ",
                     "josh,30,Wuhan,-1",
                     // unsigned long max value, will be parsed to -1
                     "lop,31,HongKong,18446744073709551615");

        String[] args = new String[]{
                "-f", configPath("long_property/struct.json"),
                "-s", configPath("long_property/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(4, vertices.size());
    }

    @Test
    public void testVertexJointPrimaryKeys() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("vertex_joint_pks/struct.json"),
                "-s", configPath("vertex_joint_pks/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();

        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);

        Assert.assertTrue(vertex.id().toString().contains("marko!Beijing"));
        Assert.assertEquals("person", vertex.label());
        Assert.assertEquals("marko", vertex.property("name"));
        Assert.assertEquals(29, vertex.property("age"));
        Assert.assertEquals("Beijing", vertex.property("city"));
    }

    @Test
    public void testSelectedFields() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,redundant",
                     "marko,29,Beijing,value1",
                     "vadas,27,Hongkong,value2");

        String[] args = new String[]{
                "-f", configPath("selected_fields/struct.json"),
                "-s", configPath("selected_fields/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(3, vertex.properties().size());
            Assert.assertFalse(vertex.properties().containsKey("redundant"));
        }
    }

    @Test
    public void testIgnoredFields() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,redundant",
                     "marko,29,Beijing,value1",
                     "vadas,27,Hongkong,value2");

        String[] args = new String[]{
                "-f", configPath("ignored_fields/struct.json"),
                "-s", configPath("ignored_fields/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(3, vertex.properties().size());
            Assert.assertFalse(vertex.properties().containsKey("redundant"));
        }
    }

    @Test
    public void testSelectedAndIgnoredFields() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city,redundant",
                     "marko,29,Beijing,value1",
                     "vadas,27,Hongkong,value2");

        String[] args = new String[]{
                "-f", configPath("selected_and_ignored_fields/struct.json"),
                "-s", configPath("selected_and_ignored_fields/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testIgnoreTailRedundantEmptyColumn() {
        // Has many redundant seperator at the tail of line
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing,,,,");

        String[] args = new String[]{
                "-f", configPath("ignore_tail_redudant_empty_column/struct.json"),
                "-s", configPath("ignore_tail_redudant_empty_column/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(1, vertices.size());
        Vertex vertex = vertices.get(0);
        Assert.assertEquals(3, vertex.properties().size());
    }

    @Test
    public void testFillMissingColumnWithEmpty() {
        ioUtil.write("vertex_person.text",
                     "name|age|city",
                     "marko|29|",
                     "vadas|",
                     "josh");

        String[] args = new String[]{
                "-f", configPath("fill_missing_column_with_empty/struct.json"),
                "-s", configPath("fill_missing_column_with_empty/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(3, vertices.size());
    }

    @Test
    public void testIgnoreNullValueColumns() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,NULL,null",
                     "vadas,NULL,",
                     "josh,,null");

        String[] args = new String[]{
                "-f", configPath("ignore_null_value_columns/struct.json"),
                "-s", configPath("ignore_null_value_columns/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(3, vertices.size());

        for (Vertex vertex : vertices) {
            Assert.assertNull(vertex.property("age"));
            Assert.assertNull(vertex.property("city"));
        }
    }

    @Test
    public void testMappingIgnoreNullValueColumns() {
        ioUtil.write("vertex_person.csv",
                     "姓名,年龄,城市",
                     "marko,NULL,--",
                     "vadas,-,Hongkong",
                     "josh,30,null");

        String[] args = new String[]{
                "-f", configPath("mapping_ignore_null_value_columns/struct.json"),
                "-s", configPath("mapping_ignore_null_value_columns/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(3, vertices.size());

        for (Vertex vertex : vertices) {
            if (vertex.property("name").equals("marko")) {
                Assert.assertNull(vertex.property("age"));
                Assert.assertNull(vertex.property("city"));
            } else if (vertex.property("name").equals("vadas")) {
                Assert.assertNull(vertex.property("age"));
                Assert.assertEquals("Hongkong", vertex.property("city"));
            } else if (vertex.property("name").equals("josh")) {
                Assert.assertEquals(30, vertex.property("age"));
                Assert.assertNull(vertex.property("city"));
            }
        }
    }

    @Test
    public void testFileNoHeader() {
        ioUtil.write("vertex_person.csv",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("file_no_header/struct.json"),
                "-s", configPath("file_no_header/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testMultiFilesHaveHeader() {
        ioUtil.write("vertex_dir/vertex_person_1.csv",
                     "name,age,city",
                     "marko,29,Beijing");
        ioUtil.write("vertex_dir/vertex_person_2.csv",
                     "name,age,city",
                     "vadas,27,Hongkong");

        String[] args = new String[]{
                "-f", configPath("multi_files_have_header/struct.json"),
                "-s", configPath("multi_files_have_header/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testFileHasEmptyLine() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,#Beijing",
                     "",
                     "vadas,27,//Hongkong");

        String[] args = new String[]{
                "-f", configPath("file_has_empty_line/struct.json"),
                "-s", configPath("file_has_empty_line/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testFileHasSkippedLineRegex() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "# This is a comment",
                     "marko,29,#Beijing",
                     "// This is also a comment",
                     "# This is still a comment",
                     "vadas,27,//Hongkong");

        String[] args = new String[]{
                "-f", configPath("file_has_skipped_line_regex/struct.json"),
                "-s", configPath("file_has_skipped_line_regex/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testDirHasNoFile() {
        ioUtil.mkdirs("vertex_dir");
        String[] args = new String[]{
                "-f", configPath("dir_has_no_file/struct.json"),
                "-s", configPath("dir_has_no_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(0, vertices.size());
    }

    @Test
    public void testEmptyFileWithHeader() {
        ioUtil.write("vertex_person.csv");

        String[] args = new String[]{
                "-f", configPath("empty_file_with_header/struct.json"),
                "-s", configPath("empty_file_with_header/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(0, vertices.size());
    }

    @Test
    public void testEmptyFileWithoutHeader() {
        ioUtil.write("vertex_person.csv");

        String[] args = new String[]{
                "-f", configPath("empty_file_without_header/struct.json"),
                "-s", configPath("empty_file_without_header/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };

        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testDirHasMultiFiles() {
        ioUtil.write("vertex_dir/vertex_person1.csv",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing");
        ioUtil.write("vertex_dir/vertex_person2.csv",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");
        ioUtil.write("vertex_dir/vertex_person3.csv");

        String[] args = new String[]{
                "-f", configPath("dir_has_multi_files/struct.json"),
                "-s", configPath("dir_has_multi_files/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }

    @Test
    public void testMatchedDatePropertyAndFormat() {
        ioUtil.write("vertex_person_birth_date.csv",
                     "marko,1992-10-01,Beijing",
                     "vadas,2000-01-01,Hongkong");

        // DateFormat is yyyy-MM-dd
        String[] args = new String[]{
                "-f", configPath("matched_date_property_format/struct.json"),
                "-s", configPath("matched_date_property_format/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testUnMatchedDatePropertyAndFormat() {
        ioUtil.write("vertex_person_birth_date.csv",
                     "marko,1992/10/01,Beijing",
                     "vadas,2000/01/01,Hongkong");

        // DateFormat is yyyy-MM-dd
        String[] args = new String[]{
                "-f", configPath("unmatched_date_property_format/struct.json"),
                "-s", configPath("unmatched_date_property_format/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        Assert.assertThrows(ParseException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testValueMapping() throws java.text.ParseException {
        /*
         * "age": {"1": 25, "2": 30}
         * "birth": {"1": "1994-01-01", "2": "1989-01-01"}
         * "city": "1": "Beijing", "2": "Shanghai"
         */
        ioUtil.write("vertex_person.csv",
                     "marko,1,1,1",
                     "vadas,2,2,2");

        String[] args = new String[]{
                "-f", configPath("value_mapping/struct.json"),
                "-s", configPath("value_mapping/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        // TODO: Fix date property be saved as long in client
        assertContains(vertices, "person", "name", "marko", "age", 25,
                       "birth",
                       DateUtil.parse("1994-01-01", "yyyy-MM-dd").getTime(),
                       "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas", "age", 30,
                       "birth",
                       DateUtil.parse("1989-01-01", "yyyy-MM-dd").getTime(),
                       "city", "Shanghai");
    }

    @Test
    public void testPkValueMapping() {
        /*
         * "1": "marko"
         * "2": "vadas"
         */
        ioUtil.write("vertex_person.csv",
                     "1,29,Beijing",
                     "2,27,Shanghai");

        String[] args = new String[]{
                "-f", configPath("pk_value_mapping/struct.json"),
                "-s", configPath("pk_value_mapping/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        assertContains(vertices, "person", "name", "marko", "age", 29,
                       "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas", "age", 27,
                       "city", "Shanghai");
    }

    @Test
    public void testSourceTargetValueMapping() {
        ioUtil.write("vertex_person.csv",
                     "id,name,age,city",
                     "p1,marko,29,Beijing",
                     "p2,vadas,27,Hongkong");
        ioUtil.write("vertex_software.csv",
                     "id,name,lang,price",
                     "s1,lop,java,328",
                     "s2,ripple,java,199");
        ioUtil.write("edge_created.csv",
                     "source_id,target_id,weight",
                     "p1,s1,0.8",
                     "p2,s2,0.6");

        String[] args = new String[]{
                "-f", configPath("source_target_value_mapping/struct.json"),
                "-s", configPath("source_target_value_mapping/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

        Assert.assertEquals(4, vertices.size());
        Assert.assertEquals(2, edges.size());

        assertContains(edges, "created", "person_marko", "software_lop",
                       "person", "software", "weight", 0.8);
        assertContains(edges, "created", "person_vadas", "software_ripple",
                       "person", "software", "weight", 0.6);
    }

    @Test
    public void testValueMappingInJsonFile() {
        // 1(Integer) and "1"(String) are both mapping
        ioUtil.write("vertex_person.json",
                     "{\"name\": \"marko\", \"age\": 29, \"city\": 1}",
                     "{\"name\": \"vadas\", \"age\": 27, \"city\": \"1\"}");

        String[] args = new String[]{
                "-f", configPath("value_mapping_in_json_file/struct.json"),
                "-s", configPath("value_mapping_in_json_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas",
                       "age", 27, "city", "Beijing");
    }

    @Test
    public void testFilterFileBySuffix() {
        // Allowed file suffix is [".csv"]
        ioUtil.write("vertex_person.dat",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong");

        String[] args = new String[]{
                "-f", configPath("filter_file_by_suffix/struct.json"),
                "-s", configPath("filter_file_by_suffix/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testFilterPathBySuffix() {
        ioUtil.write("vertex_dir/vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong");
        ioUtil.write("vertex_dir/vertex_person.dat",
                     "name,age,city",
                     "marko1,29,Beijing",
                     "vadas1,27,Hongkong");

        String[] args = new String[]{
                "-f", configPath("filter_path_by_suffix/struct.json"),
                "-s", configPath("filter_path_by_suffix/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
    }

    @Test
    public void testGZipCompressFile() {
        ioUtil.write("vertex_person.gz", Compression.GZIP,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("gzip_compress_file/struct.json"),
                "-s", configPath("gzip_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testBZ2CompressFile() {
        ioUtil.write("vertex_person.bz2", Compression.BZ2,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("bz2_compress_file/struct.json"),
                "-s", configPath("bz2_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testXZCompressFile() {
        ioUtil.write("vertex_person.xz", Compression.XZ,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("xz_compress_file/struct.json"),
                "-s", configPath("xz_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testLZMACompressFile() {
        ioUtil.write("vertex_person.lzma", Compression.LZMA,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("lzma_compress_file/struct.json"),
                "-s", configPath("lzma_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testPack200CompressFile() {
        ioUtil.write("vertex_person.pack", Compression.PACK200,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("pack200_compress_file/struct.json"),
                "-s", configPath("pack200_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    /**
     * Didn't find a way to generate the compression file using code
     */
    //@Test
    public void testSnappyRawCompressFile() {
        ioUtil.write("vertex_person.snappy", Compression.SNAPPY_RAW,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("snappy_raw_compress_file/struct.json"),
                "-s", configPath("snappy_raw_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testSnappyFramedCompressFile() {
        ioUtil.write("vertex_person.snappy", Compression.SNAPPY_FRAMED,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("snappy_framed_compress_file/struct.json"),
                "-s", configPath("snappy_framed_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    /**
     * Didn't find a way to generate the compression file using code
     */
    //@Test
    public void testZCompressFile() {
        ioUtil.write("vertex_person.z", Compression.Z,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("z_compress_file/struct.json"),
                "-s", configPath("z_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testDeflateCompressFile() {
        ioUtil.write("vertex_person.deflate", Compression.DEFLATE,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("deflate_compress_file/struct.json"),
                "-s", configPath("deflate_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testLZ4BlockCompressFile() {
        ioUtil.write("vertex_person.lz4", Compression.LZ4_BLOCK,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("lz4_block_compress_file/struct.json"),
                "-s", configPath("lz4_block_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testLZ4FramedCompressFile() {
        ioUtil.write("vertex_person.lz4", Compression.LZ4_FRAMED,
                     "name,age,city",
                     "marko,29,Beijing");

        String[] args = new String[]{
                "-f", configPath("lz4_framed_compress_file/struct.json"),
                "-s", configPath("lz4_framed_compress_file/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
    }

    @Test
    public void testParserNotThrowException() {
        // Here are 2 parse errors, and expect no exception thrown
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "p1,marko,22,Beijing",
                     "tom,24,Hongkong",
                     "jerry,18");

        String[] args = new String[]{
                "-f", configPath("too_few_columns/struct.json"),
                "-s", configPath("too_few_columns/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--max-parse-errors", "3"
        };
        HugeGraphLoader.main(args);
    }
}
