/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.loader.spark;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.junit.Assert;
import org.junit.Test;

public class SparkRowTest {

    @Test
    public void testCsvColumnsReachVertexBuilderWithoutResplitting() throws Exception {
        RecordingGraph graph = load("CSV", false, RowFactory.create("Alice, Inc", "Paris", ""));
        Assert.assertEquals(1, graph.writes);
        Vertex vertex = graph.vertices.get(0);
        Assert.assertEquals("Alice, Inc", vertex.id());
        Assert.assertEquals("Paris", vertex.properties().get("city"));
        Assert.assertFalse(vertex.properties().containsKey("note"));
    }

    @Test
    public void testCsvNullColumnPreservesOtherFields() throws Exception {
        RecordingGraph graph = load("CSV", false, RowFactory.create("Alice", null, "tail"));
        Vertex vertex = graph.vertices.get(0);
        Assert.assertEquals("Alice", vertex.id());
        Assert.assertFalse(vertex.properties().containsKey("city"));
        Assert.assertEquals("tail", vertex.properties().get("note"));
    }

    @Test
    public void testDryRunParsesRowsWithoutGraphWrites() throws Exception {
        RecordingGraph graph = load("CSV", true, RowFactory.create("Alice, Inc", "Paris", ""));
        Assert.assertEquals(0, graph.writes);
    }

    @Test(expected = LoadException.class)
    public void testDryRunStillRejectsMismatchedColumns() throws Exception {
        load("CSV", true, RowFactory.create("Alice"));
    }

    @Test
    public void testTextUsesExistingLineParser() throws Exception {
        RecordingGraph graph = load("TEXT", false, RowFactory.create("Alice,Paris,"));
        Assert.assertEquals("Alice", graph.vertices.get(0).id());
        Assert.assertEquals("Paris", graph.vertices.get(0).properties().get("city"));
    }

    @Test
    public void testStandaloneClusterRejectedWithoutStartingSpark() {
        SparkConf conf = new SparkConf(false).setMaster("spark://example:7077");
        conf.set("spark.submit.deployMode", "client");
        HugeGraphSparkLoader.checkDeployment(conf);
        conf.set("spark.submit.deployMode", "cluster");
        try {
            HugeGraphSparkLoader.checkDeployment(conf);
            Assert.fail("Expected unsupported standalone cluster deployment");
        } catch (LoadException expected) {
            Assert.assertTrue(expected.getMessage().contains("Standalone cluster"));
        }
    }

    @Test
    public void testInvalidHeadersRejectedBeforeSparkOrPartitionsStart() throws Exception {
        String oldMaster = System.getProperty("spark.master");
        String oldDeployMode = System.getProperty("spark.submit.deployMode");
        System.setProperty("spark.master", "invalid-header-preflight-master");
        System.setProperty("spark.submit.deployMode", "client");
        try {
            for (String type : Arrays.asList("file", "hdfs")) {
                for (String format : Arrays.asList("CSV", "TEXT")) {
                    assertHeaderRejectedBeforeInitialization(type, format, "\"header\":null");
                    assertHeaderRejectedBeforeInitialization(type, format, "\"has_header\":false");
                    assertHeaderRejectedBeforeInitialization(type, format,
                                                             "\"header\":[\"name\"],\"has_header\":true");
                }
            }
        } finally {
            restoreProperty("spark.master", oldMaster);
            restoreProperty("spark.submit.deployMode", oldDeployMode);
        }
    }

    @Test
    public void testHeaderlessMappingsAndJsonRemainSupported() {
        for (String type : Arrays.asList("file", "hdfs")) {
            for (String format : Arrays.asList("CSV", "TEXT")) {
                for (String header : Arrays.asList("\"header\":[\"name\"]",
                                                   "\"header\":[\"name\"],\"has_header\":false")) {
                    HugeGraphSparkLoader.checkHeaders(MappingUtil.parse(
                            "{\"vertices\":[" + headerMapping(type, format, header) + "]}").structs());
                }
            }
            HugeGraphSparkLoader.checkHeaders(MappingUtil.parse(
                    "{\"vertices\":[" + headerMapping(type, "JSON", "\"header\":null") + "]}").structs());
        }
    }

