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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.flink.api.common.TaskInfo;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.legacy.ParallelSourceFunction;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.graph.StreamNode;
import org.apache.flink.streaming.runtime.operators.sink.SinkWriterOperatorFactory;
import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class HugeGraphSinkTest {

    @Test
    public void testRejectsUnsupportedOptionsBeforeOpeningClient() throws Exception {
        Path mapping = Files.createTempFile("flink-unsupported", ".json");
        try {
            Files.writeString(mapping, "not a mapping");
            for (String option : new String[]{"--dry-run", "--use-prefilter"}) {
                String[] args = {"--file", mapping.toString(), "--graph", "fixture", option, "true"};
                Assert.assertThrows(IllegalArgumentException.class, () -> new HugeGraphFlinkCDCLoader(args));
                Assert.assertThrows(IllegalArgumentException.class,
                                    () -> new HugeGraphOutputFormat<>(InputStruct.EMPTY, args));
            }
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    @Test
    public void testInvalidUpdatePreservesSnapshotIdentity() throws Exception {
        try (MappedFixture fixture = new MappedFixture(true)) {
            RecordingGraph graph = installRecordingGraph(fixture.format, fixture.builder);
            String before = "{\"id\":\"one\",\"a\":\"old\",\"b\":\"old\"}";
            String after = "{\"id\":\"two\",\"a\":\"new\",\"b\":\"new\"}";
            fixture.format.writeRecord("{\"op\":\"r\",\"data\":" + before + "}");
            Assert.assertEquals(Set.of("one"), graph.vertices.keySet());
            Assert.assertThrows(IllegalArgumentException.class,
                                () -> fixture.format.writeRecord("{\"op\":\"u\",\"before\":" + before +
                                                                 ",\"data\":" + after + "}"));
            Assert.assertEquals(0, graph.deletes);
            Assert.assertEquals(Set.of("one"), graph.vertices.keySet());
            Assert.assertEquals("old", graph.vertices.get("one").property("city"));
        }
    }

    @Test
    public void testInvalidEdgeUpdatePreservesSnapshotIdentity() throws Exception {
        try (MappedFixture fixture = new MappedFixture(true)) {
            InputStruct struct = JsonUtil.fromJson("{\"input\":{\"type\":\"jdbc\",\"vendor\":\"mysql\", " +
                                                   "\"header\":[\"source\",\"target\",\"city\"]}}", InputStruct.class);
            EdgeLabel label = JsonUtil.fromJson("{\"name\":\"fixture_edge\",\"frequency\":\"SINGLE\", " +
                                               "\"links\":[{\"fixture\":\"fixture\"}]," +
                                               "\"properties\":[\"city\"],\"nullable_keys\":[\"city\"]}",
                                               EdgeLabel.class);
            Field contextField = HugeGraphOutputFormat.class.getDeclaredField("loadContext");
            contextField.setAccessible(true);
            LoadContext context = (LoadContext) contextField.get(fixture.format);
            Field cacheField = LoadContext.class.getDeclaredField("schemaCache");
            cacheField.setAccessible(true);
            cacheField.set(context, new SchemaCache(List.of(new PropertyKey("city")),
                                                   List.of(fixture.label), List.of(label)));
            EdgeMapping mapping = new EdgeMapping(List.of("source"), false, List.of("target"), false);
            mapping.label("fixture_edge");
            EdgeBuilder builder = new EdgeBuilder(context, struct, mapping);
            HugeGraphOutputFormat<Object> format = new HugeGraphOutputFormat<>(struct,
                    new String[]{"--file", fixture.file.toString(), "--graph", "fixture"});
            contextField.set(format, context);
            RecordingGraph graph = installRecordingGraph(format, builder);
            String before = "{\"source\":\"one\",\"target\":\"two\",\"city\":\"old\"}";
            String after = "{\"source\":\"one\",\"target\":\"three\",\"city\":\"new\"}";
            format.writeRecord("{\"op\":\"r\",\"data\":" + before + "}");
            Assert.assertEquals(1, graph.edges.size());
            Assert.assertThrows(IllegalArgumentException.class,
                                () -> format.writeRecord("{\"op\":\"u\",\"before\":" + before +
                                                         ",\"data\":" + after + "}"));
            Assert.assertEquals(0, graph.edgeReads);
            Assert.assertEquals(0, graph.deletes);
            Assert.assertEquals(1, graph.edges.size());
            Assert.assertEquals("two", graph.edges.get(0).targetId());
            Assert.assertEquals("old", graph.edges.get(0).property("city"));
        }
    }

    @Test
    public void testAllMappingsAreCheckedBeforeAnyWriteOrBufferChange() throws Exception {
        try (MappedFixture fixture = new MappedFixture(true)) {
            RecordingGraph graph = installRecordingGraph(fixture.format, fixture.builder);
            Field contextField = HugeGraphOutputFormat.class.getDeclaredField("loadContext");
            contextField.setAccessible(true);
            LoadContext context = (LoadContext) contextField.get(fixture.format);
            VertexMapping changedMapping = new VertexMapping("a", false);
            changedMapping.label("fixture");
            VertexBuilder changed = new VertexBuilder(context, fixture.struct, changedMapping);
            Map<ElementBuilder, List<String>> mappings = new LinkedHashMap<>();
            mappings.put(fixture.builder, new ArrayList<>());
            mappings.put(changed, new ArrayList<>());
            Field builders = HugeGraphOutputFormat.class.getDeclaredField("builders");
            builders.setAccessible(true);
            builders.set(fixture.format, mappings);
            String before = "{\"id\":\"one\",\"a\":\"old\",\"b\":\"old\"}";
            String after = "{\"id\":\"one\",\"a\":\"new\",\"b\":\"new\"}";
            Assert.assertThrows(IllegalArgumentException.class,
                                () -> fixture.format.writeRecord("{\"op\":\"u\",\"before\":" + before +
                                                                 ",\"data\":" + after + "}"));
            Assert.assertTrue(graph.vertices.isEmpty());
            Assert.assertEquals(0, graph.deletes);
            mappings.values().forEach(rows -> Assert.assertTrue(rows.isEmpty()));
        }
    }

    @Test
    public void testRejectsAddedAndRemovedUnfoldedIdentities() throws Exception {
        for (String[] ids : new String[][]{{"one", "one|two"}, {"one|two", "one"},
                                           {"one|two", "one|three"}}) {
            try (MappedFixture fixture = new MappedFixture(true, true)) {
                RecordingGraph graph = installRecordingGraph(fixture.format, fixture.builder);
                String before = JsonUtil.toJson(Map.of("id", ids[0], "a", "old", "b", "old"));
                String after = JsonUtil.toJson(Map.of("id", ids[1], "a", "new", "b", "new"));
                fixture.format.writeRecord("{\"op\":\"r\",\"data\":" + before + "}");
                Set<Object> existing = Set.copyOf(graph.vertices.keySet());
                Assert.assertThrows(IllegalArgumentException.class,
                                    () -> fixture.format.writeRecord("{\"op\":\"u\",\"before\":" + before +
                                                                     ",\"data\":" + after + "}"));
                Assert.assertEquals(existing, graph.vertices.keySet());
                graph.vertices.values().forEach(vertex -> Assert.assertEquals("old", vertex.property("city")));
                Assert.assertEquals(0, graph.deletes);
                Field builders = HugeGraphOutputFormat.class.getDeclaredField("builders");
                builders.setAccessible(true);
                Map<ElementBuilder, List<String>> mappings =
                        (Map<ElementBuilder, List<String>>) builders.get(fixture.format);
                mappings.values().forEach(rows -> Assert.assertTrue(rows.isEmpty()));
            }
        }
    }

    private static RecordingGraph installRecordingGraph(HugeGraphOutputFormat<?> format,
                                                          ElementBuilder<?> builder) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field instance = unsafe.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        Object allocator = instance.get(null);
        // Reuse the cached-schema allocation seam without constructing an HTTP client.
        RecordingGraph graph = (RecordingGraph) unsafe.getMethod("allocateInstance", Class.class)
                                                          .invoke(allocator, RecordingGraph.class);
        graph.vertices = new LinkedHashMap<>();
        graph.edges = new ArrayList<>();
        HugeClient client = (HugeClient) unsafe.getMethod("allocateInstance", Class.class)
                                                  .invoke(allocator, HugeClient.class);
        Field graphField = HugeClient.class.getDeclaredField("graph");
        graphField.setAccessible(true);
        graphField.set(client, graph);
        Field contextField = HugeGraphOutputFormat.class.getDeclaredField("loadContext");
        contextField.setAccessible(true);
        LoadContext context = (LoadContext) contextField.get(format);
        Field clientField = LoadContext.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(context, client);
        Field builders = HugeGraphOutputFormat.class.getDeclaredField("builders");
        builders.setAccessible(true);
        builders.set(format, Map.of(builder, new ArrayList<String>()));
        return graph;
    }

    private static class RecordingGraph extends GraphManager {

        private Map<Object, Vertex> vertices;
        private List<Edge> edges;
        private int edgeReads;
        private int deletes;

        RecordingGraph() {
            super(null, "DEFAULT", "fixture");
        }

        @Override
        public List<Vertex> addVertices(List<Vertex> batch) {
            batch.forEach(vertex -> this.vertices.put(vertex.id(), vertex));
            return batch;
        }

        @Override
        public Vertex getVertex(Object id) {
            return this.vertices.get(id);
        }

        @Override
        public void removeVertex(Object id) {
            this.deletes++;
            this.vertices.remove(id);
        }

        @Override
        public List<Vertex> updateVertices(BatchVertexRequest request) {
            throw new AssertionError("Invalid request reached graph update");
        }

        @Override
        public List<Edge> addEdges(List<Edge> batch) {
            this.edges.addAll(batch);
            return batch;
        }

        @Override
        public List<Edge> getEdges(Object vertexId, Direction direction, String label,
                                   Map<String, Object> properties, boolean keepP, int offset, int limit) {
            this.edgeReads++;
            return this.edges;
        }

        @Override
        public void removeEdge(String id) {
            this.deletes++;
            this.edges.clear();
        }

        @Override
        public List<Edge> updateEdges(BatchEdgeRequest request) {
            throw new AssertionError("Invalid request reached graph update");
        }
    }

    @Test
    public void testRejectsParallelSinkBeforeReadingMapping() throws Exception {
        Path mapping = Files.createTempFile("flink-parallelism", ".json");
        try {
            Files.writeString(mapping, "not a mapping");
            for (String parallelism : new String[]{"0", "-1", "2"}) {
                Assert.assertThrows(IllegalArgumentException.class, () -> new HugeGraphFlinkCDCLoader(
                        new String[]{"--file", mapping.toString(), "--graph", "fixture",
                                     "--cdc-sink-parallelism", parallelism}));
            }
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    @Test
    public void testInvalidParallelSinkFailsDirectJvm() throws Exception {
        Path directory = Files.createTempDirectory("flink-exit");
        Process process = null;
        try {
            Path mapping = directory.resolve("mapping.json");
            Files.writeString(mapping, "not a mapping");
            String classpath = System.getProperty("surefire.test.class.path");
            Assert.assertNotNull(classpath);
            Path java = Path.of(System.getProperty("java.home"), "bin", "java");
            process = new ProcessBuilder(java.toString(), "-Xmx96m", "-XX:ActiveProcessorCount=2",
                                         "-Dfile.encoding=UTF-8", "-cp", classpath,
                                         HugeGraphFlinkCDCLoader.class.getName(),
                                         "--file", mapping.toString(), "--graph", "fixture",
                                         "--cdc-sink-parallelism", "2")
                    .directory(directory.toFile()).redirectErrorStream(true).start();
            Assert.assertTrue("Direct JVM did not exit", process.waitFor(30, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertTrue(output, output.contains("CDC ordered writes require --cdc-sink-parallelism 1"));
            Assert.assertFalse(output, output.contains("NoClassDefFoundError"));
            Assert.assertFalse(output, output.contains("ClassNotFoundException"));
            Assert.assertFalse(output, output.contains("OutOfMemoryError"));
            Assert.assertNotEquals(output, 0, process.exitValue());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    @Test
    public void testAcceptsDefaultAndExplicitSerialSink() throws Exception {
        Path mapping = Files.createTempFile("flink-parallelism", ".json");
        try {
            Assert.assertNotNull(new HugeGraphFlinkCDCLoader(
                    new String[]{"--file", mapping.toString(), "--graph", "fixture"}));
            Assert.assertNotNull(new HugeGraphFlinkCDCLoader(
                    new String[]{"--file", mapping.toString(), "--graph", "fixture",
                                 "--cdc-sink-parallelism", "1"}));
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    @Test
    public void testPipelinePinsSourceAndSinkWithoutChangingGlobalParallelism() throws Exception {
        Path mapping = Files.createTempFile("flink-parallelism", ".json");
        try {
            try (InputStream resource = getClass().getResourceAsStream("/jdbc_number_to_string/struct.json")) {
                Assert.assertNotNull(resource);
                Files.copy(resource, mapping, StandardCopyOption.REPLACE_EXISTING);
            }
            for (boolean globalParallelism : new boolean[]{false, true}) {
                StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
                if (globalParallelism) {
                    env.setParallelism(2);
                }
                int configuredParallelism = env.getParallelism();
                HugeGraphFlinkCDCLoader loader = new HugeGraphFlinkCDCLoader(
                        new String[]{"--file", mapping.toString(), "--graph", "fixture"});
                for (InputStruct struct : LoadMapping.of(mapping.toString()).structs()) {
                    DataStreamSource<String> source = env.addSource(new ParallelFixtureSource());
                    if (globalParallelism) {
                        Assert.assertEquals(2, source.getParallelism());
                    }
                    loader.configureSink(source, struct);
                    Assert.assertEquals(1, source.getParallelism());
                }
                Assert.assertEquals(configuredParallelism, env.getParallelism());
                StreamGraph graph = env.getStreamGraph();
                Assert.assertEquals(2, graph.getSourceIDs().size());
                Assert.assertEquals(4, graph.getStreamNodes().size());
                int sinks = 0;
                for (StreamNode node : graph.getStreamNodes()) {
                    Assert.assertEquals(1, node.getParallelism());
                    if (node.getOperatorFactory() instanceof SinkWriterOperatorFactory<?, ?>) {
                        SinkWriterOperatorFactory<?, ?> writer =
                                (SinkWriterOperatorFactory<?, ?>) node.getOperatorFactory();
                        Assert.assertTrue(writer.getSink() instanceof HugeGraphSink<?>);
                        Assert.assertTrue(node.getOutEdges().isEmpty());
                        sinks++;
                    }
                }
                Assert.assertEquals(2, sinks);
            }
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    private static class ParallelFixtureSource implements ParallelSourceFunction<String> {

        private static final long serialVersionUID = 1L;
        private volatile boolean cancelled;

        @Override
        public void run(SourceContext<String> context) {
            synchronized (context.getCheckpointLock()) {
                if (!this.cancelled) {
                    context.collect("fixture");
                }
            }
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }

    @Test
    public void testUsesPublicFlinkWriterContract() throws Exception {
        // CDC's compatibility jar also contains a Sink class with a legacy abstract method.
        Assert.assertTrue(Modifier.isAbstract(Sink.class.getMethod("createWriter",
                                                                   WriterInitContext.class).getModifiers()));
    }

    @Test
    public void testWriterLifecycleAndFlushFailure() throws Exception {
        Path mapping = Files.createTempFile("flink-sink", ".json");
        try {
            RecordingFormat format = new RecordingFormat(mapping);
            HugeGraphSink<String> sink = new HugeGraphSink<>(format);
            SinkWriter<String> writer = sink.createWriter(context());
            Assert.assertEquals(2, format.tasks);
            Assert.assertEquals(1, format.task);
            Assert.assertEquals(3, format.attempt);
            writer.write("record", null);
            Assert.assertEquals(1, format.rows.size());
            Assert.assertEquals("record", format.rows.get(0));
            writer.flush(false);
            Assert.assertEquals(1, format.flushes);
            format.failFlush = true;
            Assert.assertThrows(IllegalStateException.class, () -> writer.flush(false));
            writer.close();
            Assert.assertTrue(format.closed);
        } finally {
            Files.deleteIfExists(mapping);
        }
    }

    @Test
    public void testNullUpdateRemovesMappedPropertyAndCanBeReplayed() throws Exception {
        Path file = Files.createTempFile("flink-null", ".json");
        try {
            InputStruct struct = JsonUtil.fromJson("{\"input\":{\"type\":\"jdbc\",\"vendor\":\"mysql\", " +
                                                   "\"header\":[\"city\",\"name\"]}}",
                                                   InputStruct.class);
            HugeGraphOutputFormat<Object> format = new HugeGraphOutputFormat<>(struct,
                    new String[]{"--file", file.toString(), "--graph", "fixture"});
            VertexMapping mapping = new VertexMapping("id", false);
            mapping.mappingFields(Map.of("city", "location"));
            mapping.ignoredFields(Set.of("name"));
            Vertex vertex = new Vertex("fixture") {
                @Override
                public Vertex removeProperty(String key) {
                    Assert.assertTrue(this.properties().containsKey(key));
                    this.properties().remove(key);
                    return this;
                }
            };
            vertex.property("location", "old");
            vertex.property("name", "keep");
            Method update = HugeGraphOutputFormat.class.getDeclaredMethod("removeNullProperties",
                                                                          List.class, ElementMapping.class,
                                                                          String.class, Set.class);
            update.setAccessible(true);
            String event = "{\"data\":{\"city\":null,\"name\":null}}";
            update.invoke(format, List.of(vertex), mapping, event, Set.of());
            update.invoke(format, List.of(vertex), mapping, event, Set.of());
            Assert.assertNull(vertex.property("location"));
            Assert.assertEquals("keep", vertex.property("name"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testReturnedEndpointIdKeepsSchemaType() throws Exception {
        Method match = HugeGraphOutputFormat.class.getDeclaredMethod("sameReturnedVertexId",
                                                                     Object.class, Object.class);
        match.setAccessible(true);
        UUID uuid = UUID.randomUUID();
        Assert.assertTrue((boolean) match.invoke(null, uuid, uuid.toString()));
        Assert.assertTrue((boolean) match.invoke(null, 123L, 123));
        Assert.assertFalse((boolean) match.invoke(null, 123L, "123"));
    }

    @Test
    public void testNullAliasKeepsConstructedMappedProperty() throws Exception {
        for (String missing : new String[]{null, "", "<NULL>"}) {
            try (MappedFixture fixture = new MappedFixture(true)) {
                if ("<NULL>".equals(missing)) {
                    fixture.mapping.nullValues(Set.of("<NULL>"));
                }
                Assert.assertEquals("Paris", fixture.update(missing, "Paris").property("city"));
                Assert.assertEquals("Berlin", fixture.update("Berlin", missing).property("city"));
            }
        }
    }

    @Test
    public void testConfiguredNullSentinelsRemoveStaleProperty() throws Exception {
        for (String missing : new String[]{null, "", "<NULL>"}) {
            try (MappedFixture fixture = new MappedFixture(true)) {
                if ("<NULL>".equals(missing)) {
                    fixture.mapping.nullValues(Set.of("<NULL>"));
                }
                Assert.assertNull(fixture.update(missing, missing).property("city"));
            }
        }
    }

    @Test
    public void testCleanupKeepsNonNullableIgnoredAndUnmappedProperties() throws Exception {
        try (MappedFixture fixture = new MappedFixture(false)) {
            Assert.assertEquals("", fixture.update("", "").property("city"));
        }
        for (boolean selected : new boolean[]{false, true}) {
            try (MappedFixture fixture = new MappedFixture(true)) {
                fixture.mapping.mappingFields(Map.of("a", "city", "b", "other"));
                if (selected) {
                    fixture.mapping.selectedFields(Set.of("id", "b"));
                } else {
                    fixture.mapping.ignoredFields(Set.of("a"));
                }
                fixture.label.properties().add("other");
                fixture.label.nullableKeys().add("other");
                fixture.unmappedNull = true;
                Assert.assertEquals("old", fixture.update(null, "keep").property("city"));
            }
        }
    }

    @Test
    public void testSqlNullDoesNotDependOnConfiguredNullSentinels() throws Exception {
        try (MappedFixture fixture = new MappedFixture(true)) {
            fixture.mapping.nullValues(Set.of());
            Assert.assertNull(fixture.update(null, null).property("city"));
            Assert.assertEquals("", fixture.update("", "").property("city"));
            Assert.assertEquals("Paris", fixture.update(null, "Paris").property("city"));
            Assert.assertEquals("Berlin", fixture.update("Berlin", null).property("city"));
        }
        try (MappedFixture fixture = new MappedFixture(false)) {
            fixture.mapping.nullValues(Set.of());
            Assert.assertThrows(InvocationTargetException.class, () -> fixture.update(null, null),
                                failure -> Assert.assertTrue(failure.getCause() instanceof IllegalArgumentException));
        }
    }

    private static class MappedFixture implements AutoCloseable {

        private final Path file;
        private final InputStruct struct;
        private final VertexMapping mapping;
        private final VertexLabel label;
        private final VertexBuilder builder;
        private final HugeGraphOutputFormat<Object> format;
        private boolean unmappedNull;

        MappedFixture(boolean nullable) throws Exception {
            this(nullable, false);
        }

        MappedFixture(boolean nullable, boolean unfold) throws Exception {
            this.file = Files.createTempFile("flink-mapped-null", ".json");
            this.struct = JsonUtil.fromJson("{\"input\":{\"type\":\"jdbc\",\"vendor\":\"mysql\", " +
                                           "\"header\":[\"id\",\"a\",\"b\"],\"list_format\":{\"start_symbol\":\"\"," +
                                           "\"end_symbol\":\"\",\"elem_delimiter\":\"|\"}}}", InputStruct.class);
            this.mapping = new VertexMapping("id", unfold);
            this.mapping.label("fixture");
            this.mapping.mappingFields(Map.of("a", "city", "b", "city"));
            this.label = new VertexLabel("fixture");
            this.label.idStrategy(IdStrategy.CUSTOMIZE_STRING);
            this.label.properties().add("city");
            if (nullable) {
                this.label.nullableKeys().add("city");
            }
            SchemaCache cache = new SchemaCache(List.of(new PropertyKey("city"), new PropertyKey("other")),
                                                List.of(this.label), List.of());
            // Match the existing LoadContextTest allocation seam: cached schema, no clients.
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field instance = unsafe.getDeclaredField("theUnsafe");
            instance.setAccessible(true);
            LoadContext context = (LoadContext) unsafe.getMethod("allocateInstance", Class.class)
                                                      .invoke(instance.get(null), LoadContext.class);
            for (Map.Entry<String, Object> field : Map.of("options", new LoadOptions(),
                                                         "schemaCache", cache).entrySet()) {
                Field target = LoadContext.class.getDeclaredField(field.getKey());
                target.setAccessible(true);
                target.set(context, field.getValue());
            }
            this.builder = new VertexBuilder(context, this.struct, this.mapping);
            this.format = new HugeGraphOutputFormat<>(this.struct,
                    new String[]{"--file", this.file.toString(), "--graph", "fixture"});
            Field loadContext = HugeGraphOutputFormat.class.getDeclaredField("loadContext");
            loadContext.setAccessible(true);
            loadContext.set(this.format, context);
        }

        Vertex update(String first, String second) throws Exception {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", "one");
            data.put("a", first);
            data.put("b", second);
            if (this.unmappedNull) {
                // Not in the input header: do not remove an unrelated graph property.
                data.put("city", null);
            }
            Method build = HugeGraphOutputFormat.class.getDeclaredMethod("buildElements",
                                                                         ElementBuilder.class, JsonNode.class,
                                                                         boolean.class);
            build.setAccessible(true);
            List<GraphElement> built = (List<GraphElement>) build.invoke(this.format, this.builder,
                    new ObjectMapper().valueToTree(data), false);
            Assert.assertEquals(1, built.size());
            Vertex returned = new Vertex("fixture") {
                @Override
                public Vertex removeProperty(String key) {
                    this.properties().remove(key);
                    return this;
                }
            };
            returned.property("city", "old");
            built.get(0).properties().forEach(returned::property);
            Method remove = HugeGraphOutputFormat.class.getDeclaredMethod("removeNullProperties",
                                                                          List.class, ElementMapping.class,
                                                                          String.class, Set.class);
            remove.setAccessible(true);
            remove.invoke(this.format, List.of(returned), this.mapping,
                          JsonUtil.toJson(Map.of("data", data)), built.get(0).properties().keySet());
            return returned;
        }

        @Override
        public void close() throws Exception {
            Files.deleteIfExists(this.file);
        }
    }

    private static WriterInitContext context() {
        TaskInfo task = (TaskInfo) Proxy.newProxyInstance(TaskInfo.class.getClassLoader(),
                new Class<?>[]{TaskInfo.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getNumberOfParallelSubtasks":
                            return 2;
                        case "getIndexOfThisSubtask":
                            return 1;
                        case "getAttemptNumber":
                            return 3;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        return (WriterInitContext) Proxy.newProxyInstance(WriterInitContext.class.getClassLoader(),
                new Class<?>[]{WriterInitContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getTaskInfo")) {
                        return task;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static class RecordingFormat extends HugeGraphOutputFormat<Object> {

        private static final long serialVersionUID = 1L;
        private final List<Object> rows = new ArrayList<>();
        private int tasks;
        private int task;
        private int attempt;
        private int flushes;
        private boolean failFlush;
        private boolean closed;

        RecordingFormat(Path mapping) {
            super(InputStruct.EMPTY, new String[]{"--file", mapping.toString(), "--graph", "fixture"});
        }

        @Override
        public void open(InitializationContext context) {
            this.tasks = context.getNumTasks();
            this.task = context.getTaskNumber();
            this.attempt = context.getAttemptNumber();
        }

        @Override
        public void writeRecord(Object row) {
            this.rows.add(row);
        }

        @Override
        synchronized void flushAll() {
            if (this.failFlush) {
                throw new IllegalStateException("flush failed");
            }
            this.flushes++;
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }
}
