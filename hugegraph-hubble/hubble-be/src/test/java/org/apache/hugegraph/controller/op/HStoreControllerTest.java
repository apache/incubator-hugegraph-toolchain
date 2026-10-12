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

package org.apache.hugegraph.controller.op;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.driver.HStoreManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerCapabilityUnavailableException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.testutil.Assert;

public class HStoreControllerTest {

    @Test
    public void testMissingApiStatusesMapToCapabilityUnavailable() {
        for (int status : new int[]{404, 405, 501}) {
            ServerException server = serverException(status);
            RuntimeException mapped = HStoreController.mapHstoreFailure(
                                      server, "node-2", "node-1");
            Assert.assertTrue(mapped instanceof
                              ServerCapabilityUnavailableException);
            Assert.assertSame(server, mapped.getCause());
        }
    }

    @Test
    public void testOtherServerFailuresRemainUnchanged() {
        for (int status : new int[]{400, 401, 403, 409, 500, 503}) {
            ServerException server = serverException(status);
            Assert.assertSame(server,
                              HStoreController.mapHstoreFailure(server));
        }
    }

    @Test
    public void testStartupFailureReportsSuccessfulAndFailedNodes() {
        this.assertPartialNodeFailure(true);
    }

    @Test
    public void testShutdownFailureReportsSuccessfulAndFailedNodes() {
        this.assertPartialNodeFailure(false);
    }

    private void assertPartialNodeFailure(boolean startup) {
        HugeClient client = Mockito.mock(HugeClient.class);
        HStoreManager manager = Mockito.mock(HStoreManager.class);
        Mockito.when(client.hStoreManager()).thenReturn(manager);
        ServerException server = serverException(404);
        if (startup) {
            Mockito.doThrow(server).when(manager).nodeStartup("node-2");
        } else {
            Mockito.doThrow(server).when(manager).nodeShutdown("node-2");
        }
        HStoreController controller = new TestHStoreController(client);
        Map<String, java.util.List<String>> request = ImmutableMap.of(
                "nodes", ImmutableList.of("node-1", "node-2"));

        try {
            if (startup) {
                controller.nodesStartup(request);
            } else {
                controller.nodesShutdown(request);
            }
            Assert.fail("Expected unavailable HStore capability");
        } catch (ServerCapabilityUnavailableException e) {
            Assert.assertSame(server, e.getCause());
            Assert.assertEquals("node-2", e.args()[0]);
            Assert.assertEquals(ImmutableList.of("node-1"), e.args()[1]);
        }
    }

    private static ServerException serverException(int status) {
        ServerException server = new ServerException("upstream-" + status);
        server.status(status);
        return server;
    }

    private static final class TestHStoreController extends HStoreController {

        private final HugeClient client;

        private TestHStoreController(HugeClient client) {
            this.client = client;
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            return this.client;
        }
    }
}
