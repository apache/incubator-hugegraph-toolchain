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
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
        KafkaUtil.prepare();
        mockVertexPersonData();
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
        //clearServerData();
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

        Assert.assertEquals(5, vertices.size());
        Assert.assertEquals(0, edges.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(Integer.class, vertex.id().getClass());
        }
        //for (Edge edge : edges) {
        //    Assert.assertEquals(Integer.class, edge.sourceId().getClass());
        //    Assert.assertEquals(Integer.class, edge.targetId().getClass());
        //}
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

    private static void commonMockData(String[] keys, Object[][] objects, String topic)
            throws JsonProcessingException {

        Properties props = new Properties();
        props.put("bootstrap.servers", KafkaUtil.getBootStrapServers());
        props.put("key.serializer", StringDeserializer.class.getName());
        props.put("value.serializer", StringDeserializer.class.getName());
        Producer<String, String> producer = new KafkaProducer<>(props);

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
}
