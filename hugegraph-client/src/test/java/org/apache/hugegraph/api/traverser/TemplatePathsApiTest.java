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

package org.apache.hugegraph.api.traverser;

import java.util.List;
import java.util.Set;

import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.structure.traverser.TemplatePathsRequest;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TemplatePathsApiTest extends TraverserApiTest {

    @BeforeClass
    public static void initShortestPathGraph() {
        schema().propertyKey("weight")
                .asDouble()
                .ifNotExist()
                .create();

        schema().vertexLabel("node")
                .useCustomizeNumberId()
                .ifNotExist()
                .create();

        schema().edgeLabel("link")
                .sourceLabel("node").targetLabel("node")
                .ifNotExist()
                .create();
        schema().edgeLabel("relateTo")
                .sourceLabel("node").targetLabel("node")
                .properties("weight")
                .ifNotExist()
                .create();

        Vertex v1 = graph().addVertex(T.LABEL, "node", T.ID, 1);
        Vertex v2 = graph().addVertex(T.LABEL, "node", T.ID, 2);
        Vertex v3 = graph().addVertex(T.LABEL, "node", T.ID, 3);
        Vertex v4 = graph().addVertex(T.LABEL, "node", T.ID, 4);
        Vertex v5 = graph().addVertex(T.LABEL, "node", T.ID, 5);
        Vertex v6 = graph().addVertex(T.LABEL, "node", T.ID, 6);
        Vertex v7 = graph().addVertex(T.LABEL, "node", T.ID, 7);
        Vertex v8 = graph().addVertex(T.LABEL, "node", T.ID, 8);
        Vertex v9 = graph().addVertex(T.LABEL, "node", T.ID, 9);
        Vertex v10 = graph().addVertex(T.LABEL, "node", T.ID, 10);
        Vertex v11 = graph().addVertex(T.LABEL, "node", T.ID, 11);
        Vertex v12 = graph().addVertex(T.LABEL, "node", T.ID, 12);
        Vertex v13 = graph().addVertex(T.LABEL, "node", T.ID, 13);
        Vertex v14 = graph().addVertex(T.LABEL, "node", T.ID, 14);
        Vertex v15 = graph().addVertex(T.LABEL, "node", T.ID, 15);
        Vertex v16 = graph().addVertex(T.LABEL, "node", T.ID, 16);
        Vertex v17 = graph().addVertex(T.LABEL, "node", T.ID, 17);

        v1.addEdge("link", v2);
        v2.addEdge("link", v3);
        v3.addEdge("link", v4);
        v4.addEdge("link", v5);
        v5.addEdge("link", v6);
        v6.addEdge("link", v7);
        v8.addEdge("link", v7);
        v9.addEdge("link", v8);
        v10.addEdge("link", v9);

        v1.addEdge("link", v11);
        v11.addEdge("link", v12);
        v12.addEdge("link", v13);
        v13.addEdge("link", v14);
        v10.addEdge("link", v15);
        v15.addEdge("link", v14);

        v1.addEdge("link", v16);
        v16.addEdge("link", v17);
        v10.addEdge("link", v17);

        v1.addEdge("relateTo", v16, "weight", 0.8D);
        v16.addEdge("relateTo", v17, "weight", 0.5D);
        v10.addEdge("relateTo", v17, "weight", 0.6D);
        v17.addEdge("relateTo", v16, "weight", 0.3D);
    }

    @Test
    public void testTemplatePaths() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).maxTimes(3);
        builder.steps().direction(Direction.OUT).maxTimes(3);
        builder.steps().direction(Direction.IN).maxTimes(3);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(3, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                ImmutableList.of(1, 11, 12, 13, 14, 15, 10),
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testTemplatePathsWithVertex() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo").maxTimes(3);
        builder.withVertex(true);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        List<Object> expectedIds = ImmutableList.of(1, 16, 17, 10);
        List<List<Object>> expectedPath = ImmutableList.of(expectedIds);
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expectedPath.contains(path.objects()));
        }
        Set<Vertex> vertices = pathsWithVertices.vertices();
        Assert.assertEquals(4, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertTrue(expectedIds.contains(v.id()));
        }
    }

    @Test
    public void testTemplatePathsWithLabel() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("link").maxTimes(3);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(3, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                ImmutableList.of(1, 11, 12, 13, 14, 15, 10),
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo").maxTimes(3);
        request = builder.build();
        pathsWithVertices = templatePathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testTemplatePathsWithRing() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo").maxTimes(3);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo").maxTimes(3);
        builder.withRing(true);
        request = builder.build();
        pathsWithVertices = templatePathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(4, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 16, 17, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testTemplatePathsWithProperties() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo").maxTimes(3);
        builder.withRing(true);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(4, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 16, 17, 10),
                ImmutableList.of(1, 16, 17, 16, 17, 16, 17, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("relateTo")
               .properties("weight", "P.gt(0.4)").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("relateTo")
               .properties("weight", "P.gt(0.4)").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("relateTo")
               .properties("weight", "P.gt(0.4)").maxTimes(3);
        builder.withRing(true);
        request = builder.build();
        pathsWithVertices = templatePathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testTemplatePathsWithLimit() {
        TemplatePathsRequest.Builder builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("link").maxTimes(3);
        TemplatePathsRequest request = builder.build();
        PathsWithVertices pathsWithVertices = templatePathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(3, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                ImmutableList.of(1, 11, 12, 13, 14, 15, 10),
                ImmutableList.of(1, 16, 17, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = TemplatePathsRequest.builder();
        builder.sources().ids(1);
        builder.targets().ids(10);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.OUT).labels("link").maxTimes(3);
        builder.steps().direction(Direction.IN).labels("link").maxTimes(3);
        builder.limit(2);
        request = builder.build();
        pathsWithVertices = templatePathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(1, 16, 17, 10),
                ImmutableList.of(1, 11, 12, 13, 14, 15, 10)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }
}
