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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaLoadTest extends LoadTest {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaLoadTest.class);

    private static final DockerImageName KAFKA_DOCKER_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

    private static KafkaContainer kafkaContainer;

    private static String bootStrapServers;

    private static AdminClient adminClient;

    public static final String DEFAULT_TOPIC_NAME = "test_topic";

    @BeforeClass
    public static void setUp() throws JsonProcessingException {
        clearServerData();

        startService();
        createAdminClient();
        createTopic();
        mockData();
    }

    @AfterClass
    public static void tearDown() {
        adminClient.close();
        kafkaContainer.close();
    }

    @Test
    public void testKafkaEnv() throws ExecutionException, InterruptedException {
        listTopics();

        //testConsumer();
    }

    @Test
    public void testCustomizedSchema() {

        String[] args = new String[]{
                "-f", configPath("kafka_customized_schema/struct.json"),
                "-s", configPath("kafka_customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT)
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

    private static void testConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                       StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                       StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                       "earliest");

        Consumer<String, String> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Collections.singletonList(DEFAULT_TOPIC_NAME));

        AtomicInteger cnt = new AtomicInteger();
        while (true) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(100));

            records.forEach(record -> {
                System.out.printf("Consumed record with key %s and value %s%n", record.key(),
                                  record.value());
                cnt.getAndIncrement();
            });
            if (cnt.get() >= 5) {
                break;
            }
        }
    }

    private static void listTopics() throws InterruptedException, ExecutionException {
        ListTopicsResult listTopicsResult = adminClient.listTopics();
        Set<String> topicNames = listTopicsResult.names().get();

        for (String topicName : topicNames) {
            System.out.println(topicName);
        }
    }

    private static void startService() {
        kafkaContainer = new KafkaContainer(KAFKA_DOCKER_IMAGE);
        kafkaContainer.setPortBindings(Arrays.asList("9092:9092", "9093:9093"));
        kafkaContainer.start();
        kafkaContainer.waitingFor(Wait.defaultWaitStrategy());
        bootStrapServers = kafkaContainer.getBootstrapServers();
        LOG.info("Kafka Container run successfully, bootstrapServer: {}.", bootStrapServers);
        System.out.println(bootStrapServers);
    }

    private static void createAdminClient() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        adminClient = AdminClient.create(properties);
    }

    private static void createTopic() {
        int numPartitions = 1;
        int replicationFactor = 1;
        NewTopic newTopic =
                new NewTopic(DEFAULT_TOPIC_NAME, numPartitions, (short) replicationFactor);
        adminClient.createTopics(Collections.singleton(newTopic));
    }

    private static void mockData() throws JsonProcessingException {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootStrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        String[] keys = {"id", "name", "age", "city"};
        Object[][] objects = {
                {1, "marko", 29, "Beijing"},
                {2, "vadas", 27, "HongKong"},
                {3, "josh", 32, "Beijing"},
                {4, "peter", 35, "Shanghai"},
                {5, "peter", 26, "Wu,han"}
        };

        for (Object[] object : objects) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], object[i]);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            String value = objectMapper.writeValueAsString(map);
            System.out.println(value);
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(DEFAULT_TOPIC_NAME, value);
            producer.send(record);
        }

        producer.flush();
        producer.close();
    }
}
