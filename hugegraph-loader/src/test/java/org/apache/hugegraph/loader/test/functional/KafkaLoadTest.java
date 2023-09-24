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

package org.apache.hugegraph.loader.test.functional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaLoadTest extends LoadTest {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaLoadTest.class);

    @BeforeClass
    public static void setUp() throws JsonProcessingException {
        clearServerData();
        KafkaUtil.prepareEnv();
        mockVertexPersonData();
        mockVertexSoftwareData();
        mockEdgeKnowsData();
        mockEdgeCreatedData();
        mockVertexPersonValueMapping();
        mockVertexPersonFormatText();
        mockVertexPersonFormatCsv();
    }

    @AfterClass
    public static void tearDown() {
        KafkaUtil.close();
    }

    @Before
    public void init() {
    }

    @After
    public void clear() {
        clearServerData();
    }

    @Test
    public void testCustomizedSchema() {
        String[] args = new String[]{
                "-f", configPath("kafka_customized_schema/struct.json"),
                "-s", configPath("kafka_customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

        Assert.assertEquals(7, vertices.size());
        Assert.assertEquals(6, edges.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(Integer.class, vertex.id().getClass());
        }
        for (Edge edge : edges) {
            Assert.assertEquals(Integer.class, edge.sourceId().getClass());
            Assert.assertEquals(Integer.class, edge.targetId().getClass());
        }
    }

    @Test
    public void testNumberToStringInKafkaSource() {
        String[] args = new String[]{
                "-f", configPath("kafka_number_to_string/struct.json"),
                "-s", configPath("kafka_number_to_string/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        HugeGraphLoader.main(args);
        List<Vertex> vertices = CLIENT.graph().listVertices();

        Assert.assertEquals(7, vertices.size());
        assertContains(vertices, "person",
                       "name", "marko", "age", "29", "city", "Beijing");
        assertContains(vertices, "software",
                       "name", "ripple", "lang", "java", "price", "199.67");
    }

    @Test
    public void testValueMappingInKafkaSource() {
        String[] args = new String[]{
                "-f", configPath("kafka_value_mapping/struct.json"),
                "-s", configPath("kafka_value_mapping/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, "person", "name", "marko", "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas", "age", 27, "city", "Shanghai");
    }

    @Test
    public void testKafkaFormatNotSupport() {
        String[] args = new String[]{
                "-f", configPath("kafka_format_not_support/struct.json"),
                "-s", configPath("kafka_format_not_support/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        Assert.assertThrows(SerializeException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testKafkaTextFormat() {
        String[] args = new String[]{
                "-f", configPath("kafka_format_text/struct.json"),
                "-s", configPath("kafka_format_text/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        assertContains(vertices, "person", "name", "marko", "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas", "age", 27, "city", "Shanghai");
    }

    @Test
    public void testKafkaCsvFormat() {
        String[] args = new String[]{
                "-f", configPath("kafka_format_csv/struct.json"),
                "-s", configPath("kafka_format_csv/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };

        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());

        assertContains(vertices, "person", "name", "marko", "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas", "age", 27, "city", "Shanghai");
    }

    private static void mockVertexPersonFormatCsv() {
        String topicName = "vertex-format-csv";
        Object[] objects = {
                "1,marko,29,Beijing",
                "2,vadas,27,Shanghai"
        };
        KafkaUtil.createTopic(topicName);
        commonMockTextData(objects, topicName);
    }

    private static void mockVertexPersonFormatText() {
        String topicName = "vertex-format-text";
        Object[] objects = {
                "1\tmarko\t29\tBeijing",
                "2\tvadas\t27\tShanghai"
        };
        KafkaUtil.createTopic(topicName);
        commonMockTextData(objects, topicName);
    }

    private static void mockVertexPersonValueMapping() throws JsonProcessingException {
        String topicName = "vertex-person-value-mapping";
        String[] keys = {"id", "name", "age", "city"};
        Object[][] objects = {
                {1, "marko", 29, "1"},
                {2, "vadas", 27, "2"}
        };
        KafkaUtil.createTopic(topicName);
        commonMockData(keys, objects, topicName);
    }

    private static void mockVertexPersonData() throws JsonProcessingException {
        String topicName = "vertex-person";
        String[] keys = {"id", "name", "age", "city"};
        Object[][] objects = {
                {1, "marko", 29, "Beijing"},
                {2, "vadas", 27, "HongKong"},
                {3, "josh", 32, "Beijing"},
                {4, "peter", 35, "Shanghai"},
                {5, "peter", 26, "Wu,han"}
        };
        KafkaUtil.createTopic(topicName);
        commonMockData(keys, objects, topicName);
    }

    private static void mockVertexSoftwareData() throws JsonProcessingException {
        String topicName = "vertex-software";
        String[] keys = {"id", "name", "lang", "price"};
        Object[][] objects = {
                {100, "lop", "java", 328.00},
                {200, "ripple", "java", 199.67}
        };
        KafkaUtil.createTopic(topicName);
        commonMockData(keys, objects, topicName);
    }

    private static void mockEdgeKnowsData() throws JsonProcessingException {
        String topicName = "edge-knows";
        String[] keys = {"id", "source_id", "target_id", "date", "weight"};
        Object[][] objects = {
                {1, 1, 2, "2016-01-10", 0.50},
                {2, 1, 3, "2013-02-20", 1.00}
        };
        KafkaUtil.createTopic(topicName);
        commonMockData(keys, objects, topicName);
    }

    private static void mockEdgeCreatedData() throws JsonProcessingException {
        String topicName = "edge-created";
        String[] keys = {"id", "source_id", "target_id", "date", "weight"};
        Object[][] objects = {
                {1, 1, 100, "2017-12-10", 0.40},
                {2, 3, 100, "2009-11-11", 0.40},
                {3, 3, 200, "2017-12-10", 1.00},
                {4, 4, 100, "2017-03-24", 0.20}
        };
        KafkaUtil.createTopic(topicName);
        commonMockData(keys, objects, topicName);
    }

    @NotNull
    private static Producer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", KafkaUtil.getBootStrapServers());
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        Producer<String, String> producer = new KafkaProducer<>(props);
        return producer;
    }

    private static void commonMockData(String[] keys, Object[][] objects, String topic)
            throws JsonProcessingException {

        Producer<String, String> producer = createKafkaProducer();

        for (Object[] object : objects) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], object[i]);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            String value = objectMapper.writeValueAsString(map);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);
            producer.send(record);
        }

        producer.flush();
        producer.close();
    }

    private static void commonMockTextData(Object[] objects, String topicName) {
        Producer<String, String> producer = createKafkaProducer();

        for (Object object : objects) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topicName, object.toString());
            producer.send(record);
        }

        producer.flush();
        producer.close();
    }
}
