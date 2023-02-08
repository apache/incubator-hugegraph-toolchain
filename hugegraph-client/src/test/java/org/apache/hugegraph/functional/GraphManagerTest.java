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

package org.apache.hugegraph.functional;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GraphManagerTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
        BaseClientTest.initVertex();
        BaseClientTest.initEdge();
    }

    @After
    public void teardown() throws Exception {
        clearData();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIterateVertices() {
        Iterator<Vertex> vertices = graph().iterateVertices(1);
        List<Vertex> results = IteratorUtils.toList(vertices);
        Assert.assertEquals(6, results.size());

        vertices = graph().iterateVertices(6);
        results = IteratorUtils.toList(vertices);
        Assert.assertEquals(6, results.size());

        vertices = graph().iterateVertices(100);
        results = IteratorUtils.toList(vertices);
        Assert.assertEquals(6, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIterateEdgesWithPageLtThanTotal() {
        Iterator<Edge> edes = graph().iterateEdges(1);
        List<Edge> results = IteratorUtils.toList(edes);
        Assert.assertEquals(6, results.size());

        edes = graph().iterateEdges(6);
        results = IteratorUtils.toList(edes);
        Assert.assertEquals(6, results.size());

        edes = graph().iterateEdges(100);
        results = IteratorUtils.toList(edes);
        Assert.assertEquals(6, results.size());
    }
}
