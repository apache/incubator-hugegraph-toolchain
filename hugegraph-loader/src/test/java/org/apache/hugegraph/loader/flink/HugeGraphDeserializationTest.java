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

package org.apache.hugegraph.loader.flink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HugeGraphDeserializationTest {

    private static final Schema ROW = SchemaBuilder.struct().name("fixture.Row").optional()
            .field("id", Schema.INT32_SCHEMA)
            .field("name", Schema.STRING_SCHEMA)
            .field("city", Schema.OPTIONAL_STRING_SCHEMA).build();
    private static final Schema ENVELOPE = SchemaBuilder.struct().name("fixture.Envelope")
            .field("before", ROW).field("after", ROW).field("op", Schema.STRING_SCHEMA).build();

    @Test
    public void testSnapshotAndUpdatePreserveNull() throws Exception {
        for (String op : new String[]{"r", "u"}) {
            JsonNode event = new ObjectMapper().readTree(convert(record(op, row("old-city"), row(null))).get(0));
            Assert.assertEquals(op, event.get("op").asText());
            Assert.assertEquals("1", event.get("data").get("id").asText());
            Assert.assertTrue(event.get("data").get("city").isNull());
            if ("u".equals(op)) {
                Assert.assertEquals("old-city", event.get("before").get("city").asText());
            }
        }
    }

    @Test
    public void testUpdateRequiresBeforeImage() {
        Assert.assertThrows(IllegalArgumentException.class, () -> convert(record("u", null, row(null))));
    }

    @Test
    public void testDeleteUsesBeforeImage() throws Exception {
        JsonNode event = new ObjectMapper().readTree(convert(record("d", row("old-city"), null)).get(0));
        Assert.assertEquals("d", event.get("op").asText());
        Assert.assertEquals("old-city", event.get("data").get("city").asText());
    }

    @Test
    public void testTombstoneDoesNotEmitGraphMutation() {
        SourceRecord tombstone = new SourceRecord(Collections.emptyMap(), Collections.emptyMap(),
                                                  "fixture", null, null);
        Assert.assertTrue(convert(tombstone).isEmpty());
    }

    @Test
    public void testLogsOnlySafeCdcMetadata() throws Exception {
        Logger logger = Logger.getLogger(HugeGraphDeserialization.class.getName());
        Level previous = logger.getLevel();
        CapturingAppender appender = new CapturingAppender();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        try {
            JsonNode event = new ObjectMapper().readTree(convert(record(
                    "u", row("historical-sensitive-value"), row("current-sensitive-value"))).get(0));
            Assert.assertEquals("historical-sensitive-value", event.get("before").get("city").asText());
            Assert.assertEquals("current-sensitive-value", event.get("data").get("city").asText());
            String logged = appender.buffer.toString();
            Assert.assertFalse(logged.contains("historical-sensitive-value"));
            Assert.assertFalse(logged.contains("current-sensitive-value"));
            Assert.assertFalse(logged.contains("city"));
            Assert.assertTrue(logged.contains("op=u"));
            Assert.assertTrue(logged.contains("dataFields=3"));
            Assert.assertTrue(logged.contains("beforeFields=3"));
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previous);
        }
    }

    private static class CapturingAppender extends AppenderSkeleton {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        protected void append(LoggingEvent event) {
            this.buffer.append(event.getRenderedMessage()).append('\n');
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    private static Struct row(String city) {
        return new Struct(ROW).put("id", 1).put("name", "name").put("city", city);
    }

    private static SourceRecord record(String op, Struct before, Struct after) {
        Struct value = new Struct(ENVELOPE).put("before", before).put("after", after).put("op", op);
        return new SourceRecord(Collections.emptyMap(), Collections.emptyMap(), "fixture", ENVELOPE, value);
    }

    private static List<String> convert(SourceRecord record) {
        List<String> values = new ArrayList<>();
        new HugeGraphDeserialization().deserialize(record, new Collector<String>() {
            @Override
            public void collect(String value) {
                values.add(value);
            }

            @Override
            public void close() {
            }
        });
        return values;
    }
}
