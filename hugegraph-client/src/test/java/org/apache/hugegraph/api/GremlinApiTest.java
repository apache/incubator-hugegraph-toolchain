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

package org.apache.hugegraph.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.GraphAttachable;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GremlinApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @Before
    public void prepareData() {
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testQueryAllVertices() {
        GremlinRequest request = new GremlinRequest("g.V()");
        ResultSet resultSet = gremlin().execute(request);

        Assert.assertEquals(6, resultSet.size());

        request = new GremlinRequest("g.V().drop()");
        gremlin().execute(request);

        request = new GremlinRequest("g.V()");
        resultSet = gremlin().execute(request);

        Assert.assertEquals(0, resultSet.size());
    }

    @Test
    public void testQueryAllEdges() {
        GremlinRequest request = new GremlinRequest("g.E()");
        ResultSet resultSet = gremlin().execute(request);

        Assert.assertEquals(6, resultSet.size());

        request = new GremlinRequest("g.E().drop()");
        gremlin().execute(request);

        request = new GremlinRequest("g.E()");
        resultSet = gremlin().execute(request);

        Assert.assertEquals(0, resultSet.size());
    }

    @Test
    public void testAsyncRemoveAllVertices() {
        GremlinRequest request = new GremlinRequest("g.V()");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(6, resultSet.size());

        String gremlin = "hugegraph.traversal().V().drop()";
        request = new GremlinRequest(gremlin);
        long id = gremlin().executeAsTask(request);
        waitUntilTaskCompleted(id);

        request = new GremlinRequest("g.V()");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(0, resultSet.size());
    }

    @Test
    public void testAsyncRemoveAllEdges() {
        GremlinRequest request = new GremlinRequest("g.E()");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(6, resultSet.size());

        String gremlin = "g.E().drop()";
        request = new GremlinRequest(gremlin);
        long id = gremlin().executeAsTask(request);
        waitUntilTaskCompleted(id);

        request = new GremlinRequest("g.E()");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(0, resultSet.size());
    }

    @Test
    public void testPrimitiveObject() {
        GremlinRequest request = new GremlinRequest("1 + 2");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(1, resultSet.size());

        Iterator<Result> results = resultSet.iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Object object = result.getObject();
            Assert.assertEquals(Integer.class, object.getClass());
            Assert.assertEquals(3, object);
        }
    }

    @Test
    public void testIterateEmptyResultSet() {
        GremlinRequest request = new GremlinRequest("g.V().limit(0)");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(0, resultSet.size());
        Assert.assertThrows(NoSuchElementException.class, () -> {
            resultSet.iterator().next();
        });
    }

    @Test
    public void testAttachedManager() {
        GremlinRequest request = new GremlinRequest("g.V()");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(6, resultSet.size());

        Iterator<Result> results = resultSet.iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Object object = result.getObject();
            Assert.assertEquals(Vertex.class, object.getClass());
            Vertex vertex = (Vertex) object;
            Assert.assertNotNull(Whitebox.getInternalState(vertex, "manager"));
        }

        request = new GremlinRequest("g.E()");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(6, resultSet.size());

        results = resultSet.iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Object object = result.getObject();
            Assert.assertEquals(Edge.class, object.getClass());
            Edge edge = (Edge) object;
            Assert.assertNotNull(Whitebox.getInternalState(edge, "manager"));
        }

        request = new GremlinRequest("g.V().outE().path()");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(6, resultSet.size());

        results = resultSet.iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Object object = result.getObject();
            Assert.assertEquals(Path.class, object.getClass());
            Path path = (Path) object;
            Assert.assertNotNull(path.objects());
            for (Object pathObject : path.objects()) {
                Assert.assertTrue(pathObject instanceof GraphAttachable);
                Assert.assertNotNull(Whitebox.getInternalState(pathObject,
                                                               "manager"));
            }
            Assert.assertNull(path.crosspoint());
        }
    }

    @Test
    public void testInvalidGremlin() {
        Assert.assertThrows(ServerException.class, () -> {
            client().post("gremlin", "{");
        }, e -> {
            Assert.assertContains("body could not be parsed", e.getMessage());
        });

        GremlinRequest request = new GremlinRequest("g.V2()");
        Assert.assertThrows(ServerException.class, () -> {
            gremlin().execute(request);
        }, e -> {
            Assert.assertContains("No signature of method: ", e.getMessage());
            Assert.assertContains(".V2() is applicable for argument types: ()",
                                  e.getMessage());
        });
    }

    @Test
    public void testSecurityOperation() {
        GremlinRequest request = new GremlinRequest("System.exit(-1)");
        Assert.assertThrows(ServerException.class, () -> {
            gremlin().execute(request);
        }, e -> {
            String msg = "Not allowed to call System.exit() via Gremlin";
            Assert.assertContains(msg, e.getMessage());
        });
    }
}
