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

    public static void prepareEnv() {
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
}
