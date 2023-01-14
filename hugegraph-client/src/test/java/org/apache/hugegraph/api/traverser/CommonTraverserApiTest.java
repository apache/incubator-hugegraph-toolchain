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

import static org.apache.hugegraph.structure.constant.Traverser.DEFAULT_PAGE_LIMIT;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Edges;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.graph.Vertices;
import org.apache.hugegraph.structure.traverser.CrosspointsRequest;
import org.apache.hugegraph.structure.traverser.CustomizedCrosspoints;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CommonTraverserApiTest extends TraverserApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initIndexLabel();
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testCrosspoints() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        List<Path> paths = crosspointsAPI.get(markoId, peterId, Direction.OUT,
                                              null, 3, -1L, -1L, 10);
        Assert.assertEquals(2, paths.size());
        Path crosspoint1 = new Path(lopId,
                                    ImmutableList.of(markoId, lopId, peterId));
        Path crosspoint2 = new Path(lopId, ImmutableList.of(markoId, joshId,
                                                            lopId, peterId));

        List<Path> crosspoints = ImmutableList.of(crosspoint1, crosspoint2);
        Assert.assertTrue(crosspoints.contains(paths.get(0)));
        Assert.assertTrue(crosspoints.contains(paths.get(1)));
    }

    @Test
    public void testCrosspointsWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");
        Object peterId = getVertexId("person", "name", "peter");

        Assert.assertThrows(ServerException.class, () -> {
            crosspointsAPI.get(markoId, peterId, Direction.OUT,
                               null, 3, -1L, 2L, 10);
        }, e -> {
            String expect = "Exceed capacity '2' while finding paths";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testCustomizedCrosspoints() {
        Object lopId = getVertexId("software", "name", "lop");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        CrosspointsRequest.Builder builder = CrosspointsRequest.builder();
        builder.sources().ids(lopId, rippleId);
        builder.pathPatterns().steps().direction(Direction.IN)
                                      .labels("created").degree(-1);
        builder.withPath(true).withVertex(true).capacity(-1).limit(-1);

        CustomizedCrosspoints customizedCrosspoints =
                              customizedCrosspointsAPI.post(builder.build());
        List<Object> crosspoints = customizedCrosspoints.crosspoints();
        Assert.assertEquals(1, crosspoints.size());
        Assert.assertEquals(joshId, crosspoints.get(0));

        List<Path> paths = customizedCrosspoints.paths();
        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(rippleId, joshId);
        List<Object> path2 = ImmutableList.of(lopId, joshId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        Set<?> vertices = customizedCrosspoints.vertices().stream()
                                               .map(Vertex::id)
                                               .collect(Collectors.toSet());
        List<Object> expectedVids = ImmutableList.of(rippleId, joshId, lopId);
        Assert.assertTrue(expectedVids.containsAll(vertices));
    }

    @Test
    public void testVertices() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Object> ids = ImmutableList.of(markoId, vadasId, joshId,
                                            peterId, lopId, rippleId);
        List<Vertex> vertices = verticesAPI.list(ids);

        Assert.assertEquals(6, vertices.size());

        Assert.assertEquals(markoId, vertices.get(0).id());
        Assert.assertEquals(vadasId, vertices.get(1).id());
        Assert.assertEquals(joshId, vertices.get(2).id());
        Assert.assertEquals(peterId, vertices.get(3).id());
        Assert.assertEquals(lopId, vertices.get(4).id());
        Assert.assertEquals(rippleId, vertices.get(5).id());

        Map<String, Object> props = ImmutableMap.of("name", "josh",
                                                    "city", "Beijing",
                                                    "age", 32);
        Assert.assertEquals(props, vertices.get(2).properties());
    }

    @Test
    public void testEdges() {
        String date2012Id = getEdgeId("knows", "date", "2012-01-10");
        String date2013Id = getEdgeId("knows", "date", "2013-01-10");
        String date2014Id = getEdgeId("created", "date", "2014-01-10");
        String date2015Id = getEdgeId("created", "date", "2015-01-10");
        String date2016Id = getEdgeId("created", "date", "2016-01-10");
        String date2017Id = getEdgeId("created", "date", "2017-01-10");

        List<String> ids = ImmutableList.of(date2012Id, date2013Id, date2014Id,
                                            date2015Id, date2016Id, date2017Id);

        List<Edge> edges = edgesAPI.list(ids);

        Assert.assertEquals(6, edges.size());

        Assert.assertEquals(date2012Id, edges.get(0).id());
        Assert.assertEquals(date2013Id, edges.get(1).id());
        Assert.assertEquals(date2014Id, edges.get(2).id());
        Assert.assertEquals(date2015Id, edges.get(3).id());
        Assert.assertEquals(date2016Id, edges.get(4).id());
        Assert.assertEquals(date2017Id, edges.get(5).id());

        String date = Utils.formatDate("2014-01-10");
        Map<String, Object> props = ImmutableMap.of("date", date,
                                                    "city", "Shanghai");
        Assert.assertEquals(props, edges.get(2).properties());
    }

    @Test
    public void testScanVertex() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        List<Vertex> vertices = new LinkedList<>();
        for (Shard shard : shards) {
            Vertices results = verticesAPI.scan(shard, null, 0L);
            vertices.addAll(ImmutableList.copyOf(results.results()));
            Assert.assertNull(results.page());
        }
        Assert.assertEquals(6, vertices.size());
    }

    @Test
    public void testScanVertexInPaging() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        List<Vertex> vertices = new LinkedList<>();
        for (Shard shard : shards) {
            String page = "";
            while (page != null) {
                Vertices results = verticesAPI.scan(shard, page, DEFAULT_PAGE_LIMIT);
                vertices.addAll(ImmutableList.copyOf(results.results()));
                page = results.page();
            }
        }
        Assert.assertEquals(6, vertices.size());
    }

    @Test
    public void testScanVertexInPagingWithNegativeLimit() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        for (Shard shard : shards) {
            String page = "";
            Assert.assertThrows(ServerException.class, () -> {
                verticesAPI.scan(shard, page, -1);
            }, e -> {
                Assert.assertContains("Invalid limit -1", e.getMessage());
            });
        }
    }

    @Test
    public void testScanVertexWithSplitSizeLt1MB() {
        Assert.assertThrows(ServerException.class, () -> {
            verticesAPI.shards(1 * 1024 * 1024 - 1);
        }, e -> {
            String expect = "The split-size must be >= 1048576 bytes, " +
                            "but got 1048575";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testScanEdge() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        List<Edge> edges = new LinkedList<>();
        for (Shard shard : shards) {
            Edges results = edgesAPI.scan(shard, null, 0L);
            Assert.assertNull(results.page());
            edges.addAll(ImmutableList.copyOf(results.results()));
        }
        Assert.assertEquals(6, edges.size());
    }

    @Test
    public void testScanEdgeInPaging() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        List<Edge> edges = new LinkedList<>();
        for (Shard shard : shards) {
            String page = "";
            while (page != null) {
                Edges results = edgesAPI.scan(shard, page, DEFAULT_PAGE_LIMIT);
                edges.addAll(ImmutableList.copyOf(results.results()));
                page = results.page();
            }
        }
        Assert.assertEquals(6, edges.size());
    }

    @Test
    public void testScanEdgeInPagingWithNegativeLimit() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        for (Shard shard : shards) {
            String page = "";
            Assert.assertThrows(ServerException.class, () -> {
                edgesAPI.scan(shard, page, -1);
            }, e -> {
                String expect = "Invalid limit -1";
                Assert.assertContains(expect, e.getMessage());
            });
        }
    }

    @Test
    public void testScanEdgeWithSplitSizeLt1MB() {
        Assert.assertThrows(ServerException.class, () -> {
            edgesAPI.shards(1 * 1024 * 1024 - 1);
        });
    }
}
