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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.space.GraphSpaceController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.space.DirectVermeerService;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.apache.hugegraph.service.space.VermeerClient;
import org.apache.hugegraph.service.space.VermeerService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class GraphSpaceVermeerDeletionTest {

    private HugeConfig config;
    private VermeerClient backend;
    private HugeClient administrator;
    private HugeClient space;
    private GraphSpaceService spaces;
    private UserService users;
    private GraphSpaceController controller;
    private boolean authorized;
    private int restored;

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
            Assert.fail("Expected " + type.getSimpleName());
        } catch (RuntimeException error) {
            Assert.assertTrue(error.toString(), type.isInstance(error));
        }
    }

    @Before
    public void setup() {
        this.authorized = true;
        this.config = Mockito.mock(HugeConfig.class);
        Mockito.when(this.config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        this.backend = Mockito.mock(VermeerClient.class);
        Mockito.when(this.backend.configured()).thenReturn(true);
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null))
               .thenReturn(Map.of("tasks", List.of()));
        Mockito.when(this.backend.request("GET", "graphs", null)).thenReturn(Map.of("graphs", List.of(
                Map.of("name", "shop-first"), Map.of("name", "shop-second"),
                Map.of("name", "shop-orphan"), Map.of("name", "OTHER-first"))));
        this.administrator = Mockito.mock(HugeClient.class, Mockito.RETURNS_DEEP_STUBS);
        this.space = Mockito.mock(HugeClient.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.space.graphs().listGraph()).thenReturn(List.of("first", "second", "plain"));
        AtomicReference<String> selectedGraph = new AtomicReference<>();
        Mockito.when(this.space.getGraphSpaceName()).thenReturn("shop");
        Mockito.when(this.space.getGraphName()).thenAnswer(call -> selectedGraph.get());
        Mockito.when(this.space.assignGraph(Mockito.eq("shop"), Mockito.any())).thenAnswer(call -> {
            selectedGraph.set(call.getArgument(1));
            return this.space;
        });
        Mockito.when(this.space.auth().isSuperAdmin()).thenReturn(true);
        this.spaces = Mockito.mock(GraphSpaceService.class);
        this.users = Mockito.mock(UserService.class);
        Mockito.when(this.users.listGraphSpaceAdmin(this.administrator, "shop")).thenReturn(List.of("owner"));
        DirectVermeerService direct = new DirectVermeerService();
        ReflectionTestUtils.setField(direct, "config", this.config);
        ReflectionTestUtils.setField(direct, "backend", this.backend);
        ReflectionTestUtils.setField(direct, "authMode", Mockito.mock(AuthModeService.class));
        VermeerService service = new VermeerService();
        ReflectionTestUtils.setField(service, "direct", direct);
        this.controller = new GraphSpaceController() {
            @Override
            protected HugeClient requireGraphSpaceAdministrator() {
                if (!GraphSpaceVermeerDeletionTest.this.authorized) {
                    throw new ForbiddenException("Permission denied");
                }
                return GraphSpaceVermeerDeletionTest.this.administrator;
            }

            @Override
            protected HugeClient authClient(String graphspace, String graph) {
                if (graphspace == null && graph == null) {
                    GraphSpaceVermeerDeletionTest.this.restored++;
                    return GraphSpaceVermeerDeletionTest.this.administrator;
                }
                Assert.assertEquals("shop", graphspace);
                if (graph == null) {
                    return GraphSpaceVermeerDeletionTest.this.space;
                }
                HugeClient client = Mockito.mock(HugeClient.class, Mockito.RETURNS_DEEP_STUBS);
                Mockito.when(client.getGraphSpaceName()).thenReturn(graphspace);
                Mockito.when(client.getGraphName()).thenReturn(graph);
                Mockito.when(client.auth().isSuperAdmin()).thenReturn(true);
                return client;
            }
        };
        ReflectionTestUtils.setField(this.controller, "config", this.config);
        ReflectionTestUtils.setField(this.controller, "vermeerService", service);
        ReflectionTestUtils.setField(this.controller, "graphSpaceService", this.spaces);
        ReflectionTestUtils.setField(this.controller, "userService", this.users);
        Mockito.clearInvocations(this.space);
    }

    @Test
    public void testAllMemberCopiesAreCleanedBeforeAnySpaceMutation() {
        this.controller.delete("shop");
        InOrder order = Mockito.inOrder(this.backend, this.users, this.administrator.auth(), this.spaces);
        order.verify(this.backend).request("DELETE", "graphs/shop-first", null);
        order.verify(this.backend).request("DELETE", "graphs/shop-second", null);
        order.verify(this.users).listGraphSpaceAdmin(this.administrator, "shop");
        order.verify(this.administrator.auth()).delSpaceAdmin("owner", "shop");
        order.verify(this.spaces).delete(this.administrator, "shop");
        Mockito.verify(this.space.graphs()).listGraph();
        Mockito.verify(this.backend, Mockito.never()).request("DELETE", "graphs/shop-plain", null);
        Mockito.verify(this.backend, Mockito.never()).request("DELETE", "graphs/shop-orphan", null);
        Mockito.verify(this.backend, Mockito.never()).request("DELETE", "graphs/OTHER-first", null);
        Mockito.verify(this.backend).request("GET", "tasks?type=todo&limit=10000", null);
        Assert.assertNull(this.space.getGraphName());
        Assert.assertEquals(1, this.restored);
    }

    @Test
    public void testCleanupFailurePreservesSpaceAndAdministratorBindings() {
        Mockito.when(this.backend.request("DELETE", "graphs/shop-second", null))
               .thenThrow(new ExternalException("Native cleanup failed"));
        expect(ExternalException.class, () -> this.controller.delete("shop"));
        Mockito.verifyNoInteractions(this.users, this.spaces);
        Mockito.verify(this.administrator.auth(), Mockito.never())
               .delSpaceAdmin(Mockito.anyString(), Mockito.anyString());
        Assert.assertEquals(1, this.restored);
    }

    @Test
    public void testPendingReloadStopsSpaceDeletionWithoutGraphRecord() {
        Mockito.when(this.backend.request("GET", "graphs", null)).thenReturn(Map.of("graphs", List.of()));
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null)).thenReturn(
                Map.of("tasks", List.of(Map.of("graph_name", "shop-first",
                                               "task_type", "reload", "status", "loading"))));
        expect(ExternalException.class, () -> this.controller.delete("shop"));
        Mockito.verifyNoInteractions(this.users, this.spaces);
        Assert.assertEquals(1, this.restored);
    }

    @Test
    public void testPendingSecondMemberPreventsEveryUnload() {
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null)).thenReturn(
                Map.of("tasks", List.of(Map.of("graph_name", "shop-second",
                                               "task_type", "reload", "status", "loading"))));
        expect(ExternalException.class, () -> this.controller.delete("shop"));
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.eq("DELETE"), Mockito.anyString(), Mockito.isNull());
        Mockito.verify(this.backend).request("GET", "tasks?type=todo&limit=10000", null);
        Mockito.verifyNoInteractions(this.users, this.spaces);
        Assert.assertNull(this.space.getGraphName());
    }

    @Test
    public void testSimilarSpacePrefixIsNotAMember() {
        Mockito.when(this.backend.request("GET", "tasks?type=todo&limit=10000", null)).thenReturn(
                Map.of("tasks", List.of(Map.of("graph_name", "shop-eu-first",
                                               "task_type", "load", "status", "waiting"))));
        this.controller.delete("shop");
        Mockito.verify(this.backend).request("DELETE", "graphs/shop-first", null);
        Mockito.verify(this.backend).request("DELETE", "graphs/shop-second", null);
        Mockito.verify(this.backend).request("GET", "tasks?type=todo&limit=10000", null);
        Mockito.verify(this.spaces).delete(this.administrator, "shop");
    }

    @Test
    public void testUnconfiguredIntegrationRetainsOriginalDeletion() {
        Mockito.when(this.backend.configured()).thenReturn(false);
        this.controller.delete("shop");
        Mockito.verifyNoInteractions(this.space);
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(this.spaces).delete(this.administrator, "shop");
    }

    @Test
    public void testMissingNativeCopiesStillAllowsSpaceDeletion() {
        Mockito.when(this.backend.request("GET", "graphs", null)).thenReturn(Map.of("graphs", List.of()));
        this.controller.delete("shop");
        Mockito.verify(this.backend, Mockito.never()).request(
                Mockito.eq("DELETE"), Mockito.anyString(), Mockito.isNull());
        Mockito.verify(this.spaces).delete(this.administrator, "shop");
    }

    @Test
    public void testPermissionAndStandaloneRejectionPrecedeCleanup() {
        this.authorized = false;
        expect(ForbiddenException.class, () -> this.controller.delete("shop"));
        Mockito.when(this.config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        expect(IllegalArgumentException.class, () -> this.controller.delete("shop"));
        Mockito.verifyNoInteractions(this.backend, this.space, this.users, this.spaces);
    }
}
