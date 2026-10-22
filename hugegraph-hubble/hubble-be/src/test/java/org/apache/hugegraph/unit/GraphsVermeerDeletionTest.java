/*
 *
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

package org.apache.hugegraph.unit;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.graphs.GraphsController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.GremlinCollectionService;
import org.apache.hugegraph.service.space.DirectVermeerService;
import org.apache.hugegraph.service.space.VermeerClient;
import org.apache.hugegraph.service.space.VermeerService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class GraphsVermeerDeletionTest {

    private static final String CONFIRM = "I'm sure to drop the graph";
    private VermeerClient backend;
    private HugeConfig config;
    private HugeClient client;
    private GraphsService graphs;
    private ExecuteHistoryService history;
    private GremlinCollectionService collections;
    private JobManagerService jobs;
    private GraphsController controller;

    @Before
    public void setup() {
        this.backend = Mockito.mock(VermeerClient.class);
        Mockito.when(this.backend.configured()).thenReturn(true);
        Mockito.when(this.backend.request("GET", "graphs", null)).thenReturn(
                Map.of("graphs", List.of(Map.of("name", "DEFAULT-graph_a"))));
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null))
               .thenReturn(Map.of("tasks", List.of()));
        this.client = Mockito.mock(HugeClient.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.client.getGraphSpaceName()).thenReturn("DEFAULT");
        Mockito.when(this.client.getGraphName()).thenReturn("graph_a");
        Mockito.when(this.client.auth().isSuperAdmin()).thenReturn(true);
        DirectVermeerService direct = new DirectVermeerService();
        this.config = Mockito.mock(HugeConfig.class);
        Mockito.when(this.config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        ReflectionTestUtils.setField(direct, "config", this.config);
        ReflectionTestUtils.setField(direct, "backend", this.backend);
        ReflectionTestUtils.setField(direct, "authMode", Mockito.mock(AuthModeService.class));
        VermeerService vermeer = new VermeerService();
        ReflectionTestUtils.setField(vermeer, "direct", direct);
        this.graphs = Mockito.mock(GraphsService.class);
        this.history = Mockito.mock(ExecuteHistoryService.class);
        this.collections = Mockito.mock(GremlinCollectionService.class);
        this.jobs = Mockito.mock(JobManagerService.class);
        this.controller = new GraphsController() {
            @Override
            protected HugeClient authClient(String graphspace, String graph) {
                Assert.assertEquals("DEFAULT", graphspace);
                Assert.assertEquals("graph_a", graph);
                return GraphsVermeerDeletionTest.this.client;
            }

            @Override
            public boolean isVermeerEnabled(HugeClient graphClient) {
                throw new AssertionError("Source cleanup must not use interactive capability");
            }
        };
        ReflectionTestUtils.setField(this.controller, "graphsService", this.graphs);
        ReflectionTestUtils.setField(this.controller, "vermeerService", vermeer);
        ReflectionTestUtils.setField(this.controller, "executeHistoryService", this.history);
        ReflectionTestUtils.setField(this.controller, "gremlinCollectionService", this.collections);
        ReflectionTestUtils.setField(this.controller, "jobManagerService", this.jobs);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
            Assert.fail("Expected " + type.getSimpleName());
        } catch (RuntimeException error) {
            Assert.assertTrue(error.toString(), type.isInstance(error));
        }
    }

    @Test
    public void testCleanupPrecedesSourceDeletionWithoutWorkerCapability() {
        Mockito.when(this.backend.request("GET", "workers", null)).thenReturn(Map.of("workers", List.of()));
        this.controller.delete("DEFAULT", "graph_a", CONFIRM);
        InOrder order = Mockito.inOrder(this.backend, this.graphs);
        order.verify(this.backend).request("GET", "graphs", null);
        order.verify(this.backend).request("DELETE", "graphs/DEFAULT-graph_a", null);
        order.verify(this.graphs).delete(this.client, "graph_a", CONFIRM);
        Mockito.verify(this.backend, Mockito.never()).request("GET", "workers", null);
        Mockito.verify(this.history).deleteByGraph("DEFAULT", "graph_a");
    }

    @Test
    public void testUnauthorizedCallerCannotUnloadOrDeleteSource() {
        Mockito.when(this.client.auth().isSuperAdmin()).thenReturn(false);
        expect(ForbiddenException.class,
                () -> this.controller.delete("DEFAULT", "graph_a", CONFIRM));
        Mockito.verify(this.backend).request("GET", "graphs", null);
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.eq("DELETE"), Mockito.anyString(), Mockito.isNull());
        Mockito.verifyNoInteractions(this.graphs, this.history, this.collections, this.jobs);
    }

    @Test
    public void testCleanupFailurePreventsSourceAndLocalDeletion() {
        Mockito.when(this.backend.request("DELETE", "graphs/DEFAULT-graph_a", null))
               .thenThrow(new ExternalException("Vermeer cleanup unavailable"));
        expect(ExternalException.class,
                () -> this.controller.delete("DEFAULT", "graph_a", CONFIRM));
        Mockito.verifyNoInteractions(this.graphs, this.history, this.collections, this.jobs);
    }

    @Test
    public void testMissingMaterializationPreservesSpaceAdministratorDeletion() {
        Mockito.when(this.client.auth().isSuperAdmin()).thenReturn(false);
        Mockito.when(this.backend.request("GET", "graphs", null)).thenReturn(Map.of("graphs", List.of()));
        this.controller.delete("DEFAULT", "graph_a", CONFIRM);
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.eq("DELETE"), Mockito.anyString(), Mockito.isNull());
        Mockito.verify(this.graphs).delete(this.client, "graph_a", CONFIRM);
    }

    @Test
    public void testStandaloneDeletionDoesNotTouchConfiguredVermeer() {
        Mockito.when(this.config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        this.controller.delete("DEFAULT", "graph_a", CONFIRM);
        Mockito.verify(this.backend, Mockito.never()).request(Mockito.anyString(),
                Mockito.anyString(), Mockito.any());
        Mockito.verify(this.graphs).delete(this.client, "graph_a", CONFIRM);
        Mockito.verify(this.history).deleteByGraph("DEFAULT", "graph_a");
    }

    @Test
    public void testInvalidConfirmationDoesNotUnloadOrDelete() {
        expect(IllegalArgumentException.class,
                () -> this.controller.delete("DEFAULT", "graph_a", "not confirmed"));
        Mockito.verifyNoInteractions(this.backend, this.graphs, this.history,
                                     this.collections, this.jobs);
    }

    @Test
    public void testAcceptedLoadBlocksDeletionEvenWhenNativeGraphIsAbsent() {
        for (String type : List.of("load", "reload")) {
            for (String state : List.of("created", "waiting", "loading")) {
                Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null))
                       .thenReturn(Map.of("tasks", List.of(Map.of("graph_name", "DEFAULT-graph_a",
                               "task_type", type, "status", state))));
                Mockito.when(this.backend.request("GET", "graphs", null))
                       .thenReturn(Map.of("graphs", List.of()));
                expect(ExternalException.class,
                        () -> this.controller.delete("DEFAULT", "graph_a", CONFIRM));
            }
        }
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.eq("DELETE"), Mockito.anyString(), Mockito.isNull());
        Mockito.verifyNoInteractions(this.graphs, this.history, this.collections, this.jobs);
    }

    @Test
    public void testUnrelatedAndCompletedLoadsDoNotBlockDeletion() {
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null))
               .thenReturn(Map.of("tasks", List.of(
                       Map.of("graph_name", "OTHER-graph_a", "task_type", "load", "status", "loading"),
                       Map.of("graph_name", "DEFAULT-graph_a", "task_type", "reload", "status", "loaded"),
                       Map.of("graph_name", "DEFAULT-graph_a", "task_type", "compute", "status", "computing"))));
        this.controller.delete("DEFAULT", "graph_a", CONFIRM);
        Mockito.verify(this.graphs).delete(this.client, "graph_a", CONFIRM);
    }

    @Test
    public void testTaskQueueFailureOrTruncationStopsDeletion() {
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null))
               .thenThrow(new ExternalException("Vermeer task queue unavailable"))
               .thenReturn(Map.of("tasks", java.util.Collections.nCopies(10000,
                       Map.of("graph_name", "OTHER-graph_a"))));
        expect(ExternalException.class, () -> this.controller.delete("DEFAULT", "graph_a", CONFIRM));
        expect(ExternalException.class, () -> this.controller.delete("DEFAULT", "graph_a", CONFIRM));
        Mockito.verifyNoInteractions(this.graphs, this.history, this.collections, this.jobs);
    }
}
