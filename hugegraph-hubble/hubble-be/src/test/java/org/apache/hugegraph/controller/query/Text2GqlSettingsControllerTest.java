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

package org.apache.hugegraph.controller.query;

import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.Text2GqlSettingsRequest;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.query.Text2GqlSettingsService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Text2GqlSettingsControllerTest {

    @Test
    public void testAnonymousOrdinaryAndSpaceAdminCannotReadOrWriteModelSettings() {
        for (String level : new String[]{"ANONYMOUS", "USER", "SPACEADMIN"}) {
            Text2GqlSettingsService settings = Mockito.mock(Text2GqlSettingsService.class);
            TestController controller = new TestController(level);
            ReflectionTestUtils.setField(controller, "settings", settings);
            assertForbidden(controller::get);
            assertForbidden(() -> controller.update(new Text2GqlSettingsRequest()));
            Mockito.verifyNoInteractions(settings);
        }
    }

    @Test
    public void testOnlyAdministratorCanReadAndUpdateSafeDescriptor() {
        Text2GqlSettingsService settings = Mockito.mock(Text2GqlSettingsService.class);
        TestController controller = new TestController("ADMIN");
        ReflectionTestUtils.setField(controller, "settings", settings);
        Map<String, Object> safe = Map.of("has_api_key", true, "model", "shared-model");
        Text2GqlSettingsRequest request = new Text2GqlSettingsRequest();
        Mockito.when(settings.settings()).thenReturn(safe);
        Mockito.when(settings.save(request)).thenReturn(safe);
        assertEquals(safe, controller.get());
        assertEquals(safe, controller.update(request));
        Mockito.verify(settings).settings();
        Mockito.verify(settings).save(request);
    }

    @Test
    public void testMutationRequiresPutWithJsonAndHasNoPostAlias() throws Exception {
        Text2GqlSettingsService settings = Mockito.mock(Text2GqlSettingsService.class);
        TestController controller = new TestController("ADMIN");
        ReflectionTestUtils.setField(controller, "settings", settings);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/v1.3/settings/text2gql").contentType(MediaType.APPLICATION_JSON)
                   .content("{}"))
           .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/v1.3/settings/text2gql").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                   .content("enabled=true&api_key=secret"))
           .andExpect(status().isUnsupportedMediaType());
        Mockito.verifyNoInteractions(settings);
    }

    private static void assertForbidden(Runnable operation) {
        try {
            operation.run();
            fail("Expected server-side administrator authorization");
        } catch (ForbiddenException expected) {
            // Both descriptor reads and writes are privileged.
        }
    }

    private static final class TestController extends Text2GqlSettingsController {

        private final HugeClient client = Mockito.mock(HugeClient.class);

        private TestController(String level) {
            this.authMode = Mockito.mock(AuthModeService.class);
            Mockito.when(this.authMode.anonymous()).thenReturn("ANONYMOUS".equals(level));
            this.userService = Mockito.mock(UserService.class);
            Mockito.when(this.userService.userLevel(this.client, "current-user")).thenReturn(level);
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            return this.client;
        }

        @Override
        protected String getUser() {
            return "current-user";
        }
    }
}
