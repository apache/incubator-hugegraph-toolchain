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

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.Ranks;
import org.apache.hugegraph.testutil.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class NeighborRankApiTest extends TraverserApiTest {

    @BeforeClass
    public static void initNeighborRankGraph() {
        GraphManager graph = graph();
        SchemaManager schema = schema();

        schema.propertyKey("name").asText().ifNotExist().create();

        schema.vertexLabel("person")
              .properties("name")
              .useCustomizeStringId()
              .ifNotExist()
              .create();

        schema.vertexLabel("movie")
              .properties("name")
              .useCustomizeStringId()
              .ifNotExist()
              .create();

        schema.edgeLabel("follow")
              .sourceLabel("person")
              .targetLabel("person")
              .ifNotExist()
              .create();

        schema.edgeLabel("like")
              .sourceLabel("person")
              .targetLabel("movie")
              .ifNotExist()
              .create();

        schema.edgeLabel("directedBy")
              .sourceLabel("movie")
              .targetLabel("person")
              .ifNotExist()
              .create();

        Vertex O = graph.addVertex(T.LABEL, "person", T.ID, "O", "name", "O");

        Vertex A = graph.addVertex(T.LABEL, "person", T.ID, "A", "name", "A");
        Vertex B = graph.addVertex(T.LABEL, "person", T.ID, "B", "name", "B");
        Vertex C = graph.addVertex(T.LABEL, "person", T.ID, "C", "name", "C");
        Vertex D = graph.addVertex(T.LABEL, "person", T.ID, "D", "name", "D");

        Vertex E = graph.addVertex(T.LABEL, "movie", T.ID, "E", "name", "E");
        Vertex F = graph.addVertex(T.LABEL, "movie", T.ID, "F", "name", "F");
        Vertex G = graph.addVertex(T.LABEL, "movie", T.ID, "G", "name", "G");
        Vertex H = graph.addVertex(T.LABEL, "movie", T.ID, "H", "name", "H");
        Vertex I = graph.addVertex(T.LABEL, "movie", T.ID, "I", "name", "I");
        Vertex J = graph.addVertex(T.LABEL, "movie", T.ID, "J", "name", "J");

        Vertex K = graph.addVertex(T.LABEL, "person", T.ID, "K", "name", "K");
        Vertex L = graph.addVertex(T.LABEL, "person", T.ID, "L", "name", "L");
        Vertex M = graph.addVertex(T.LABEL, "person", T.ID, "M", "name", "M");

        O.addEdge("follow", A);
        O.addEdge("follow", B);
        O.addEdge("follow", C);
        D.addEdge("follow", O);

        A.addEdge("follow", B);
        A.addEdge("like", E);
        A.addEdge("like", F);

        B.addEdge("like", G);
        B.addEdge("like", H);

        C.addEdge("like", I);
        C.addEdge("like", J);

        E.addEdge("directedBy", K);
        F.addEdge("directedBy", B);
        F.addEdge("directedBy", L);

        G.addEdge("directedBy", M);
    }

    @AfterClass
    public static void clearNeighborRankGraph() {
        List<Long> taskIds = new ArrayList<>();
        taskIds.add(edgeLabelAPI.delete("directedBy"));
        taskIds.add(edgeLabelAPI.delete("like"));
        taskIds.add(edgeLabelAPI.delete("follow"));
        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
        taskIds.clear();
        taskIds.add(vertexLabelAPI.delete("movie"));
        taskIds.add(vertexLabelAPI.delete("person"));
        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
    }

    @Test
    public void testNeighborRank() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.alpha(0.9).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.4305D, "A", 0.3D, "C", 0.3D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.builder()
                                        .put("G", 0.17550000000000002D)
                                        .put("H", 0.17550000000000002D)
                                        .put("I", 0.135D)
                                        .put("J", 0.135D)
                                        .put("E", 0.09000000000000001D)
                                        .put("F", 0.09000000000000001D)
                                        .build(),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("M", 0.15795D,
                                            "K", 0.08100000000000002D,
                                            "L", 0.04050000000000001D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithOtherAlpha() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.steps().direction(Direction.OUT).degree(-1).top(10);
        builder.alpha(1.0).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.5D,
                                            "A", 0.3333333333333333D,
                                            "C", 0.3333333333333333D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.builder()
                                        .put("G", 0.2222222222222222D)
                                        .put("H", 0.2222222222222222D)
                                        .put("I", 0.16666666666666666D)
                                        .put("J", 0.16666666666666666D)
                                        .put("E", 0.1111111111111111D)
                                        .put("F", 0.1111111111111111D)
                                        .build(),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("M", 0.2222222222222222D,
                                            "K", 0.1111111111111111D,
                                            "L", 0.05555555555555555D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithDirection() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.BOTH);
        builder.steps().direction(Direction.IN);
        builder.steps().direction(Direction.OUT);
        builder.alpha(0.9).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("A", 0.32625000000000004D,
                                            "B", 0.27056250000000004D,
                                            "C", 0.225D,
                                            "D", 0.225D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.of("F", 0.10125D),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("L", 0.045562500000000006D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithLabels() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().labels("follow").direction(Direction.OUT);
        builder.steps().labels("like").direction(Direction.OUT);
        builder.steps().labels("directedBy").direction(Direction.OUT);
        builder.alpha(0.9).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.36075D,
                                            "A", 0.3D,
                                            "C", 0.3D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.builder()
                                        .put("E", 0.135)
                                        .put("F", 0.135)
                                        .put("G", 0.135)
                                        .put("H", 0.135)
                                        .put("I", 0.135)
                                        .put("J", 0.135)
                                        .build(),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("K", 0.12150000000000001D,
                                            "M", 0.12150000000000001D,
                                            "L", 0.060750000000000005D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithTop() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT).degree(-1).top(2);
        builder.steps().direction(Direction.OUT).degree(-1).top(3);
        builder.steps().direction(Direction.OUT).degree(-1).top(2);
        builder.alpha(0.9).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.4305D, "A", 0.3D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.of("G", 0.17550000000000002D,
                                            "H", 0.17550000000000002D,
                                            "I", 0.135D),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("M", 0.15795D,
                                            "K", 0.08100000000000002D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithDegree() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT).degree(2);
        builder.steps().direction(Direction.OUT).degree(1);
        builder.steps().direction(Direction.OUT).degree(1);
        builder.alpha(0.9).capacity(-1);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.855D, "A", 0.45D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.of("G", 0.7695D), ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("M", 0.69255D), ranks.get(3));

        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT).degree(2);
        builder.steps().direction(Direction.OUT).degree(2);
        builder.steps().direction(Direction.OUT).degree(1);
        builder.alpha(0.9).capacity(-1);
        request = builder.build();

        ranks = neighborRankAPI.post(request);
        Assert.assertEquals(4, ranks.size());
        Assert.assertEquals(ImmutableMap.of("O", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of("B", 0.6525000000000001D,
                                            "A", 0.45D),
                            ranks.get(1));
        Assert.assertEquals(ImmutableMap.of("G", 0.293625D,
                                            "H", 0.293625D,
                                            "E", 0.2025D),
                            ranks.get(2));
        Assert.assertEquals(ImmutableMap.of("M", 0.2642625D,
                                            "K", 0.18225000000000002D),
                            ranks.get(3));
    }

    @Test
    public void testNeighborRankWithCapacity() {
        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("O");
        builder.steps().direction(Direction.OUT);
        builder.steps().direction(Direction.OUT);
        builder.steps().direction(Direction.OUT);
        builder.alpha(0.9).capacity(1);
        NeighborRankAPI.Request request = builder.build();

        Assert.assertThrows(ServerException.class, () -> {
            neighborRankAPI.post(request);
        }, e -> {
            String expect = "Exceed capacity '1' while finding neighbor rank";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testNeighborRankWithIsolatedVertex() {
        Vertex isolate = graph().addVertex(T.LABEL, "person", T.ID, "isolate",
                                           "name", "isolate-vertex");

        NeighborRankAPI.Request.Builder builder;
        builder = NeighborRankAPI.Request.builder();
        builder.source("isolate").alpha(0.9);
        builder.steps().direction(Direction.BOTH);
        NeighborRankAPI.Request request = builder.build();

        List<Ranks> ranks = neighborRankAPI.post(request);
        Assert.assertEquals(2, ranks.size());
        Assert.assertEquals(ImmutableMap.of("isolate", 1.0D), ranks.get(0));
        Assert.assertEquals(ImmutableMap.of(), ranks.get(1));

        graph().removeVertex(isolate.id());
    }

    @Test
    public void testNeighborRankWithInvalidParams() {
        // Invalid source
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.source(null);
        });

        // Invalid degree
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.steps().degree(-2);
        });

        // Invalid top
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.steps().top(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.steps().top(1001);
        });

        // Invalid alpha
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.alpha(0.0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.alpha(1.1);
        });

        // Invalid capacity
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.capacity(-2);
        });

        // Without steps
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            NeighborRankAPI.Request.Builder builder;
            builder = NeighborRankAPI.Request.builder();
            builder.source("A");
            builder.build();
        });
    }
}
