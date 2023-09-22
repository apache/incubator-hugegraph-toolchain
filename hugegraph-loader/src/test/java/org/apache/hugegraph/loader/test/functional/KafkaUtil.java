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

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class KafkaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaUtil.class);

    private static final DockerImageName KAFKA_DOCKER_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

    private static KafkaContainer kafkaContainer;

    private static String bootStrapServers;

    private static AdminClient adminClient;

    public static void prepare() {
        startService();
        createAdminClient();
    }

    public static void close() {
        adminClient.close();
        kafkaContainer.close();
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

    public static void createTopic(String topicName) {
        int numPartitions = 1;
        int replicationFactor = 1;
        NewTopic newTopic =
                new NewTopic(topicName, numPartitions, (short) replicationFactor);
        adminClient.createTopics(Collections.singleton(newTopic));
    }

    public static String getBootStrapServers() {
        return bootStrapServers;
    }

    public static AdminClient getAdminClient() {
        return adminClient;
    }

    //@Test
    //public void testKafkaEnv() throws ExecutionException, InterruptedException {
    //    listTopics();
    //
    //    //testConsumer();
    //}

    //private static void testConsumer() {
    //    Properties properties = new Properties();
    //    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
    //    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
    //    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
    //                   StringDeserializer.class.getName());
    //    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
    //                   StringDeserializer.class.getName());
    //    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
    //                   "earliest");
    //
    //    Consumer<String, String> consumer = new KafkaConsumer<>(properties);
    //
    //    consumer.subscribe(Collections.singletonList(DEFAULT_TOPIC_NAME));
    //
    //    AtomicInteger cnt = new AtomicInteger();
    //    while (true) {
    //        ConsumerRecords<String, String> records =
    //                consumer.poll(Duration.ofMillis(100));
    //
    //        records.forEach(record -> {
    //            System.out.printf("Consumed record with key %s and value %s%n", record.key(),
    //                              record.value());
    //            cnt.getAndIncrement();
    //        });
    //        if (cnt.get() >= 5) {
    //            break;
    //        }
    //    }
    //}
    //
    //private static void listTopics() throws InterruptedException, ExecutionException {
    //    ListTopicsResult listTopicsResult = adminClient.listTopics();
    //    Set<String> topicNames = listTopicsResult.names().get();
    //
    //    for (String topicName : topicNames) {
    //        System.out.println(topicName);
    //    }
    //}
}
