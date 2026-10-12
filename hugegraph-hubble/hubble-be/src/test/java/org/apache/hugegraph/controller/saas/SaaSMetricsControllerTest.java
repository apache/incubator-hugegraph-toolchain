/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.controller.saas;

import java.util.HashMap;
import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.saas.PrometheusService;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SaaSMetricsControllerTest {

    @Test
    public void testMetricsSerializesUnavailableElementCounts() {
        HugeClient client = Mockito.mock(HugeClient.class);
        GraphSpaceService graphSpaceService =
                Mockito.mock(GraphSpaceService.class);
        PrometheusService prometheusService =
                Mockito.mock(PrometheusService.class);
        SaaSMetricsController controller = new SaaSMetricsController() {
            @Override
            protected HugeClient authClient(String graphSpace, String graph) {
                return client;
            }
        };
        ReflectionTestUtils.setField(controller, "graphSpaceService",
                                     graphSpaceService);
        ReflectionTestUtils.setField(controller, "prometheusService",
                                     prometheusService);

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("gsCount", 1L);
        metrics.put("gCount", 2L);
        metrics.put("vCount", null);
        metrics.put("eCount", null);
        metrics.put("vlCount", 3L);
        metrics.put("elCount", 4L);
        metrics.put("preDayTaskCount", 5L);
        Mockito.when(graphSpaceService.metrics(client)).thenReturn(metrics);
        Mockito.when(prometheusService.queryCountOffSet1Day(
                             Mockito.anyString())).thenReturn(6L);

        Object result = controller.metrics();
        JsonNode json = new ObjectMapper().valueToTree(result);

        Assert.assertTrue(json.get("vertex-count").isNull());
        Assert.assertTrue(json.get("edge-count").isNull());
        Assert.assertEquals(1L, json.get("graph-space-count").longValue());
        Assert.assertEquals(2L, json.get("graph-count").longValue());
    }
}
