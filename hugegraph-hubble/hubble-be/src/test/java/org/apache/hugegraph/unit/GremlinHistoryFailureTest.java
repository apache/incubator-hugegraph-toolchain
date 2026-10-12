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

import org.apache.hugegraph.controller.query.GremlinQueryController;
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

public class GremlinHistoryFailureTest {

    @Test
    public void testSyncFailurePersistsOnlyControlledReasonCode() {
        HugeClient client = Mockito.mock(HugeClient.class);
        QueryService queryService = Mockito.mock(QueryService.class);
        ExecuteHistoryService historyService =
                Mockito.mock(ExecuteHistoryService.class);
        RuntimeException unsafe = new RuntimeException(
                "No signature of method: secret.Groovy.stack()");
        Mockito.when(queryService.executeGremlinQuery(
                     Mockito.eq(client), Mockito.any(GremlinQuery.class)))
               .thenThrow(unsafe);

        TestController controller = new TestController(client);
        ReflectionTestUtils.setField(controller, "queryService", queryService);
        ReflectionTestUtils.setField(controller, "historyService", historyService);

        Assert.assertThrows(RuntimeException.class,
                            () -> controller.gremlin("DEFAULT", "hugegraph",
                                                     new GremlinQuery("g.bad()")));

        ArgumentCaptor<ExecuteHistory> saved =
                ArgumentCaptor.forClass(ExecuteHistory.class);
        Mockito.verify(historyService).update(saved.capture());
        Assert.assertEquals("GREMLIN_EXECUTION_FAILED",
                            saved.getValue().getFailureReason());
        Assert.assertFalse(saved.getValue().getFailureReason()
                                .contains("signature"));
    }

    private static class TestController extends GremlinQueryController {

        private final HugeClient client;

        TestController(HugeClient client) {
            this.client = client;
        }

        @Override
        protected HugeClient authGremlinClient(String graphSpace, String graph) {
            return this.client;
        }
    }
}
