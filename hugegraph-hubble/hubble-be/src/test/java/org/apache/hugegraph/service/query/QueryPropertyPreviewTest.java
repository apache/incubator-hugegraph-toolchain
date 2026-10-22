/*
 *
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

package org.apache.hugegraph.service.query;

import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.query.GremlinResult.Type;
import org.apache.hugegraph.entity.query.TypedResult;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryPropertyPreviewTest {

    @Test
    public void testLargePropertiesAreCopiedWithoutWritesOrDoubleTruncation() {
        String payload = "x".repeat(1024 * 1024);
        BigInteger id = new BigInteger("9007199254740993123456");
        Vertex original = new Vertex("person");
        original.id(id);
        original.property("payload", payload);
        original.property("number", id);
        GraphManager manager = Mockito.mock(GraphManager.class);
        original.attachManager(manager);
        GraphView graph = new GraphView(List.of(original), List.of());
        QueryPropertyPreview preview = new QueryPropertyPreview();

        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        Vertex graphCopy = preview.project(graph).getVertices().iterator().next();
        String expected = "x".repeat(256) + "… [preview 256/1048576 chars]";
        Assert.assertEquals(expected, copy.property("payload"));
        Assert.assertEquals(expected, graphCopy.property("payload"));
        Assert.assertEquals(payload, original.property("payload"));
        Assert.assertSame(id, copy.id());
        Assert.assertSame(id, copy.property("number"));
        Assert.assertTrue(preview.truncated());
        Mockito.verifyNoInteractions(manager);
    }

    @Test
    public void testPathIdentityLabelsAndEndpointsArePreserved() {
        String id = "id".repeat(200);
        Vertex vertex = new Vertex("label".repeat(100));
        vertex.id(id);
        Edge edge = new Edge("edge-label");
        edge.id("edge".repeat(100));
        edge.sourceId(id);
        edge.targetId(new BigInteger("9007199254740995"));
        edge.sourceLabel(vertex.label());
        edge.targetLabel("target-label");
        edge.property("payload", "x".repeat(1024 * 1024));
        Path path = new Path(id, List.of(vertex, edge));
        path.labels("start", "edge");

        QueryPropertyPreview preview = new QueryPropertyPreview();
        Path copy = (Path) preview.project(new TypedResult(Type.PATH, List.of(path))).getData().get(0);
        Vertex copiedVertex = (Vertex) copy.objects().get(0);
        Edge copiedEdge = (Edge) copy.objects().get(1);
        Assert.assertSame(id, copiedVertex.id());
        Assert.assertEquals(vertex.label(), copiedVertex.label());
        Assert.assertEquals(edge.id(), copiedEdge.id());
        Assert.assertSame(id, copiedEdge.sourceId());
        Assert.assertEquals(edge.targetId(), copiedEdge.targetId());
        Assert.assertEquals(edge.sourceLabel(), copiedEdge.sourceLabel());
        Assert.assertEquals(edge.targetLabel(), copiedEdge.targetLabel());
        Assert.assertEquals(path.labels(), copy.labels());
        Assert.assertSame(id, copy.crosspoint());
        Assert.assertTrue(((String) copiedEdge.property("payload")).endsWith("/1048576 chars]"));
        Assert.assertEquals(1024 * 1024, ((String) edge.property("payload")).length());
    }

    @Test
    public void testLongPathStopsBeforeReadingOmittedObjectsAndLabels() throws Exception {
        Path original = Mockito.mock(Path.class);
        List<Object> objects = new AbstractList<>() {
            @Override
            public Object get(int index) {
                Assert.assertTrue(index < QueryPropertyPreview.PATH_LIMIT);
                Vertex vertex = new Vertex("person");
                vertex.id(index);
                return vertex;
            }

            @Override
            public int size() {
                return 1000000;
            }
        };
        List<Object> labels = new AbstractList<>() {
            @Override
            public Object get(int index) {
                Assert.assertTrue(index < QueryPropertyPreview.PATH_LIMIT);
                return List.of("step" + index);
            }

            @Override
            public int size() {
                return 1000000;
            }
        };
        Mockito.when(original.objects()).thenReturn(objects);
        Mockito.when(original.labels()).thenReturn(labels);
        QueryPropertyPreview preview = new QueryPropertyPreview();
        TypedResult result = preview.project(new TypedResult(Type.PATH, List.of(original)));
        Path copy = (Path) result.getData().get(0);
        Assert.assertEquals(1000, copy.objects().size());
        Assert.assertEquals(1000, copy.labels().size());
        Assert.assertEquals(999, ((Vertex) copy.objects().get(999)).id());
        Assert.assertEquals(List.of("step999"), copy.labels().get(999));
        Assert.assertTrue(result.isTruncated());
        Assert.assertTrue(preview.truncated());
        Assert.assertEquals(1000000, original.objects().size());
        Assert.assertEquals(1000000, original.labels().size());
        String json = new ObjectMapper().writeValueAsString(copy);
        Assert.assertTrue(json.length() < 200000);
        Assert.assertEquals(1000, new ObjectMapper().readTree(json).get("objects").size());
        Assert.assertEquals(1000, new ObjectMapper().readTree(json).get("labels").size());
    }

    @Test
    public void testShortPathRetainsCollectionTypesAndLabelOrder() {
        Path original = new Path(List.of(List.of("Beijing", "Wuhan"), List.of(1L, 2L)));
        original.labels(List.of("start", "city"), List.of());
        QueryPropertyPreview preview = new QueryPropertyPreview();
        TypedResult result = preview.project(new TypedResult(Type.PATH, List.of(original)));
        Path copy = (Path) result.getData().get(0);
        Assert.assertEquals(original, copy);
        Assert.assertFalse(result.isTruncated());
        Assert.assertFalse(preview.truncated());
    }

    @Test
    public void testPathLabelsAndRepeatedPathsShareTheValueBudget() {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            objects.add(i);
        }
        Path original = new Path(objects);
        original.labels(objects.toArray());
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            rows.add(original);
        }
        QueryPropertyPreview preview = new QueryPropertyPreview();
        TypedResult result = preview.project(new TypedResult(Type.PATH, rows));
        int values = 0;
        for (Object row : result.getData()) {
            Path copy = (Path) row;
            values += copy.objects().size() + copy.labels().size();
        }
        Assert.assertTrue(values < QueryPropertyPreview.VALUE_LIMIT);
        Assert.assertTrue(((Path) result.getData().get(999)).objects().isEmpty());
        Assert.assertEquals(1000, original.objects().size());
        Assert.assertEquals(1000, original.labels().size());
        Assert.assertTrue(result.isTruncated());
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testPathDepthAndLabelsCannotResetNestedValueBudget() {
        Path original = new Path();
        original.objects(original);
        original.labels(List.of("x".repeat(1000000)));
        QueryPropertyPreview preview = new QueryPropertyPreview();
        TypedResult result = preview.project(new TypedResult(Type.PATH, List.of(original)));
        Path copy = (Path) result.getData().get(0);
        for (int depth = 0; depth < 4; depth++) {
            Assert.assertEquals(1, copy.objects().size());
            copy = (Path) copy.objects().get(0);
        }
        Assert.assertTrue(copy.objects().isEmpty());
        Assert.assertTrue(result.isTruncated());
        Assert.assertTrue(preview.truncated());
        Assert.assertSame(original, original.objects().get(0));
        Path outer = (Path) result.getData().get(0);
        Assert.assertTrue(((String) ((List<?>) outer.labels().get(0)).get(0))
                          .endsWith("/1000000 chars]"));
    }

    @Test
    public void testEnrichedGraphReusesDetachedObjectsWithoutChargingOrTruncatingTwice() {
        Vertex original = new Vertex("person");
        original.id(1);
        original.property("payload", "x".repeat(1000000));
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        Vertex graphCopy = preview.project(new GraphView(List.of(copy), List.of()))
                                  .getVertices().iterator().next();
        Assert.assertSame(copy.properties(), graphCopy.properties());
        Assert.assertEquals("x".repeat(256) + "… [preview 256/1000000 chars]",
                            graphCopy.property("payload"));
        Assert.assertEquals(1000000, ((String) original.property("payload")).length());
    }

    @Test
    public void testNestedGraphPropertiesCannotResetDepthOrMutateTheSource() {
        Vertex original = new Vertex("person");
        original.id(1);
        original.properties().put("self", original);
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        for (int depth = 0; depth < 4; depth++) {
            Assert.assertEquals(1, copy.id());
            copy = (Vertex) copy.property("self");
        }
        Assert.assertTrue(copy.properties().isEmpty());
        Assert.assertSame(original, original.property("self"));
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testLargeBinaryPropertiesUseAnExplicitSummary() {
        byte[] payload = new byte[1024 * 1024];
        Vertex original = new Vertex("person");
        original.id(1);
        original.property("binary", payload);
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        Assert.assertEquals("[binary preview: 1048576 bytes omitted]", copy.property("binary"));
        Assert.assertSame(payload, original.property("binary"));
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testUnicodeBoundaryDoesNotSplitSurrogatePair() {
        String text = "a".repeat(255) + "😀";
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Object value = preview.project(new TypedResult(Type.GENERAL, List.of(text))).getData().get(0);
        Assert.assertEquals("a".repeat(255) + "… [preview 255/257 chars]", value);
        Assert.assertTrue(preview.truncated());
        QueryPropertyPreview exact = new QueryPropertyPreview();
        Assert.assertEquals("a".repeat(256), exact.project(
                            new TypedResult(Type.GENERAL, List.of("a".repeat(256)))).getData().get(0));
        Assert.assertFalse(exact.truncated());
    }

    @Test
    public void testOversizedPropertyKeysAreOmittedWithoutChangingIdentity() {
        String longName = "key".repeat(100000);
        Vertex original = new Vertex(longName);
        original.id(longName);
        original.property(longName, "omitted");
        original.property("normal", 7L);
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        Assert.assertEquals(Map.of("normal", 7L), copy.properties());
        Assert.assertSame(longName, copy.id());
        Assert.assertSame(longName, copy.label());
        Assert.assertTrue(original.properties().containsKey(longName));
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testStructuredAndUnknownKeysAreOmittedWithoutStringifyingThem() throws Exception {
        Map<String, Object> structured = Map.of("payload", "x".repeat(1024 * 1024));
        Object unknown = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("Preview must not stringify an unknown key");
            }
        };
        Map<Object, Object> original = new LinkedHashMap<>();
        original.put(structured, 1L);
        original.put(unknown, 2L);
        original.put("normal", 3L);
        original.put(7, 4L);
        original.put(true, 5L);
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Object copy = preview.project(new TypedResult(Type.GENERAL, List.of(original))).getData().get(0);
        String json = new ObjectMapper().writeValueAsString(copy);

        Assert.assertEquals(Map.of("normal", 3L, 7, 4L, true, 5L), copy);
        Assert.assertTrue(json.length() < 100);
        Assert.assertTrue(preview.truncated());
        Assert.assertEquals(5, original.size());
        Assert.assertEquals(1024 * 1024, ((String) structured.get("payload")).length());
    }

    @Test
    public void testBranchingCollectionsShareOneValueBudget() {
        Object branch = 7L;
        for (int depth = 0; depth < 4; depth++) {
            Map<String, Object> parent = new LinkedHashMap<>();
            for (int i = 0; i < 20; i++) {
                parent.put("key" + i, branch);
            }
            branch = parent;
        }
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Object projected = preview.project(new TypedResult(Type.GENERAL, List.of(branch)))
                                  .getData().get(0);
        Assert.assertTrue(this.projectedValues(projected) <= QueryPropertyPreview.VALUE_LIMIT + 5);
        Assert.assertTrue(projected.toString().contains("value budget exhausted"));
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testBudgetIsSharedAcrossRowsAndGraphViewWithoutLosingIdentity() {
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            rows.add(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));
        }
        QueryPropertyPreview preview = new QueryPropertyPreview();
        List<Object> projected = preview.project(new TypedResult(Type.GENERAL, rows)).getData();
        int count = 0;
        for (Object row : projected) {
            count += this.projectedValues(row);
        }
        Assert.assertTrue(count <= QueryPropertyPreview.VALUE_LIMIT + rows.size());
        Assert.assertEquals("[preview: value budget exhausted]", projected.get(999));
        Vertex vertex = new Vertex("person");
        BigInteger id = new BigInteger("9007199254740993123456");
        vertex.id(id);
        vertex.property("payload", "still bounded");
        GraphView graph = preview.project(new GraphView(List.of(vertex), List.of()));
        Vertex copy = graph.getVertices().iterator().next();
        Assert.assertSame(id, copy.id());
        Assert.assertEquals("person", copy.label());
        Assert.assertEquals("[preview: value budget exhausted]", copy.property("payload"));
        Assert.assertEquals("still bounded", vertex.property("payload"));
        Assert.assertTrue(preview.truncated());
        QueryPropertyPreview nextQuery = new QueryPropertyPreview();
        Assert.assertEquals(7L, nextQuery.project(new TypedResult(Type.GENERAL, List.of(7L))).getData().get(0));
        Assert.assertFalse(nextQuery.truncated());
    }

    @Test
    public void testThousandVertexViewsDoNotChargeProjectedPropertiesTwice() {
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Vertex vertex = new Vertex("person");
            vertex.id(i);
            for (int property = 0; property < 10; property++) {
                vertex.property("key" + property, "value" + property);
            }
            rows.add(vertex);
        }
        QueryPropertyPreview preview = new QueryPropertyPreview();
        TypedResult typed = preview.project(new TypedResult(Type.VERTEX, rows));
        List<Vertex> vertices = new ArrayList<>();
        for (Object row : typed.getData()) {
            vertices.add((Vertex) row);
        }
        GraphView graph = new GraphView(vertices, List.of());
        Assert.assertNotSame(vertices.get(0), graph.getVertices().iterator().next());
        List<Vertex> graphVertices = new ArrayList<>(preview.project(graph).getVertices());
        Assert.assertEquals(1000, graphVertices.size());
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            Vertex graphVertex = graphVertices.get(i);
            Assert.assertEquals(vertex.id(), graphVertex.id());
            Assert.assertEquals(vertex.properties(), graphVertex.properties());
            Assert.assertSame(vertex.properties(), graphVertex.properties());
        }
        Assert.assertEquals(10, vertices.get(0).properties().size());
        Map<String, Object> lastProperties = vertices.get(999).properties();
        Assert.assertEquals(1, lastProperties.size());
        Assert.assertEquals("[preview: value budget exhausted]", lastProperties.values().iterator().next());
        Assert.assertEquals(lastProperties, graphVertices.get(999).properties());
        Assert.assertEquals(10, ((Vertex) rows.get(999)).properties().size());
        List<String> partialIds = preview.partialElementIds(graphVertices);
        Assert.assertTrue(partialIds.size() > 1);
        Assert.assertFalse(partialIds.contains("0"));
        Assert.assertTrue(partialIds.contains("999"));
        Assert.assertTrue(preview.truncated());
    }

    @Test
    public void testRepeatedGraphObjectsCannotReuseAnUnchargedPropertyTree() {
        Map<String, Object> branch = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            branch.put("key" + i, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        }
        Vertex vertex = new Vertex("person");
        vertex.id(1L);
        vertex.properties().put("branch", branch);
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            rows.add(vertex);
        }
        QueryPropertyPreview preview = new QueryPropertyPreview();
        List<Object> copies = preview.project(new TypedResult(Type.VERTEX, rows)).getData();
        int count = 0;
        for (Object value : copies) {
            Vertex copy = (Vertex) value;
            Assert.assertEquals(1L, copy.id());
            count += this.projectedValues(copy.property("branch"));
        }
        Assert.assertTrue(count <= QueryPropertyPreview.VALUE_LIMIT + rows.size() + 5);
        Assert.assertEquals("[preview: value budget exhausted]",
                            ((Vertex) copies.get(999)).property("branch"));
        Assert.assertTrue(preview.truncated());
    }

    private int projectedValues(Object value) {
        int count = 1;
        if (value instanceof Map) {
            for (Object nested : ((Map<?, ?>) value).values()) {
                count += this.projectedValues(nested);
            }
        } else if (value instanceof List) {
            for (Object nested : (List<?>) value) {
                count += this.projectedValues(nested);
            }
        }
        return count;
    }

    @Test
    public void testNestedCollectionsAndScalarStringsAreBounded() {
        Vertex original = new Vertex("person");
        original.id(1);
        original.properties().put("nested", List.of(Map.of("text", "x".repeat(100000))));
        QueryPropertyPreview preview = new QueryPropertyPreview();
        Vertex copy = (Vertex) preview.project(new TypedResult(Type.VERTEX, List.of(original)))
                                     .getData().get(0);
        Map<?, ?> nested = (Map<?, ?>) ((List<?>) copy.property("nested")).get(0);
        Assert.assertTrue(((String) nested.get("text")).endsWith("/100000 chars]"));
        Assert.assertEquals(100000, ((String) ((Map<?, ?>) ((List<?>) original.property("nested"))
                                               .get(0)).get("text")).length());
        List<Object> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            many.add(i);
        }
        List<?> bounded = (List<?>) preview.project(new TypedResult(Type.GENERAL, List.of(many)))
                                          .getData().get(0);
        Assert.assertEquals(20, bounded.size());
        Assert.assertTrue(preview.truncated());
    }
}