    @Test
    public void testFileCharsetPreflightBeforeSparkStarts() throws Exception {
        String oldMaster = System.getProperty("spark.master");
        String oldDeployMode = System.getProperty("spark.submit.deployMode");
        System.setProperty("spark.master", "invalid-charset-preflight-master");
        System.setProperty("spark.submit.deployMode", "client");
        try {
            for (String type : Arrays.asList("file", "hdfs")) {
                for (String format : Arrays.asList("CSV", "JSON", "TEXT")) {
                    for (String charset : Arrays.asList("UTF-8", "utf8")) {
                        HugeGraphSparkLoader.checkHeaders(MappingUtil.parse(
                                "{\"vertices\":[" + headerMapping(type, format,
                                        "\"header\":[\"name\"],\"charset\":\"" + charset + "\"") +
                                "]}").structs());
                    }
                    for (String charset : Arrays.asList("GBK", "UTF-16")) {
                        String options = "\"header\":[\"name\"],\"charset\":\"" + charset + "\"";
                        assertHeaderRejectedBeforeInitialization(type, format, options);
                        try {
                            HugeGraphSparkLoader.checkHeaders(MappingUtil.parse(
                                    "{\"vertices\":[" + headerMapping(type, format, options) +
                                    "]}").structs());
                            Assert.fail("Expected non-UTF-8 input rejection");
                        } catch (LoadException expected) {
                            Assert.assertTrue(expected.getMessage().contains("requires UTF-8 charset"));
                            Assert.assertTrue(expected.getMessage().contains(type + "-" + format));
                        }
                    }
                }
            }
        } finally {
            restoreProperty("spark.master", oldMaster);
            restoreProperty("spark.submit.deployMode", oldDeployMode);
        }
    }

    @Test
    public void testUnsupportedResumeModesRejectedBeforeMappingOrSpark() throws Exception {
        HugeGraphSparkLoader.checkLoadOptions(new LoadOptions());
        for (String mode : Arrays.asList("incrementalMode", "failureMode")) {
            LoadOptions options = new LoadOptions();
            set(options, mode, true);
            HugeGraphSparkLoader loader = allocate(HugeGraphSparkLoader.class);
            set(loader, "loadOptions", options);
            // No mapping or executor is configured: load() must reject the option first.
            try {
                loader.load();
                Assert.fail("Expected unsupported Spark resume mode rejection");
            } catch (LoadException expected) {
                Assert.assertTrue(expected.getMessage().contains("--incremental-mode"));
                Assert.assertTrue(expected.getMessage().contains("--failure-mode"));
            }
        }
    }

