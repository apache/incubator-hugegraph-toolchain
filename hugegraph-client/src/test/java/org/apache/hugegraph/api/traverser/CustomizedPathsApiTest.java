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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.traverser.CustomizedPathsRequest;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CustomizedPathsApiTest extends TraverserApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initVertex();
    }

    @Override
    @Before
    public void setup() {
        initEdgesWithWeights();
    }

    @Override
    @After
    public void teardown() {
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsSourceLabelProperty() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
               .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, rippleId);
        Assert.assertEquals(path1, paths.get(0).objects());
        Assert.assertEquals(path2, paths.get(1).objects());

        List<Double> weights1 = ImmutableList.of(1.0D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1.0D, 1.0D);
        Assert.assertEquals(weights1, paths.get(0).weights());
        Assert.assertEquals(weights2, paths.get(1).weights());

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(4, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId,
                                                  rippleId, joshId);
        Assert.assertEquals(expectedVertices, vertices);
    }

    @Test
    public void testCustomizedPathsSourceIds() {
        Object markoId = getVertexId("person", "name", "marko");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().ids(markoId, peterId);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(peterId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        List<Double> weights1 = ImmutableList.of(0.2D);
        List<Double> weights2 = ImmutableList.of(0.4D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));
        Assert.assertTrue(expectedWeights.contains(paths.get(1).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId, peterId);
        Assert.assertEquals(expectedVertices, vertices);
    }

    @Test
    public void testCustomizedPathsSourceLabelPropertyMultiValue() {
        Object markoId = getVertexId("person", "name", "marko");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        List<String> names = ImmutableList.of("marko", "peter");
        builder.sources().label("person").property("name", names);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(peterId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        List<Double> weights1 = ImmutableList.of(0.2D);
        List<Double> weights2 = ImmutableList.of(0.4D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));
        Assert.assertTrue(expectedWeights.contains(paths.get(1).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId, peterId);
        Assert.assertEquals(expectedVertices, vertices);
    }

    @Test
    public void testCustomizedPathsWithSample() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
               .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1).sample(1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(1, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));

        List<Double> weights1 = ImmutableList.of(1D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1D, 1D);

        Assert.assertTrue(weights1.equals(paths.get(0).weights()) ||
                          weights2.equals(paths.get(0).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Assert.assertTrue(path1.containsAll(vertices) ||
                          path2.containsAll(vertices));
    }

    @Test
    public void testCustomizedPathsWithDecr() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
               .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.DECR).withVertex(true)
               .capacity(-1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, lopId);
        Assert.assertEquals(path1, paths.get(0).objects());
        Assert.assertEquals(path2, paths.get(1).objects());

        List<Double> weights1 = ImmutableList.of(1.0D, 1.0D);
        List<Double> weights2 = ImmutableList.of(1.0D, 0.4D);
        Assert.assertEquals(weights1, paths.get(0).weights());
        Assert.assertEquals(weights2, paths.get(1).weights());

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(4, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId,
                                                  rippleId, joshId);
        Assert.assertEquals(expectedVertices, vertices);
    }

    @Test
    public void testCustomizedPathsWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
               .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(1);
        CustomizedPathsRequest request = builder.build();

        PathsWithVertices customizedPaths = customizedPathsAPI.post(request);
        List<PathsWithVertices.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(1, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, rippleId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));

        List<Double> weights1 = ImmutableList.of(1.0D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1.0D, 1.0D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> vertices1 = ImmutableSet.of(markoId, lopId, joshId);
        Set<?> vertices2 = ImmutableSet.of(markoId, lopId, rippleId);
        Set<Set<?>> expectedVertices = ImmutableSet.of(vertices1, vertices2);
        Assert.assertTrue(expectedVertices.contains(vertices));
    }

    @Test
    public void testCustomizedPathsWithCapacity() {
        CustomizedPathsRequest.Builder builder = CustomizedPathsRequest.builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
               .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
               .weightBy("weight").degree(-1);
        builder.sortBy(CustomizedPathsRequest.SortBy.INCR).withVertex(true)
               .capacity(1).limit(-1);
        CustomizedPathsRequest request = builder.build();

        Assert.assertThrows(ServerException.class, () -> {
            customizedPathsAPI.post(request);
        }, e -> {
            String expect = "Exceed capacity '1' while finding customized paths";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    private static void initEdgesWithWeights() {
        SchemaManager schema = schema();
        schema.edgeLabel("knows1")
              .sourceLabel("person")
              .targetLabel("person")
              .properties("date", "weight")
              .nullableKeys("weight")
              .ifNotExist()
              .create();

        schema.edgeLabel("created1")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "weight")
              .nullableKeys("weight")
              .ifNotExist()
              .create();

        Vertex marko = getVertex("person", "name", "marko");
        Vertex vadas = getVertex("person", "name", "vadas");
        Vertex lop = getVertex("software", "name", "lop");
        Vertex josh = getVertex("person", "name", "josh");
        Vertex ripple = getVertex("software", "name", "ripple");
        Vertex peter = getVertex("person", "name", "peter");

        marko.addEdge("knows1", vadas, "date", "2016-01-10", "weight", 0.5);
        marko.addEdge("knows1", josh, "date", "2013-02-20", "weight", 1.0);
        marko.addEdge("created1", lop, "date", "2017-12-10", "weight", 0.4);
        josh.addEdge("created1", lop, "date", "2009-11-11", "weight", 0.4);
        josh.addEdge("created1", ripple, "date", "2017-12-10", "weight", 1.0);
        peter.addEdge("created1", lop, "date", "2017-03-24", "weight", 0.2);
    }

    private static void removeEdgesWithWeights() {
        List<Long> elTaskIds = new ArrayList<>();
        EdgeLabel knows1 = schema().getEdgeLabel("knows1");
        EdgeLabel created1 = schema().getEdgeLabel("created1");
        ImmutableList.of(knows1, created1).forEach(edgeLabel -> {
            elTaskIds.add(edgeLabelAPI.delete(edgeLabel.name()));
        });
        elTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
    }
}
