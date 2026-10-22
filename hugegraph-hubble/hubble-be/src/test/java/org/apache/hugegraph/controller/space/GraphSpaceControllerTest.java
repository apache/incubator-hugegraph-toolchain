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

package org.apache.hugegraph.controller.space;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.GraphSpaceManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.space.BuiltInEntity;
import org.apache.hugegraph.entity.space.GraphSpaceEntity;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.handler.ExceptionAdvisor;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GraphSpaceControllerTest {

    @Test
    public void testAnonymousBriefListUsesMetadataRoute() throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        GraphSpaceService service = Mockito.mock(GraphSpaceService.class);
        TestGraphSpaceController controller = new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        AuthModeService authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(authMode.anonymous()).thenReturn(true);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "authMode", authMode);
        ReflectionTestUtils.setField(controller, "graphSpaceService", service);

        MockMvcBuilders.standaloneSetup(controller).build()
                       .perform(get("/api/v1.3/graphspaces")
                                .param("brief", "true"))
                       .andExpect(status().isOk());

        Mockito.verify(service).queryBriefPage(client, "", "", 1, 100,
                                               true, false, "");
        Mockito.verifyNoMoreInteractions(service);
    }

    @Test
    public void testBriefListForwardsSearchPageAndSelectedSpace() throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        GraphSpaceService service = Mockito.mock(GraphSpaceService.class);
        TestGraphSpaceController controller = new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        AuthModeService authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(authMode.anonymous()).thenReturn(true);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "authMode", authMode);
        ReflectionTestUtils.setField(controller, "graphSpaceService", service);

        MockMvcBuilders.standaloneSetup(controller).build()
                       .perform(get("/api/v1.3/graphspaces")
                                .param("brief", "true").param("query", "alias")
                                .param("page_no", "2").param("page_size", "100")
                                .param("selected", "late_space"))
                       .andExpect(status().isOk());

        Mockito.verify(service).queryBriefPage(client, "alias", "", 2, 100,
                                               true, false, "late_space");
        Mockito.verifyNoMoreInteractions(service);
    }

    @Test
    public void testHttpStoragePrecisionSurvivesEntityWireWithoutCoercingLegacyConfigs() throws Exception {
        String[] values = {"524288", "\"524288\"", "null", "-1", "{}"};
        for (int i = 0; i < values.length; i++) {
            okhttp3.Response response = Mockito.mock(okhttp3.Response.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(response.code()).thenReturn(200);
            Mockito.when(response.body().string()).thenReturn(
                    "{\"name\":\"shop\",\"storage_used_kb\":" + values[i] +
                    ",\"storage_used\":3,\"storage_percent\":0.03}");
            GraphSpace space = new RestResult(response).readObject(GraphSpace.class);
            GraphSpaceEntity entity = GraphSpaceEntity.fromGraphSpace(space);
            Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(entity), Map.class);
            Assert.assertEquals(i == 0 ? 524288 : null, wire.get("storage_used_kb"));
            Assert.assertEquals(3, wire.get("storage_used"));
            Assert.assertEquals(0.03, (Double) wire.get("storage_percent"), 0.000001);
        }
    }

    @Test
    public void testStandaloneDetailNeverContainsDataPlaneSecrets() {
        GraphSpaceController controller = new GraphSpaceController();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "graphSpaceService",
                                     new GraphSpaceService());

        @SuppressWarnings("unchecked")
        Map<String, Object> detail =
                (Map<String, Object>) controller.get("DEFAULT");

        Assert.assertEquals("DEFAULT", detail.get("name"));
        Assert.assertFalse(detail.containsKey("dp_username"));
        Assert.assertFalse(detail.containsKey("dp_password"));
        Assert.assertFalse(detail.containsKey("configs"));
    }

    @Test
    public void testPdDetailResponseNeverContainsDataPlaneSecrets() {
        HugeClient client = Mockito.mock(HugeClient.class);
        GraphSpaceManager manager = Mockito.mock(GraphSpaceManager.class);
        AuthManager auth = Mockito.mock(AuthManager.class);
        GraphsService graphsService = Mockito.mock(GraphsService.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpace graphSpace = new GraphSpace("public");
        graphSpace.setDpUserName("dp-user");
        graphSpace.setDpPassWord("dp-secret");
        graphSpace.setConfigs(new java.util.HashMap<>());
        Mockito.when(client.graphSpace()).thenReturn(manager);
        Mockito.when(client.auth()).thenReturn(auth);
        Mockito.when(manager.getGraphSpace("public")).thenReturn(graphSpace);
        Mockito.when(auth.isSuperAdmin()).thenReturn(false);
        Mockito.when(graphsService.listGraphNames(client, "public", ""))
               .thenReturn(java.util.Collections.emptySet());

        GraphSpaceService service = new GraphSpaceService();
        ReflectionTestUtils.setField(service, "graphsService", graphsService);
        ReflectionTestUtils.setField(service, "userService", userService);
        TestGraphSpaceController controller =
                new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "graphSpaceService", service);

        @SuppressWarnings("unchecked")
        Map<String, Object> detail =
                (Map<String, Object>) controller.get("public");

        Assert.assertEquals("public", detail.get("name"));
        Assert.assertFalse(detail.containsKey("dp_username"));
        Assert.assertFalse(detail.containsKey("dp_password"));
        Assert.assertFalse(detail.containsKey("configs"));
    }

    @Test
    public void testAnonymousEndpointsUseOnlyPublicGraphSpaces() {
        HugeClient client = Mockito.mock(HugeClient.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller =
                new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        AuthModeService authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(authMode.anonymous()).thenReturn(true);
        Mockito.when(graphSpaceService.listAnonymous(client))
               .thenReturn(java.util.Collections.singletonList("public"));
        Mockito.when(graphSpaceService.getAnonymous(client, "public"))
               .thenReturn(java.util.Collections.singletonMap("name", "public"));
        Mockito.when(graphSpaceService.isAuthForAnonymous(client, "public"))
               .thenReturn(false);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "authMode", authMode);
        ReflectionTestUtils.setField(controller, "graphSpaceService",
                                     graphSpaceService);

        @SuppressWarnings("unchecked")
        Map<String, Object> names = (Map<String, Object>) controller.list();
        @SuppressWarnings("unchecked")
        Map<String, Object> detail =
                (Map<String, Object>) controller.get("public");
        @SuppressWarnings("unchecked")
        Map<String, Object> auth =
                (Map<String, Object>) controller.isAuth("public");

        Assert.assertEquals(java.util.Collections.singletonList("public"),
                            names.get("graphspaces"));
        Assert.assertEquals("public", detail.get("name"));
        Assert.assertEquals(false, auth.get("auth"));
        Mockito.verify(graphSpaceService, Mockito.never()).listAll(client);
        Mockito.verify(graphSpaceService, Mockito.never())
               .isAuth(client, "public");
        Mockito.verify(graphSpaceService, Mockito.never())
               .getWithoutAdmins(client, "public");
        Mockito.verify(graphSpaceService, Mockito.never())
               .evCount(client, "public");
    }

    @Test
    public void testAuthenticatedEndpointsUseOnlyAccessibleGraphSpaces() {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller =
                new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        Mockito.when(userService.isSuperAdmin(client)).thenReturn(false);
        Mockito.when(graphSpaceService.listAccessible(client))
               .thenReturn(java.util.Collections.singletonList("member"));
        Mockito.when(graphSpaceService.isAuthForAccessible(client, "member"))
               .thenReturn(true);
        GraphSpaceEntity entity = new GraphSpaceEntity();
        entity.setName("member");
        Mockito.when(graphSpaceService.getAccessibleWithAdmins(client,
                                                               "member"))
               .thenReturn(entity);
        Mockito.when(graphSpaceService.toView(entity))
               .thenReturn(java.util.Collections.singletonMap("name",
                                                               "member"));
        controller.config = config;
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "graphSpaceService",
                                     graphSpaceService);

        @SuppressWarnings("unchecked")
        Map<String, Object> names = (Map<String, Object>) controller.list();
        @SuppressWarnings("unchecked")
        Map<String, Object> detail =
                (Map<String, Object>) controller.get("member");
        @SuppressWarnings("unchecked")
        Map<String, Object> auth =
                (Map<String, Object>) controller.isAuth("member");

        Assert.assertEquals(java.util.Collections.singletonList("member"),
                            names.get("graphspaces"));
        Assert.assertEquals("member", detail.get("name"));
        Assert.assertEquals(true, auth.get("auth"));
        Mockito.verify(graphSpaceService, Mockito.never()).listAll(client);
        Mockito.verify(graphSpaceService, Mockito.never())
               .isAuth(client, "member");
        Mockito.verify(graphSpaceService, Mockito.never())
               .getWithAdmins(client, "member");
    }

    @Test
    public void testApplyDefaultsForOptionalResourceLimits() {
        GraphSpaceEntity graphSpace = new GraphSpaceEntity();

        GraphSpaceController.applyResourceDefaults(graphSpace);

        Assert.assertEquals(100, graphSpace.getMaxGraphNumber());
        Assert.assertEquals(100, graphSpace.getMaxRoleNumber());
        Assert.assertEquals(128, graphSpace.getCpuLimit());
        Assert.assertEquals(128, graphSpace.getMemoryLimit());
        Assert.assertEquals(0, graphSpace.getComputeCpuLimit());
        Assert.assertEquals(0, graphSpace.getComputeMemoryLimit());
        Assert.assertEquals(10240, graphSpace.getStorageLimit());
        Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(graphSpace.convertReq()), Map.class);
        Assert.assertFalse(wire.containsKey("compute_cpu_limit"));
        Assert.assertFalse(wire.containsKey("compute_memory_limit"));
        Assert.assertEquals(128, wire.get("cpu_limit"));
    }

    @Test
    public void testOmittedComputeLimitsKeepInheritanceSentinel() {
        GraphSpaceEntity graphSpace = new GraphSpaceEntity();
        graphSpace.setCpuLimit(4);
        graphSpace.setMemoryLimit(8);

        GraphSpaceController.applyResourceDefaults(graphSpace);

        Assert.assertEquals(0, graphSpace.getComputeCpuLimit());
        Assert.assertEquals(0, graphSpace.getComputeMemoryLimit());
    }

    @Test
    public void testExplicitZeroSurvivesDefaultsAndNegativeComputeIsRejected() {
        GraphSpaceEntity graphSpace = JsonUtil.fromJson(
                "{\"compute_cpu_limit\":0,\"compute_memory_limit\":0}", GraphSpaceEntity.class);
        GraphSpaceController.applyResourceDefaults(graphSpace);
        Assert.assertEquals(0, graphSpace.getComputeCpuLimit());
        Assert.assertEquals(0, graphSpace.getComputeMemoryLimit());
        Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(graphSpace.convertReq()), Map.class);
        Assert.assertEquals(0, wire.get("compute_cpu_limit"));
        Assert.assertEquals(0, wire.get("compute_memory_limit"));
        graphSpace.setComputeCpuLimit(-1);
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> GraphSpaceController.applyResourceDefaults(graphSpace));
        graphSpace.setComputeCpuLimit(0);
        graphSpace.setComputeMemoryLimit(-1);
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> GraphSpaceController.applyResourceDefaults(graphSpace));
    }

    @Test
    public void testUpdateComputePresenceAndSdkWireSerialization() throws Exception {
        assertComputeUpdate("", null, null);
        assertComputeUpdate(",\"compute_cpu_limit\":null,\"compute_memory_limit\":null", 0, 0);
        assertComputeUpdate(",\"compute_cpu_limit\":0", 0, null);
        assertComputeUpdate(",\"compute_cpu_limit\":0,\"compute_memory_limit\":0", 0, 0);
        assertComputeUpdate(",\"compute_cpu_limit\":8,\"compute_memory_limit\":16", 8, 16);
    }

    private static void assertComputeUpdate(String fields, Integer expectedCpu,
                                            Integer expectedMemory) throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService users = Mockito.mock(UserService.class);
        GraphSpaceService service = Mockito.mock(GraphSpaceService.class);
        TestGraphSpaceController controller = controller(client, users, service);
        Mockito.when(users.isSuperAdmin(client)).thenReturn(true);
        GraphSpaceEntity previous = new GraphSpaceEntity();
        Mockito.when(service.getWithAdmins(client, "space")).thenReturn(previous);
        Mockito.when(service.toView(previous)).thenReturn(java.util.Collections.emptyMap());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(put("/api/v1.3/graphspaces/space")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cpu_limit\":4,\"memory_limit\":8" + fields + "}"))
           .andExpect(status().isOk());
        ArgumentCaptor<GraphSpace> updated = ArgumentCaptor.forClass(GraphSpace.class);
        Mockito.verify(service).update(Mockito.eq(client), updated.capture());
        Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(updated.getValue().convertReq()), Map.class);
        Assert.assertEquals(expectedCpu != null, wire.containsKey("compute_cpu_limit"));
        Assert.assertEquals(expectedMemory != null, wire.containsKey("compute_memory_limit"));
        Assert.assertEquals(expectedCpu, wire.get("compute_cpu_limit"));
        Assert.assertEquals(expectedMemory, wire.get("compute_memory_limit"));
        Mockito.verify(service, Mockito.never()).getWithoutAdmins(Mockito.any(), Mockito.anyString());
        Assert.assertFalse(JsonUtil.toJson(updated.getValue()).contains("Provided"));
    }

    @Test
    public void testApplyDefaultsPreservesExplicitResourceLimits() {
        GraphSpaceEntity graphSpace = new GraphSpaceEntity();
        graphSpace.setMaxGraphNumber(2);
        graphSpace.setMaxRoleNumber(3);
        graphSpace.setCpuLimit(4);
        graphSpace.setMemoryLimit(5);
        graphSpace.setComputeCpuLimit(6);
        graphSpace.setComputeMemoryLimit(7);
        graphSpace.setStorageLimit(8);

        GraphSpaceController.applyResourceDefaults(graphSpace);

        Assert.assertEquals(2, graphSpace.getMaxGraphNumber());
        Assert.assertEquals(3, graphSpace.getMaxRoleNumber());
        Assert.assertEquals(4, graphSpace.getCpuLimit());
        Assert.assertEquals(5, graphSpace.getMemoryLimit());
        Assert.assertEquals(6, graphSpace.getComputeCpuLimit());
        Assert.assertEquals(7, graphSpace.getComputeMemoryLimit());
        Assert.assertEquals(8, graphSpace.getStorageLimit());
    }

    @Test
    public void testCreateUsesHubbleAuthenticationMode() throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller = controller(
                                              client, userService,
                                              graphSpaceService);
        AuthModeService authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(authMode.enabled()).thenReturn(true);
        Mockito.when(userService.isSuperAdmin(client)).thenReturn(true);
        ReflectionTestUtils.setField(controller, "authMode", authMode);

        GraphSpaceEntity graphSpace = new GraphSpaceEntity();
        graphSpace.setName("secured");
        graphSpace.setAuth(false);
        Mockito.when(graphSpaceService.getWithAdmins(client, "secured"))
               .thenReturn(graphSpace);
        Mockito.when(graphSpaceService.toView(graphSpace))
               .thenReturn(java.util.Collections.singletonMap("name",
                                                               "secured"));

        controller.add(graphSpace);

        ArgumentCaptor<GraphSpace> created =
                ArgumentCaptor.forClass(GraphSpace.class);
        Mockito.verify(graphSpaceService).create(Mockito.eq(client),
                                                 created.capture());
        Assert.assertTrue(created.getValue().isAuth());
    }

    @Test
    public void testOnlySuperadminCanMutateGraphSpaces() throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller = controller(
                                              client, userService,
                                              graphSpaceService);
        Mockito.when(userService.isSuperAdmin(client)).thenReturn(false);

        assertForbidden(() -> controller.add(new GraphSpaceEntity()));
        assertForbidden(() -> controller.update("foreign",
                                                new GraphSpaceEntity()));
        assertForbidden(() -> controller.delete("foreign"));
        assertForbidden(() -> controller.initBuiltIn(new BuiltInEntity()));

        Mockito.verifyNoInteractions(graphSpaceService);
        Mockito.when(userService.isSuperAdmin(client)).thenReturn(true);
        Assert.assertSame(client, controller.requireGlobalManager());
    }

    @Test
    public void testAnonymousModeCannotMutateGraphSpaces() throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller = controller(
                                              client, userService,
                                              graphSpaceService);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        AuthModeService authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(authMode.anonymous()).thenReturn(true);
        ReflectionTestUtils.setField(controller, "authMode",
                                     authMode);

        assertForbidden(() -> controller.add(new GraphSpaceEntity()));

        Mockito.verifyNoInteractions(userService, graphSpaceService);
    }

    @Test
    public void testForbiddenGraphSpaceMutationUsesHttpAndBody403()
            throws Exception {
        HugeClient client = Mockito.mock(HugeClient.class);
        UserService userService = Mockito.mock(UserService.class);
        GraphSpaceService graphSpaceService = Mockito.mock(
                                                GraphSpaceService.class);
        TestGraphSpaceController controller = controller(
                                              client, userService,
                                              graphSpaceService);
        Mockito.when(userService.isSuperAdmin(client)).thenReturn(false);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                                     .setControllerAdvice(
                                             new ExceptionAdvisor())
                                     .build();

        mvc.perform(post("/api/v1.3/graphspaces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.status").value(403));

        Mockito.verifyNoInteractions(graphSpaceService);
    }

    private static TestGraphSpaceController controller(
            HugeClient client, UserService userService,
            GraphSpaceService graphSpaceService) throws Exception {
        TestGraphSpaceController controller =
                new TestGraphSpaceController(client);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        controller.config = config;
        ReflectionTestUtils.setField(controller, "graphSpaceService",
                                     graphSpaceService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        Field baseUserService = BaseController.class.getDeclaredField(
                                "userService");
        baseUserService.setAccessible(true);
        baseUserService.set(controller, userService);
        return controller;
    }

    private static ForbiddenException assertForbidden(Action action) {
        try {
            action.run();
            Assert.fail("Expected ForbiddenException");
            return null;
        } catch (ForbiddenException e) {
            return e;
        }
    }

    private static class TestGraphSpaceController
            extends GraphSpaceController {

        private final HugeClient client;

        TestGraphSpaceController(HugeClient client) {
            this.client = client;
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            return this.client;
        }

        private HugeClient requireGlobalManager() {
            return this.requireGraphSpaceAdministrator();
        }
    }

    @FunctionalInterface
    private interface Action {

        void run();
    }
}
