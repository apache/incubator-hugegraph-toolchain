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

import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.SingleSourceJaccardSimilarityRequest;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class JaccardSimilarityApiTest extends TraverserApiTest {

    @BeforeClass
    public static void initShortestPathGraph() {
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

        v1.addEdge("link", v3);
        v2.addEdge("link", v3);
        v4.addEdge("link", v1);
        v4.addEdge("link", v2);

        v1.addEdge("relateTo", v5);
        v2.addEdge("relateTo", v5);
        v6.addEdge("relateTo", v1);
        v6.addEdge("relateTo", v2);

        v1.addEdge("link", v7);
        v8.addEdge("link", v1);
        v2.addEdge("link", v9);
        v10.addEdge("link", v2);
    }

    @Test
    public void testJaccardSimilarity() {
        double jaccard = jaccardSimilarityAPI.get(1, 2, Direction.BOTH,
                                                  null, -1);
        Assert.assertEquals(0.5D, jaccard, Double.MIN_VALUE);
    }

    @Test
    public void testJaccardSimilarityWithDirection() {
        double jaccard = jaccardSimilarityAPI.get(1, 2, Direction.OUT,
                                                      null, -1);
        Assert.assertEquals(0.5, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.IN,
                                         null, -1);
        Assert.assertEquals(0.5, jaccard, Double.MIN_VALUE);
    }

    @Test
    public void testJaccardSimilarityWithLabel() {
        double jaccard = jaccardSimilarityAPI.get(1, 2, Direction.BOTH,
                                                  "link", -1);
        Assert.assertEquals(0.3333333333333333D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.OUT,
                                           "link", -1);
        Assert.assertEquals(0.3333333333333333D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.IN,
                                         "link", -1);
        Assert.assertEquals(0.3333333333333333D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.BOTH,
                                         "relateTo", -1);
        Assert.assertEquals(1.0D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.OUT,
                                         "relateTo", -1);
        Assert.assertEquals(1.0D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.IN,
                                         "relateTo", -1);
        Assert.assertEquals(1.0D, jaccard, Double.MIN_VALUE);
    }

    @Test
    public void testJaccardSimilarityWithDegree() {
        double jaccard = jaccardSimilarityAPI.get(1, 2, Direction.OUT,
                                                  null, 6);
        Assert.assertEquals(0.5D, jaccard, Double.MIN_VALUE);

        jaccard = jaccardSimilarityAPI.get(1, 2, Direction.OUT,
                                         null, 1);
        Assert.assertEquals(1D, jaccard, Double.MIN_VALUE);
    }

    @Test
    public void testJaccardSimilar() {
        SingleSourceJaccardSimilarityRequest.Builder builder =
                SingleSourceJaccardSimilarityRequest.builder();
        builder.vertex(4);
        builder.step().direction(Direction.BOTH);
        SingleSourceJaccardSimilarityRequest request = builder.build();
        Map<Object, Double> results = jaccardSimilarityAPI.post(request);

        Assert.assertEquals(9, results.size());
        Set<Object> expected = ImmutableSet.of("1", "2", "3", "5", "6",
                                               "7", "8", "9", "10");
        Assert.assertEquals(expected, results.keySet());

        Assert.assertEquals(1.0, results.get("3"));
        Assert.assertEquals(1.0, results.get("5"));
        Assert.assertEquals(1.0, results.get("6"));
        Assert.assertEquals(0.5, results.get("7"));
        Assert.assertEquals(0.5, results.get("8"));
        Assert.assertEquals(0.5, results.get("9"));
        Assert.assertEquals(0.5, results.get("10"));
        Assert.assertEquals(0.0, results.get("1"));
        Assert.assertEquals(0.0, results.get("2"));
    }

    @Test
    public void testJaccardSimilarWithTop() {
        SingleSourceJaccardSimilarityRequest.Builder builder =
                SingleSourceJaccardSimilarityRequest.builder();
        builder.vertex(4);
        builder.step().direction(Direction.BOTH);
        builder.top(5);
        SingleSourceJaccardSimilarityRequest request = builder.build();
        Map<Object, Double> results = jaccardSimilarityAPI.post(request);

        Assert.assertEquals(5, results.size());
        Set<Object> expected = ImmutableSet.of("3", "5", "6", "7", "8");
        Assert.assertEquals(expected, results.keySet());

        Assert.assertEquals(1.0, results.get("3"));
        Assert.assertEquals(1.0, results.get("5"));
        Assert.assertEquals(1.0, results.get("6"));
        Assert.assertEquals(0.5, results.get("7"));
        Assert.assertEquals(0.5, results.get("8"));
    }

    @Test
    public void testJaccardSimilarWithLabel() {
        SingleSourceJaccardSimilarityRequest.Builder builder =
                SingleSourceJaccardSimilarityRequest.builder();
        builder.vertex(4);
        builder.step().direction(Direction.BOTH).labels("link");
        SingleSourceJaccardSimilarityRequest request = builder.build();
        Map<Object, Double> results = jaccardSimilarityAPI.post(request);

        Assert.assertEquals(7, results.size());
        Set<Object> expected = ImmutableSet.of("3", "7", "8", "9",
                                               "10", "1", "2");
        Assert.assertEquals(expected, results.keySet());

        Assert.assertEquals(1.0, results.get("3"));
        Assert.assertEquals(0.5, results.get("7"));
        Assert.assertEquals(0.5, results.get("8"));
        Assert.assertEquals(0.5, results.get("9"));
        Assert.assertEquals(0.5, results.get("10"));
        Assert.assertEquals(0.0, results.get("1"));
        Assert.assertEquals(0.0, results.get("2"));
    }

    @Test
    public void testJaccardSimilarWithDirection() {
        SingleSourceJaccardSimilarityRequest.Builder builder =
                SingleSourceJaccardSimilarityRequest.builder();
        builder.vertex(4);
        builder.step().direction(Direction.OUT);
        SingleSourceJaccardSimilarityRequest request = builder.build();
        Map<Object, Double> results = jaccardSimilarityAPI.post(request);

        Assert.assertEquals(6, results.size());
        Set<Object> expected = ImmutableSet.of("1", "2", "3",
                                               "5", "7", "9");
        Assert.assertEquals(expected, results.keySet());

        Assert.assertEquals(0.0, results.get("3"));
        Assert.assertEquals(0.0, results.get("5"));
        Assert.assertEquals(0.0, results.get("7"));
        Assert.assertEquals(0.0, results.get("9"));
        Assert.assertEquals(0.0, results.get("1"));
        Assert.assertEquals(0.0, results.get("2"));
    }
}
