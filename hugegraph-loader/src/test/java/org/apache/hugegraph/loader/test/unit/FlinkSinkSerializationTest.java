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

package org.apache.hugegraph.loader.test.unit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.flink.HugeGraphOutputFormat;
import org.apache.hugegraph.loader.flink.HugeGraphSink;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class FlinkSinkSerializationTest {

    @Test
    public void testJdbcMappingAndOptionsRoundTripWithoutSubmitterFile() throws Exception {
        Path mapping = Files.createTempFile("flink-mapping", ".json");
        try {
            try (InputStream resource = getClass().getResourceAsStream("/jdbc_number_to_string/struct.json")) {
                Assert.assertNotNull(resource);
                Files.copy(resource, mapping, StandardCopyOption.REPLACE_EXISTING);
            }
            InputStruct struct = LoadMapping.of(mapping.toString()).structs().get(0);
            HugeGraphOutputFormat<Object> format = new HugeGraphOutputFormat<>(struct, new String[]{
                    "--file", mapping.toString(), "--graph", "fixture",
                    "--cdc-flush-interval", "1234", "--cdc-sink-parallelism", "2"
            });
            HugeGraphSink<Object> sink = new HugeGraphSink<>(format);
            Files.delete(mapping);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(buffer)) {
                output.writeObject(sink);
            }
            Object restored;
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                restored = input.readObject();
            }
            Object restoredFormat = field(restored, "outputFormat");
            LoadOptions options = (LoadOptions) field(restoredFormat, "loadOptions");
            InputStruct restoredStruct = (InputStruct) field(restoredFormat, "struct");
            Assert.assertEquals("fixture", options.graph);
            Assert.assertEquals(1234, options.flushIntervalMs);
            Assert.assertEquals(2, options.sinkParallelism);
            Assert.assertTrue(restoredStruct.input() instanceof JDBCSource);
            Assert.assertEquals(JsonUtil.toJson(struct), JsonUtil.toJson(restoredStruct));
            Assert.assertNull(field(restoredFormat, "loadContext"));
            Assert.assertNull(field(restoredFormat, "builders"));
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}
