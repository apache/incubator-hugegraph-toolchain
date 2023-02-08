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

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.exception.InvalidOperationException;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BatchInsertTest extends BaseFuncTest {

    private static final int BATCH_SIZE = 500;

    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
    }

    @After
    public void teardown() throws Exception {
        clearData();
    }

    @Test
    public void testBatchInsertInOneLoop() {
        List<Vertex> vertices = new ArrayList<>(BATCH_SIZE);
        List<Edge> edges = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            Vertex vertex1 = new Vertex("person").property("name", "P-" + i)
                                                 .property("age", i);
            Vertex vertex2 = new Vertex("software").property("name", "S-" + i)
                                                   .property("lang", "java");
            Edge edge = new Edge("created").source(vertex1).target(vertex2)
                                           .property("date", "2018-12-25");

            vertices.add(vertex1);
            vertices.add(vertex2);
            edges.add(edge);

            if (vertices.size() >= BATCH_SIZE) {
                graph().addVertices(vertices);
                graph().addEdges(edges);

                vertices.clear();
                edges.clear();
            }
        }

        Assert.assertEquals(2 * BATCH_SIZE, graph().listVertices(-1).size());
        Assert.assertEquals(BATCH_SIZE, graph().listEdges(-1).size());
    }

    @Test
    public void testBatchInsertInOneLoopButAddEdgesBeforeVertices() {
        List<Vertex> vertices = new ArrayList<>(BATCH_SIZE);
        List<Edge> edges = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            Vertex vertex1 = new Vertex("person").property("name", "P-" + i)
                                                 .property("age", i);
            Vertex vertex2 = new Vertex("software").property("name", "S-" + i)
                                                   .property("lang", "java");
            Edge edge = new Edge("created").source(vertex1).target(vertex2)
                                           .property("date", "2018-12-25");

            vertices.add(vertex1);
            vertices.add(vertex2);
            edges.add(edge);

            if (vertices.size() >= BATCH_SIZE) {
                // Must add vertices before edges
                Assert.assertThrows(InvalidOperationException.class, () -> {
                    graph().addEdges(edges);
                });
                graph().addVertices(vertices);

                vertices.clear();
                edges.clear();
            }
        }

        Assert.assertEquals(2 * BATCH_SIZE, graph().listVertices(-1).size());
        Assert.assertEquals(0, graph().listEdges(-1).size());
    }

    @Test
    public void testBatchInsertInTwoLoops() {
        int vertexCount = BATCH_SIZE;
        List<Vertex> persons = new ArrayList<>(BATCH_SIZE);
        List<Vertex> softwares = new ArrayList<>(BATCH_SIZE);
        List<Edge> edges = new ArrayList<>(BATCH_SIZE);

        for (int i = 1; i <= vertexCount; i++) {
            Vertex person = new Vertex("person").property("name", "P-" + i)
                                                .property("age", i);
            Vertex software = new Vertex("software").property("name", "S-" + i)
                                                    .property("lang", "java");

            persons.add(person);
            softwares.add(software);
        }
        graph().addVertices(persons);
        graph().addVertices(softwares);

        for (int i = 0; i < vertexCount; i++) {
            Vertex person = persons.get(i);
            Vertex software = softwares.get(i);

            Edge edge = new Edge("created").source(person).target(software)
                                           .property("date", "2018-12-25");

            edges.add(edge);
        }
        graph().addEdges(edges);

        Assert.assertEquals(2 * BATCH_SIZE, graph().listVertices(-1).size());
        Assert.assertEquals(BATCH_SIZE, graph().listEdges(-1).size());
    }
}
