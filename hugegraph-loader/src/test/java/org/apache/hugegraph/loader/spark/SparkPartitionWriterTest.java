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

package org.apache.hugegraph.loader.spark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.filter.util.ShortIdConfig;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.junit.Assert;
import org.junit.Test;

public class SparkPartitionWriterTest {

    @Test
    public void testPartitionClosureCanCrossExecutorBoundary() throws Exception {
        LoadOptions options = new LoadOptions();
        options.host = "http://serialization-probe.invalid";
        options.port = 18080;
        options.graph = "serialization_graph";
        options.username = "fixture-user";
        options.password = "fixture-password";
        options.dryRun = true;
        options.shorterIDConfigs.add(new ShortIdConfig.ShortIdConfigConverter()
                                    .convert("person:name:text"));
        String mapping = "{\"vertices\":[{\"label\":\"person\",\"id\":\"name\","
                         + "\"input\":{\"type\":\"file\",\"format\":\"JSON\","
                         + "\"path\":\"persons.json\",\"header\":[\"name\"]}}]}";
        InputStruct struct = MappingUtil.parse(mapping).structs().get(0);
        HugeGraphSparkLoader.PartitionWriter writer =
                new HugeGraphSparkLoader.PartitionWriter(options, struct);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(writer);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            Object restored = in.readObject();
            Assert.assertEquals(writer.getClass(), restored.getClass());
            Assert.assertNotSame(writer, restored);
            HugeGraphSparkLoader.PartitionWriter restoredWriter =
                    (HugeGraphSparkLoader.PartitionWriter) restored;
            LoadOptions restoredOptions = JsonUtil.fromJson(restoredWriter.optionsJson, LoadOptions.class);
            Assert.assertEquals(options.host, restoredOptions.host);
            Assert.assertEquals(options.port, restoredOptions.port);
            Assert.assertEquals(options.graph, restoredOptions.graph);
            Assert.assertEquals(options.username, restoredOptions.username);
            Assert.assertEquals(options.password, restoredOptions.password);
            Assert.assertTrue(restoredOptions.dryRun);
            Assert.assertEquals(1, restoredOptions.shorterIDConfigs.size());
            ShortIdConfig shortId = restoredOptions.getShortIdConfig("person");
            Assert.assertEquals("name", shortId.getIdFieldName());
            Assert.assertEquals(options.shorterIDConfigs.get(0).getIdFieldType(), shortId.getIdFieldType());
            InputStruct restoredStruct = JsonUtil.fromJson(restoredWriter.structJson, InputStruct.class);
            Assert.assertEquals("person", restoredStruct.vertices().get(0).label());
            Assert.assertEquals("persons.json", restoredStruct.input().asFileSource().path());
            Assert.assertArrayEquals(new String[]{"name"}, restoredStruct.input().asFileSource().header());
        }
    }
}
