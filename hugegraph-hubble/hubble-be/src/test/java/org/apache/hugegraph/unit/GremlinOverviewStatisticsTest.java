/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import java.util.HashMap;
import java.util.Map;

import org.apache.hugegraph.controller.query.GremlinQueryController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.query.QueryService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class GremlinOverviewStatisticsTest {

    @Test
    public void testOverviewReadsRecordedCountsWithoutGremlinExecution() {
        HugeClient client = Mockito.mock(HugeClient.class);
        QueryService queries = Mockito.mock(QueryService.class);
        GraphsService graphs = Mockito.mock(GraphsService.class);
        GremlinQueryController controller = controller(client, graphs, queries);
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("vertex", 12000L);
        snapshot.put("edge", 18000L);
        Mockito.when(graphs.evCount(client, "space", "graph")).thenReturn(snapshot);

        Map<String, Object> result = controller.execute("space", "graph");

        Assert.assertEquals("12000", result.get("vertexcount"));
        Assert.assertEquals("18000", result.get("edgecount"));
        Mockito.verify(graphs).evCount(client, "space", "graph");
        Mockito.verifyNoInteractions(queries);
    }

    @Test
    public void testOverviewPreservesUnknownCountsWithoutGremlinExecution() {
        HugeClient client = Mockito.mock(HugeClient.class);
        QueryService queries = Mockito.mock(QueryService.class);
        GraphsService graphs = Mockito.mock(GraphsService.class);
        GremlinQueryController controller = controller(client, graphs, queries);
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("vertex", null);
        snapshot.put("edge", null);
        Mockito.when(graphs.evCount(client, "space", "graph")).thenReturn(snapshot);

        Map<String, Object> result = controller.execute("space", "graph");

        Assert.assertTrue(result.containsKey("vertexcount"));
        Assert.assertTrue(result.containsKey("edgecount"));
        Assert.assertNull(result.get("vertexcount"));
        Assert.assertNull(result.get("edgecount"));
        Mockito.verifyNoInteractions(queries);
    }

    private static GremlinQueryController controller(HugeClient client,
                                                      GraphsService graphs,
                                                      QueryService queries) {
        GremlinQueryController controller = new GremlinQueryController() {
            @Override
            protected HugeClient authClient(String graphSpace, String graph) {
                return client;
            }
        };
        ReflectionTestUtils.setField(controller, "graphsService", graphs);
        ReflectionTestUtils.setField(controller, "queryService", queries);
        return controller;
    }
}