    private static void assertHeaderRejectedBeforeInitialization(String type, String format,
                                                                 String header) throws Exception {
        // The valid first mapping must not submit any partition before the second is checked.
        String first = headerMapping("file", "CSV", "\"header\":[\"name\"]")
                       .replace("\"path\":\"", "\"path\":\"first-");
        String mapping = "{\"vertices\":[" + first +
                         "," + headerMapping(type, format, header) + "]}";
        Path file = Files.createTempFile("spark-header-preflight-", ".json");
        try {
            // HDFS mapping validation only needs an existing core-site file here.
            mapping = mapping.replace("\"core-site-fixture\"", JsonUtil.toJson(file.toString()));
            Files.write(file, mapping.getBytes(StandardCharsets.UTF_8));
            LoadOptions options = new LoadOptions();
            options.file = file.toString();
            // Real load() must reject headers before the invalid master can initialize Spark.
            // A null executor also prevents partition submission and executor-side client setup.
            HugeGraphSparkLoader loader = allocate(HugeGraphSparkLoader.class);
            set(loader, "loadOptions", options);
            try {
                loader.load();
                Assert.fail("Expected Spark header validation failure");
            } catch (LoadException expected) {
                Assert.assertTrue(expected.getMessage().contains("Spark " + format + " input"));
                Assert.assertTrue(expected.getMessage().contains(type + "-" + format));
                Assert.assertTrue(expected.getMessage().contains("docs/spark-java17.md"));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static String headerMapping(String type, String format, String header) {
        return "{\"label\":\"person\",\"id\":\"name\",\"input\":{\"type\":\"" + type + "\"," +
               ("hdfs".equals(type) ? "\"core_site_path\":\"core-site-fixture\"," : "") +
               "\"format\":\"" + format + "\",\"path\":\"" + type + "-" + format + "-" +
               header.contains("true") + "-" + header.contains("null") + ".data\"," + header + "}}";
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static RecordingGraph load(String format, boolean dryRun, Row row) throws Exception {
        LoadOptions options = new LoadOptions();
        options.dryRun = dryRun;
        options.usePrefilter = false;
        String mapping = "{\"vertices\":[{\"label\":\"person\",\"id\":\"name\","
                         + "\"input\":{\"type\":\"file\",\"format\":\"" + format + "\","
                         + "\"delimiter\":\",\",\"path\":\"persons.csv\","
                         + "\"header\":[\"name\",\"city\",\"note\"]}}]}";
        InputStruct struct = MappingUtil.parse(mapping).structs().get(0);
        struct.check();
        HugeGraphSparkLoader.PartitionWriter writer =
                new HugeGraphSparkLoader.PartitionWriter(options, struct);
        options = JsonUtil.fromJson(writer.optionsJson, LoadOptions.class);
        struct = JsonUtil.fromJson(writer.structJson, InputStruct.class);

        VertexLabel label = new VertexLabel("person");
        label.idStrategy(IdStrategy.CUSTOMIZE_STRING);
        label.properties().addAll(Arrays.asList("name", "city", "note"));
        label.nullableKeys().addAll(Arrays.asList("city", "note"));
        SchemaCache cache = new SchemaCache(Arrays.asList(new PropertyKey("name"),
                                                        new PropertyKey("city"),
                                                        new PropertyKey("note")),
                                            Collections.singletonList(label), Collections.emptyList());
        // Use the existing allocation fixture pattern; no network client is constructed.
        RecordingGraph graph = allocate(RecordingGraph.class);
        HugeClient client = allocate(HugeClient.class);
        set(client, "graph", graph);
        LoadContext context = allocate(LoadContext.class);
        set(context, "options", options);
        set(context, "schemaCache", cache);
        set(context, "client", client);
        VertexBuilder builder = new VertexBuilder(context, struct, struct.vertices().get(0));
        Map<ElementBuilder, List<GraphElement>> builders = new HashMap<>();
        builders.put(builder, new ArrayList<>());
        HugeGraphSparkLoader.loadRow(struct, row, context, builders, options.checkVertex);
        Assert.assertTrue(builders.get(builder).isEmpty());
        return graph;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class)
                                    .invoke(field.get(null), type));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class RecordingGraph extends GraphManager {

        private int writes;
        private List<Vertex> vertices;

        private RecordingGraph() {
            super(null, null, "fixture");
        }

        @Override
        public List<Vertex> addVertices(List<Vertex> values) {
            this.writes++;
            this.vertices = new ArrayList<>(values);
            return values;
        }

        @Override
        public List<Edge> addEdges(List<Edge> values) {
            this.writes++;
            return values;
        }

        @Override
        public List<Vertex> updateVertices(BatchVertexRequest request) {
            this.writes++;
            return Collections.emptyList();
        }

        @Override
        public List<Edge> updateEdges(BatchEdgeRequest request) {
            this.writes++;
            return Collections.emptyList();
        }
    }
}
