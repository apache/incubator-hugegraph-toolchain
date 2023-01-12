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
import java.util.LinkedList;
import java.util.List;

import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TraverserManagerTest extends BaseFuncTest {

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
    public void testIterateVerticesByShard() {
        List<Shard> shards = traverser().vertexShards(1 * 1024 * 1024);
        List<Vertex> vertices = new LinkedList<>();
        for (Shard shard : shards) {
            Iterator<Vertex> iter = traverser().iteratorVertices(shard, 1);
            while (iter.hasNext()) {
                vertices.add(iter.next());
            }
        }
        Assert.assertEquals(6, vertices.size());
    }

    @Test
    public void testIterateEdgesByShard() {
        List<Shard> shards = traverser().edgeShards(1 * 1024 * 1024);
        List<Edge> edges = new LinkedList<>();
        for (Shard shard : shards) {
            Iterator<Edge> iter = traverser().iteratorEdges(shard, 1);
            while (iter.hasNext()) {
                edges.add(iter.next());
            }
        }
        Assert.assertEquals(6, edges.size());
    }
}
