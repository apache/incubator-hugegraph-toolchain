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
import java.util.ArrayList;
import java.util.List;
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
