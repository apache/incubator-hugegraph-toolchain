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


import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class EdgeExistenceAPITest extends TraverserApiTest {

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
    public void testEdgeExistenceGet() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");

        List<Edge> edges = edgeExistenceAPI.get(markoId, vadasId, "", "", 100);
        Assert.assertEquals(1, edges.size());

        String sortValues = edges.get(0).name();
        edges = edgeExistenceAPI.get(markoId, vadasId, "knows", sortValues, 100);
        String id = edges.get(0).id();

        Assert.assertEquals(1, edges.size());
        Assert.assertTrue(id.contains("marko") && id.contains("vadas") && id.contains(sortValues));

        Object lopId = getVertexId("software", "name", "lop");

        edges = edgeExistenceAPI.get(lopId, vadasId, "knows", "", 100);
        Assert.assertEquals(0, edges.size());

        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        edges = edgeExistenceAPI.get(joshId, rippleId, "created", "", 100);
        Assert.assertEquals(1, edges.size());

        edges = edgeExistenceAPI.get(joshId, rippleId, "created", "", 0);
        Assert.assertEquals(0, edges.size());
    }
}
