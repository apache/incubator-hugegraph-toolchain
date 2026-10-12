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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.flink.util.Collector;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Assume;
import org.junit.Test;

/** Explicit opt-in test against a running, test-only HugeGraph instance. */
public class HugeGraphIdentityIntegrationTest {

    @Test
    public void testIdentityChangesDeletesAndReplay() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("flink.identity.integration"));
        int port = Integer.getInteger("flink.identity.port", 8080);
        String graph = System.getProperty("flink.identity.graph", "hugegraph");
        String prefix = "flink_identity_" + UUID.randomUUID().toString().replace("-", "");
        String custom = prefix + "_custom";
        String primary = prefix + "_primary";
        String edgeLabel = prefix + "_edge";
        String uuidLabel = prefix + "_uuid";
        String name = prefix + "_name";
        String value = prefix + "_value";
        String rank = prefix + "_rank";
        long base = 8100000000000000L + System.nanoTime() % 10000000000L;
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> edgeLabels = new ArrayList<>();
        try (HugeClient client = HugeClient.builder("http://127.0.0.1:" + port,
                                                   "DEFAULT", graph).build()) {
            SchemaManager schema = client.schema();
            try {
                for (String key : new String[]{name, value, rank}) {
                    schema.propertyKey(key).asText().create();
                    keys.add(key);
                }
                schema.vertexLabel(custom).useCustomizeNumberId().properties(value)
                      .nullableKeys(value).create();
                labels.add(custom);
                schema.vertexLabel(primary).usePrimaryKeyId().properties(name, value)
                      .primaryKeys(name).nullableKeys(value).create();
                labels.add(primary);
                schema.edgeLabel(edgeLabel).sourceLabel(custom).targetLabel(custom)
                      .multiTimes().properties(rank, value).sortKeys(rank).nullableKeys(value).create();
                edgeLabels.add(edgeLabel);
                schema.vertexLabel(uuidLabel).useCustomizeUuidId().properties(value).nullableKeys(value).create();
                labels.add(uuidLabel);
                verifyUuidVertex(port, client, uuidLabel, value);

                verifyVertices(port, client, custom, value, base + 1000, false);
                verifyVertices(port, client, primary, value, 0, true);

                List<Vertex> vertices = new ArrayList<>();
                for (int i = 0; i <= 106; i++) {
                    long vertexId = base + i;
                    assertNotFound(() -> client.graph().getVertex(vertexId));
                    Vertex vertex = new Vertex(custom);
                    vertex.id(vertexId);
                    vertex.property(value, "old");
                    vertices.add(vertex);
                }
                client.graph().addVertices(vertices);
                List<Edge> edges = new ArrayList<>();
                for (int i = 1; i <= 105; i++) {
                    Edge edge = new Edge(edgeLabel);
                    edge.source(vertices.get(0));
                    edge.target(vertices.get(i));
                    edge.property(rank, "P.gt(1)");
                    edges.add(edge);
                }
                client.graph().addEdges(edges);
                Map<String, Object> vertexMapping = Map.of("label", custom, "id", "key",
                        "field_mapping", Map.of("value", value));
                try (FormatFixture fixture = new FormatFixture(port, new String[]{"key", "value"},
                                                                "vertices", vertexMapping)) {
                    Map<String, String> before = Map.of("key", Long.toString(base), "value", "old");
                    Map<String, String> after = Map.of("key", Long.toString(base + 10000), "value", "new");
                    Assert.assertThrows(IllegalArgumentException.class, () -> fixture.emit("u", before, after));
                    Assert.assertEquals("old", client.graph().getVertex(base).property(value));
                    Assert.assertEquals(105, client.graph().getEdges(base, Direction.OUT, edgeLabel).size());
                    assertNotFound(() -> client.graph().getVertex(base + 10000));
                }
                Map<String, Object> mapping = Map.of("label", edgeLabel,
                        "source", List.of("source"), "target", List.of("target"),
                        "field_mapping", Map.of("rank", rank, "value", value),
                        "update_strategies", Map.of(value, "OVERRIDE"));
                try (FormatFixture fixture = new FormatFixture(port,
                        new String[]{"source", "target", "rank", "value"}, "edges", mapping)) {
                    List<Edge> ordered = client.graph().getEdges(base, Direction.OUT, edgeLabel,
                                                                 Map.of(rank, "P.gt(1)"), true, 0, 200);
                    Assert.assertEquals(105, ordered.size());
                    // Choose an actual result beyond the first page instead of assuming ID ordering.
                    Object lastTarget = ordered.get(104).targetId();
                    Map<String, String> deleted = edgeRow(base, lastTarget, "P.gt(1)", "old");
                    fixture.emit("d", deleted, null);
                    fixture.emit("d", deleted, null);
                    Assert.assertEquals(104, client.graph().getEdges(base, Direction.OUT, edgeLabel).size());
                    assertMissing(client, ordered.get(104));

                    Edge changed = ordered.get(103);
                    Map<String, String> before = edgeRow(base, changed.targetId(), "P.gt(1)", "old");
                    Map<String, String> after = edgeRow(base, base + 106, "P.gt(1)", "new");
                    Assert.assertThrows(IllegalArgumentException.class, () -> fixture.emit("u", before, after));
                    Assert.assertThrows(IllegalArgumentException.class, () -> fixture.emit("u", before, after));
                    Assert.assertEquals("P.gt(1)", client.graph().getEdge(changed.id()).property(rank));
                    Assert.assertEquals(104, client.graph().getEdges(base, Direction.OUT, edgeLabel).size());
                    Assert.assertTrue(client.graph().getEdges(base, Direction.OUT, edgeLabel).stream().noneMatch(
                            edge -> edge.targetId().toString().equals(Long.toString(base + 106))));

                    Map<String, String> reranked = edgeRow(base, changed.targetId(), "later", "latest");
                    Assert.assertThrows(IllegalArgumentException.class,
                                        () -> fixture.emit("u", before, reranked));
                    Assert.assertEquals("P.gt(1)", client.graph().getEdge(changed.id()).property(rank));
                    Map<String, String> updated = edgeRow(base, changed.targetId(), "P.gt(1)", "latest");
                    fixture.emit("u", before, updated);
                    fixture.emit("u", before, updated);
                    Assert.assertEquals("latest", client.graph().getEdge(changed.id()).property(value));
                    fixture.emit("d", updated, null);
                    fixture.emit("d", updated, null);
                    assertMissing(client, changed);
                    Assert.assertEquals(103, client.graph().getEdges(base, Direction.OUT, edgeLabel).size());
                }
            } finally {
                for (String edge : edgeLabels) {
                    schema.removeEdgeLabel(edge);
                }
                for (String label : labels) {
                    schema.removeVertexLabel(label);
                }
                for (String key : keys) {
                    schema.removePropertyKey(key);
                }
            }
            for (String label : labels) {
                assertNotFound(() -> schema.getVertexLabel(label));
            }
            for (String edge : edgeLabels) {
                assertNotFound(() -> schema.getEdgeLabel(edge));
            }
            for (String key : keys) {
                assertNotFound(() -> schema.getPropertyKey(key));
            }
        }
    }

    private static void verifyUuidVertex(int port, HugeClient client, String label,
                                         String valueKey) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> mapping = Map.of("label", label, "id", "id",
                                            "field_mapping", Map.of("value", valueKey));
        try (FormatFixture fixture = new FormatFixture(port, new String[]{"id", "value"}, "vertices", mapping)) {
            Map<String, String> row = Map.of("id", id.toString(), "value", "uuid");
            fixture.emit("c", null, row);
            Assert.assertEquals("uuid", client.graph().getVertex(id).property(valueKey));
            fixture.emit("d", row, null);
            fixture.emit("d", row, null);
            assertNotFound(() -> client.graph().getVertex(id));
        }
    }

    private static void verifyVertices(int port, HugeClient client, String label,
                                       String valueKey, long id, boolean primary) throws Exception {
        String key = primary ? label.replace("_primary", "_name") : "id";
        Map<String, Object> mapping = new java.util.HashMap<>();
        mapping.put("label", label);
        mapping.put("update_strategies", Map.of(valueKey, "OVERRIDE"));
        mapping.put("field_mapping", Map.of("key", key, "value", valueKey));
        if (!primary) {
            mapping.put("id", "key");
        }
        if (!primary) {
            assertNotFound(() -> client.graph().getVertex(id));
            assertNotFound(() -> client.graph().getVertex(id + 1));
        }
        try (FormatFixture fixture = new FormatFixture(port, new String[]{"key", "value"},
                                                        "vertices", mapping)) {
            Map<String, String> before = Map.of("key", primary ? "old" : Long.toString(id), "value", "old");
            Map<String, String> after = Map.of("key", primary ? "new" : Long.toString(id + 1), "value", "new");
            fixture.emit("c", null, before);
            fixture.emit("c", null, before);
            List<Vertex> vertices = client.graph().listVertices(label, null, 0, 10);
            Assert.assertEquals(1, vertices.size());
            Object oldId = vertices.get(0).id();
            Assert.assertThrows(IllegalArgumentException.class, () -> fixture.emit("u", before, after));
            Assert.assertThrows(IllegalArgumentException.class, () -> fixture.emit("u", before, after));
            Assert.assertEquals("old", client.graph().getVertex(oldId).property(valueKey));
            vertices = client.graph().listVertices(label, null, 0, 10);
            Assert.assertEquals(1, vertices.size());
            Map<String, String> updated = Map.of("key", before.get("key"), "value", "new");
            fixture.emit("u", before, updated);
            fixture.emit("u", before, updated);
            Assert.assertEquals("new", client.graph().getVertex(oldId).property(valueKey));
            fixture.emit("d", updated, null);
            fixture.emit("d", updated, null);
            assertNotFound(() -> client.graph().getVertex(oldId));
            Assert.assertTrue(client.graph().listVertices(label, null, 0, 10).isEmpty());
        }
    }

    private static Map<String, String> edgeRow(long source, Object target, String rank, String value) {
        return Map.of("source", Long.toString(source), "target", target.toString(), "rank", rank, "value", value);
    }

    private static void assertMissing(HugeClient client, Edge edge) {
        assertNotFound(() -> client.graph().getEdge(edge.id()));
    }

    private static void assertNotFound(Runnable read) {
        try {
            read.run();
            Assert.fail("Expected HTTP 404");
        } catch (ServerException e) {
            Assert.assertEquals(404, e.status());
        }
    }

    private static class FormatFixture implements AutoCloseable {

        private final HugeGraphOutputFormat<String> format;
        private final Path file;
        private final Schema rowSchema;
        private final Schema envelope;
        private final String[] fields;

        FormatFixture(int port, String[] fields, String kind, Map<String, Object> mapping) throws Exception {
            this.fields = fields;
            Map<String, Object> input = Map.of("type", "jdbc", "vendor", "mysql", "header", fields);
            InputStruct struct = JsonUtil.fromJson(JsonUtil.toJson(Map.of("input", input,
                                                                         kind, List.of(mapping))), InputStruct.class);
            this.file = Files.createTempFile("flink-identity-", ".json");
            this.format = new HugeGraphOutputFormat<>(struct, new String[]{"--file", this.file.toString(),
                    "--host", "127.0.0.1", "--port", Integer.toString(port),
                    "--graph", System.getProperty("flink.identity.graph", "hugegraph")});
            this.format.open(null);
            SchemaBuilder row = SchemaBuilder.struct().name("fixture.Row").optional();
            for (String field : fields) {
                row.field(field, Schema.OPTIONAL_STRING_SCHEMA);
            }
            this.rowSchema = row.build();
            this.envelope = SchemaBuilder.struct().name("fixture.Envelope")
                    .field("before", this.rowSchema).field("after", this.rowSchema)
                    .field("op", Schema.STRING_SCHEMA).build();
        }

        void emit(String op, Map<String, String> before, Map<String, String> after) {
            Struct value = new Struct(this.envelope).put("op", op)
                    .put("before", row(before)).put("after", row(after));
            SourceRecord source = new SourceRecord(Collections.emptyMap(), Collections.emptyMap(),
                                                    "fixture", this.envelope, value);
            new HugeGraphDeserialization().deserialize(source, new Collector<String>() {
                @Override
                public void collect(String record) {
                    format.writeRecord(record);
                }

                @Override
                public void close() {
                }
            });
        }

        private Struct row(Map<String, String> values) {
            if (values == null) {
                return null;
            }
            Struct row = new Struct(this.rowSchema);
            for (String field : this.fields) {
                row.put(field, values.get(field));
            }
            return row;
        }

        @Override
        public void close() throws Exception {
            this.format.close();
            Files.deleteIfExists(this.file);
        }
    }
}
