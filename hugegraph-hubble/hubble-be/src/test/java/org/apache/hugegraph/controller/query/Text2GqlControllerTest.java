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

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.Text2GqlService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class Text2GqlControllerTest {

    @Test
    public void testUsesAuthorizedClientForCurrentGraph() {
        HugeClient client = Mockito.mock(HugeClient.class);
        Text2GqlService service = Mockito.mock(Text2GqlService.class);
        BoundController controller = new BoundController(client);
        ReflectionTestUtils.setField(controller, "service", service);
        Text2GqlRequest request = new Text2GqlRequest();
        Mockito.when(service.generate(client, request)).thenReturn("g.V().limit(10)");

        assertEquals("g.V().limit(10)", controller.generate("my-space", "my-graph", request).get("gremlin"));
        assertEquals("my-space", controller.space);
        assertEquals("my-graph", controller.graph);
        Mockito.verify(service).generate(client, request);
        Mockito.verifyNoMoreInteractions(service);
    }

    @Test
    public void testAuthorizationFailureNeverCallsModel() {
        Text2GqlService service = Mockito.mock(Text2GqlService.class);
        BoundController controller = new BoundController(null);
        ReflectionTestUtils.setField(controller, "service", service);
        controller.failure = new ExternalException(403, "permission denied");
        try {
            controller.generate("private-space", "private-graph", new Text2GqlRequest());
            fail("Expected graph authorization failure");
        } catch (ExternalException exception) {
            assertSame(controller.failure, exception);
        }
        Mockito.verifyNoInteractions(service);
    }

    @Test
    public void testRequestToStringDoesNotExposeApiKey() {
        Text2GqlRequest request = new Text2GqlRequest();
        request.setApiKey("secret-key-never-log");
        assertFalse(request.toString().contains("secret-key-never-log"));
    }

    private static final class BoundController extends Text2GqlController {

        private final HugeClient client;
        private String space;
        private String graph;
        private ExternalException failure;

        private BoundController(HugeClient client) {
            this.client = client;
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            this.space = graphSpace;
            this.graph = graph;
            if (this.failure != null) {
                throw this.failure;
            }
            return this.client;
        }
    }
}
