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

import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.WeightedPath;
import org.apache.hugegraph.structure.traverser.WeightedPaths;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class SingleSourceShortestPathApiTest extends TraverserApiTest {

    @BeforeClass
    public static void initShortestPathGraph() {
        schema().propertyKey("weight")
                .asDouble()
                .ifNotExist()
                .create();

        schema().vertexLabel("node")
                .useCustomizeStringId()
                .ifNotExist()
                .create();

        schema().edgeLabel("link")
                .sourceLabel("node").targetLabel("node")
                .properties("weight")
                .ifNotExist()
                .create();

        schema().edgeLabel("relateTo")
                .sourceLabel("node").targetLabel("node")
                .properties("weight")
                .ifNotExist()
                .create();

        Vertex va = graph().addVertex(T.LABEL, "node", T.ID, "A");
        Vertex vb = graph().addVertex(T.LABEL, "node", T.ID, "B");
        Vertex vc = graph().addVertex(T.LABEL, "node", T.ID, "C");
        Vertex vd = graph().addVertex(T.LABEL, "node", T.ID, "D");
        Vertex ve = graph().addVertex(T.LABEL, "node", T.ID, "E");
        Vertex vf = graph().addVertex(T.LABEL, "node", T.ID, "F");
        Vertex vg = graph().addVertex(T.LABEL, "node", T.ID, "G");
        Vertex vh = graph().addVertex(T.LABEL, "node", T.ID, "H");
        Vertex vi = graph().addVertex(T.LABEL, "node", T.ID, "I");
        Vertex vj = graph().addVertex(T.LABEL, "node", T.ID, "J");
        Vertex vk = graph().addVertex(T.LABEL, "node", T.ID, "K");
        Vertex vl = graph().addVertex(T.LABEL, "node", T.ID, "L");
        Vertex vm = graph().addVertex(T.LABEL, "node", T.ID, "M");
        Vertex vn = graph().addVertex(T.LABEL, "node", T.ID, "N");
        Vertex vo = graph().addVertex(T.LABEL, "node", T.ID, "O");
        Vertex vp = graph().addVertex(T.LABEL, "node", T.ID, "P");
        Vertex vq = graph().addVertex(T.LABEL, "node", T.ID, "Q");
        Vertex vr = graph().addVertex(T.LABEL, "node", T.ID, "R");
        Vertex vz = graph().addVertex(T.LABEL, "node", T.ID, "Z");

        /*
         *   "link":
         *   A --0.2--> B --0.4--> C --0.8--> D --0.6--> Z
         *     ----------------10---------------------->
         *     <--0.5-- E <--0.3-- F <--0.4-- G <--0.1--
         *     <-------------------8--------------------
         *     --0.1--> H --0.1--> I <--0.1-- J <--0.2--
         *     -----0.4----> K -----0.5-----> L <--0.3--
         *   "relateTo":
         *     -----1.4-----> M -----3.8----> N --3.5-->
         *     <----2.2------ O <----3.3----- P <-1.6---
         *     -----3.1-----> Q <----2.0----- R --1.3-->
         */
        va.addEdge("link", vb, "weight", 0.2D);
        vb.addEdge("link", vc, "weight", 0.4D);
        vc.addEdge("link", vd, "weight", 0.8D);
        vd.addEdge("link", vz, "weight", 0.6D);

        va.addEdge("link", vz, "weight", 10.0D);

        vz.addEdge("link", vg, "weight", 0.1D);
        vg.addEdge("link", vf, "weight", 0.4D);
        vf.addEdge("link", ve, "weight", 0.3D);
        ve.addEdge("link", va, "weight", 0.5D);

        vz.addEdge("link", va, "weight", 8.0D);

        va.addEdge("link", vh, "weight", 0.1D);
        vh.addEdge("link", vi, "weight", 0.1D);
        vz.addEdge("link", vj, "weight", 0.2D);
        vj.addEdge("link", vi, "weight", 0.1D);

        va.addEdge("link", vk, "weight", 0.4D);
        vk.addEdge("link", vl, "weight", 0.5D);
        vz.addEdge("link", vl, "weight", 0.3D);

        va.addEdge("relateTo", vm, "weight", 1.4D);
        vm.addEdge("relateTo", vn, "weight", 3.8D);
        vn.addEdge("relateTo", vz, "weight", 3.5D);

        vz.addEdge("relateTo", vp, "weight", 1.6D);
        vp.addEdge("relateTo", vo, "weight", 3.3D);
        vo.addEdge("relateTo", va, "weight", 2.2D);

        va.addEdge("relateTo", vq, "weight", 3.1D);
        vr.addEdge("relateTo", vq, "weight", 2.0D);
        vr.addEdge("relateTo", vz, "weight", 1.3D);
    }

    @Test
    public void testSingleSourceShortestPath() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.BOTH, null, "weight",
                                      -1, 0, -1, -1, false);
        Assert.assertEquals(18, weightedPaths.paths().size());

        WeightedPath.Path path = weightedPaths.paths().get("B");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B"), path.vertices());

        path = weightedPaths.paths().get("C");
        Assert.assertEquals(0.6000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C"), path.vertices());

        path = weightedPaths.paths().get("D");
        Assert.assertEquals(1.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "D"),
                            path.vertices());

        path = weightedPaths.paths().get("E");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E"), path.vertices());

        path = weightedPaths.paths().get("F");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E", "F"), path.vertices());

        path = weightedPaths.paths().get("G");
        Assert.assertEquals(0.6D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "G"),
                            path.vertices());

        path = weightedPaths.paths().get("H");
        Assert.assertEquals(0.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H"), path.vertices());

        path = weightedPaths.paths().get("I");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I"), path.vertices());

        path = weightedPaths.paths().get("J");
        Assert.assertEquals(0.30000000000000004D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J"),
                            path.vertices());

        path = weightedPaths.paths().get("K");
        Assert.assertEquals(0.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "K"), path.vertices());

        path = weightedPaths.paths().get("L");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "L"),
                            path.vertices());

        path = weightedPaths.paths().get("M");
        Assert.assertEquals(1.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "M"), path.vertices());

        path = weightedPaths.paths().get("N");
        Assert.assertEquals(4.0D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "N"),
                            path.vertices());

        path = weightedPaths.paths().get("O");
        Assert.assertEquals(2.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "O"), path.vertices());

        path = weightedPaths.paths().get("P");
        Assert.assertEquals(2.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "P"),
                            path.vertices());

        path = weightedPaths.paths().get("Q");
        Assert.assertEquals(3.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "Q"), path.vertices());

        path = weightedPaths.paths().get("R");
        Assert.assertEquals(1.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "R"),
                            path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z"),
                            path.vertices());
    }

    @Test
    public void testSingleSourceShortestPathWithLabel() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.BOTH, "link", "weight",
                                      -1, 0, -1, -1, false);
        Assert.assertEquals(12, weightedPaths.paths().size());

        WeightedPath.Path path = weightedPaths.paths().get("B");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B"), path.vertices());

        path = weightedPaths.paths().get("C");
        Assert.assertEquals(0.6000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C"), path.vertices());

        path = weightedPaths.paths().get("D");
        Assert.assertEquals(1.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "D"),
                            path.vertices());

        path = weightedPaths.paths().get("E");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E"), path.vertices());

        path = weightedPaths.paths().get("F");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E", "F"), path.vertices());

        path = weightedPaths.paths().get("G");
        Assert.assertEquals(0.6D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "G"),
                            path.vertices());

        path = weightedPaths.paths().get("H");
        Assert.assertEquals(0.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H"), path.vertices());

        path = weightedPaths.paths().get("I");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I"), path.vertices());

        path = weightedPaths.paths().get("J");
        Assert.assertEquals(0.30000000000000004D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J"),
                            path.vertices());

        path = weightedPaths.paths().get("K");
        Assert.assertEquals(0.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "K"), path.vertices());

        path = weightedPaths.paths().get("L");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "L"),
                            path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z"),
                            path.vertices());

        weightedPaths = singleSourceShortestPathAPI.get(
                        "A", Direction.BOTH, "relateTo", "weight",
                        -1, 0, -1, -1, false);

        Assert.assertEquals(7, weightedPaths.paths().size());

        path = weightedPaths.paths().get("M");
        Assert.assertEquals(1.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "M"), path.vertices());

        path = weightedPaths.paths().get("N");
        Assert.assertEquals(5.199999999999999D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "M", "N"),
                            path.vertices());

        path = weightedPaths.paths().get("O");
        Assert.assertEquals(2.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "O"), path.vertices());

        path = weightedPaths.paths().get("P");
        Assert.assertEquals(5.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "O", "P"),
                            path.vertices());

        path = weightedPaths.paths().get("Q");
        Assert.assertEquals(3.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "Q"), path.vertices());

        path = weightedPaths.paths().get("R");
        Assert.assertEquals(5.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "Q", "R"),
                            path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(6.3999999999999995D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "Q", "R", "Z"),
                            path.vertices());
    }

    @Test
    public void testSingleSourceShortestPathWithDirection() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.OUT, null, "weight",
                                      -1, 0, -1, -1, false);

        Assert.assertEquals(17, weightedPaths.paths().size());

        WeightedPath.Path path = weightedPaths.paths().get("B");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B"), path.vertices());

        path = weightedPaths.paths().get("C");
        Assert.assertEquals(0.6000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C"), path.vertices());

        path = weightedPaths.paths().get("D");
        Assert.assertEquals(1.4000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D"),
                            path.vertices());

        path = weightedPaths.paths().get("E");
        Assert.assertEquals(2.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D",
                                             "Z", "G", "F", "E"),
                            path.vertices());

        path = weightedPaths.paths().get("F");
        Assert.assertEquals(2.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z", "G", "F"),
                            path.vertices());

        path = weightedPaths.paths().get("G");
        Assert.assertEquals(2.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z", "G"),
                            path.vertices());

        path = weightedPaths.paths().get("H");
        Assert.assertEquals(0.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H"), path.vertices());

        path = weightedPaths.paths().get("I");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I"), path.vertices());

        path = weightedPaths.paths().get("J");
        Assert.assertEquals(2.2D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z", "J"),
                            path.vertices());

        path = weightedPaths.paths().get("K");
        Assert.assertEquals(0.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "K"), path.vertices());

        path = weightedPaths.paths().get("L");
        Assert.assertEquals(0.9D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "K", "L"),
                            path.vertices());

        path = weightedPaths.paths().get("M");
        Assert.assertEquals(1.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "M"), path.vertices());

        path = weightedPaths.paths().get("N");
        Assert.assertEquals(5.199999999999999D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "M", "N"),
                            path.vertices());

        path = weightedPaths.paths().get("O");
        Assert.assertEquals(6.9D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z", "P", "O"),
                            path.vertices());

        path = weightedPaths.paths().get("P");
        Assert.assertEquals(3.6D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z", "P"),
                            path.vertices());

        path = weightedPaths.paths().get("Q");
        Assert.assertEquals(3.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "Q"), path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(2.0D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z"),
                            path.vertices());
    }

    @Test
    public void testSingleSourceShortestPathWithDegree() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.OUT, null, "weight",
                                      1, 0, -1, -1, false);
        Assert.assertEquals(4, weightedPaths.paths().size());

        WeightedPath.Path path = weightedPaths.paths().get("B");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B"), path.vertices());

        path = weightedPaths.paths().get("C");
        Assert.assertEquals(0.6000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C"), path.vertices());

        path = weightedPaths.paths().get("D");
        Assert.assertEquals(1.4000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D"),
                            path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(2.0D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "Z"),
                            path.vertices());
    }

    @Test
    public void testSingleSourceShortestPathWithLimit() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.BOTH, null, "weight",
                                      -1, 0, -1, 11, false);
        Assert.assertEquals(11, weightedPaths.paths().size());

        WeightedPath.Path path = weightedPaths.paths().get("B");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B"), path.vertices());

        path = weightedPaths.paths().get("C");
        Assert.assertEquals(0.6000000000000001D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "B", "C"), path.vertices());

        path = weightedPaths.paths().get("E");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E"), path.vertices());

        path = weightedPaths.paths().get("F");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "E", "F"), path.vertices());

        path = weightedPaths.paths().get("L");
        Assert.assertEquals(0.8D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "L"),
                            path.vertices());

        path = weightedPaths.paths().get("G");
        Assert.assertEquals(0.6D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z", "G"),
                            path.vertices());

        path = weightedPaths.paths().get("H");
        Assert.assertEquals(0.1D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H"), path.vertices());

        path = weightedPaths.paths().get("I");
        Assert.assertEquals(0.2D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I"), path.vertices());

        path = weightedPaths.paths().get("J");
        Assert.assertEquals(0.30000000000000004D, path.weight(),
                            Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J"),
                            path.vertices());

        path = weightedPaths.paths().get("K");
        Assert.assertEquals(0.4D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "K"), path.vertices());

        path = weightedPaths.paths().get("Z");
        Assert.assertEquals(0.5D, path.weight(), Double.MIN_VALUE);
        Assert.assertEquals(ImmutableList.of("A", "H", "I", "J", "Z"),
                            path.vertices());
    }

    @Test
    public void testSingleSourceShortestPathWithVertex() {
        WeightedPaths weightedPaths = singleSourceShortestPathAPI.get(
                                      "A", Direction.BOTH, null, "weight",
                                      -1, 0, -1, -1, true);
        Assert.assertEquals(18, weightedPaths.paths().size());
        Assert.assertEquals(19, weightedPaths.vertices().size());
    }
}
