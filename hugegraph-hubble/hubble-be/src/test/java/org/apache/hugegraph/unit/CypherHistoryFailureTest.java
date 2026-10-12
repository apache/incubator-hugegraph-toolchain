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

package org.apache.hugegraph.unit;

import org.apache.hugegraph.controller.query.CypherController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class CypherHistoryFailureTest {

    @Test
    public void testSyncFailurePersistsOnlyControlledReasonCode() {
        HugeClient client = Mockito.mock(HugeClient.class);
        QueryService queryService = Mockito.mock(QueryService.class);
        ExecuteHistoryService historyService =
                Mockito.mock(ExecuteHistoryService.class);
        RuntimeException unsafe = new RuntimeException(
                "No signature of method: secret.Groovy.stack()");
        Mockito.when(queryService.executeCypherQuery(
                     Mockito.eq(client), Mockito.anyString()))
               .thenThrow(unsafe);

        TestController controller = new TestController(client);
        ReflectionTestUtils.setField(controller, "queryService", queryService);
        ReflectionTestUtils.setField(controller, "historyService", historyService);

        Assert.assertThrows(RuntimeException.class,
                            () -> controller.execute("DEFAULT", "hugegraph",
                                                     "MATCH INVALID"));

        ArgumentCaptor<ExecuteHistory> saved =
                ArgumentCaptor.forClass(ExecuteHistory.class);
        Mockito.verify(historyService).update(saved.capture());
        Assert.assertEquals("GREMLIN_EXECUTION_FAILED",
                            saved.getValue().getFailureReason());
        Assert.assertFalse(saved.getValue().getFailureReason()
                                .contains("signature"));
    }

    @Test
    public void testAsyncSubmissionFailureDoesNotClaimExecutionFailure() {
        HugeClient client = Mockito.mock(HugeClient.class);
        QueryService queryService = Mockito.mock(QueryService.class);
        ExecuteHistoryService historyService =
                Mockito.mock(ExecuteHistoryService.class);
        Mockito.when(queryService.executeCypherAsyncTask(
                     Mockito.eq(client), Mockito.anyString()))
               .thenThrow(new RuntimeException("secret backend detail"));

        TestController controller = new TestController(client);
        ReflectionTestUtils.setField(controller, "queryService", queryService);
        ReflectionTestUtils.setField(controller, "historyService", historyService);

        Assert.assertThrows(RuntimeException.class,
                            () -> controller.executeAsyncTask(
                                    "DEFAULT", "hugegraph",
                                    new GremlinQuery("MATCH INVALID")));

        ArgumentCaptor<ExecuteHistory> saved =
                ArgumentCaptor.forClass(ExecuteHistory.class);
        Mockito.verify(historyService).update(saved.capture());
        Assert.assertNull(saved.getValue().getFailureReason());
    }

    private static class TestController extends CypherController {

        private final HugeClient client;

        TestController(HugeClient client) {
            this.client = client;
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            return this.client;
        }
    }
}
