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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import java.util.Map;

import org.apache.hugegraph.api.space.GraphSpaceAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.util.JsonUtil;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GraphSpaceAPITest extends BaseUnitTest {

    private RestClient mockClient;
    private GraphSpaceAPI graphSpaceAPI;

    @Before
    public void setup() {
        this.mockClient = Mockito.mock(RestClient.class);
        this.graphSpaceAPI = new GraphSpaceAPI(this.mockClient);
    }

    @Test
    public void testPreciseStorageUsageSurvivesSerialization() {
        GraphSpace space = JsonUtil.fromJson(
                "{\"name\":\"shop\",\"storage_used\":0,\"storage_used_kb\":524288}", GraphSpace.class);
        Assert.assertEquals(Long.valueOf(524288), space.getStorageUsedKb());
        GraphSpace copy = JsonUtil.fromJson(JsonUtil.toJson(space), GraphSpace.class);
        Assert.assertEquals(space.getStorageUsedKb(), copy.getStorageUsedKb());
        GraphSpace legacy = JsonUtil.fromJson("{\"storage_used\":2}", GraphSpace.class);
        Assert.assertNull(legacy.getStorageUsedKb());
        Assert.assertEquals(2, legacy.getStorageUsed());
        Assert.assertFalse(JsonUtil.toJson(space.convertReq()).contains("storage_used_kb"));
    }

    @Test
    public void testHttpPrecisionAcceptsOnlyNonnegativeIntegerMetrics() throws Exception {
        String[] values = {"0", "524288", "9223372036854775807", "null", "\"524288\"",
                           "\"invalid\"", "-1", "1.5", "524288.0", "9223372036854775808",
                           "{}", "[]", "true"};
        for (int i = 0; i < values.length; i++) {
            okhttp3.Response response = Mockito.mock(okhttp3.Response.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(response.code()).thenReturn(200);
            Mockito.when(response.body().string()).thenReturn(
                    "{\"name\":\"shop\",\"storage_used_kb\":" + values[i] +
                    ",\"storage_used\":3,\"storage_percent\":0.03}");
            RestResult result = new RestResult(response);
            Mockito.when(this.mockClient.get("graphspaces", "shop")).thenReturn(result);
            GraphSpace space = this.graphSpaceAPI.get("shop");
            Assert.assertEquals(i < 3 ? Long.valueOf(values[i]) : null, space.getStorageUsedKb());
            Assert.assertEquals(3, space.getStorageUsed());
            Assert.assertEquals(0.03, space.getStoragePercent(), 0.000001);
        }
    }

    @Test
    public void testPartialUpdateOmitsUnsetComputeLimits() {
        GraphSpace update = new GraphSpace("shop").setCpuLimit(4).setMemoryLimit(8);
        update.setDescription("updated");
        Map<?, ?> wire = this.updateWire(update);
        Assert.assertFalse(wire.containsKey("compute_cpu_limit"));
        Assert.assertFalse(wire.containsKey("compute_memory_limit"));
        Assert.assertEquals(4, wire.get("cpu_limit"));
        Assert.assertEquals("updated", wire.get("description"));
    }

    @Test
    public void testUpdatePreservesExplicitZeroAndPositiveComputeLimits() {
        GraphSpace update = new GraphSpace("shop");
        update.setComputeCpuLimit(0);
        Map<?, ?> wire = this.updateWire(update);
        Assert.assertEquals(0, wire.get("compute_cpu_limit"));
        Assert.assertFalse(wire.containsKey("compute_memory_limit"));
        update.setComputeCpuLimit(16);
        update.setComputeMemoryLimit(32);
        wire = this.updateWire(update);
        Assert.assertEquals(16, wire.get("compute_cpu_limit"));
        Assert.assertEquals(32, wire.get("compute_memory_limit"));
    }

    @Test
    public void testReadModifyWritePreservesComputeLimitsFromHttpGet() throws Exception {
        for (Integer limit : new Integer[]{null, 0, 16}) {
            okhttp3.Response response = Mockito.mock(okhttp3.Response.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(response.code()).thenReturn(200);
            String limits = limit == null ? "" : ",\"compute_cpu_limit\":" + limit +
                                                ",\"compute_memory_limit\":" + limit;
            Mockito.when(response.body().string()).thenReturn("{\"name\":\"shop\"" + limits + "}");
            RestResult result = new RestResult(response);
            Mockito.when(this.mockClient.get("graphspaces", "shop")).thenReturn(result);
            GraphSpace update = this.graphSpaceAPI.get("shop");
            Assert.assertEquals(limit == null ? 0 : limit.intValue(), update.getComputeCpuLimit());
            Assert.assertEquals(limit == null ? 0 : limit.intValue(), update.getComputeMemoryLimit());
            update.setDescription("updated");
            Map<?, ?> wire = this.updateWire(update);
            Assert.assertEquals(limit != null, wire.containsKey("compute_cpu_limit"));
            Assert.assertEquals(limit != null, wire.containsKey("compute_memory_limit"));
            Assert.assertEquals(limit, wire.get("compute_cpu_limit"));
            Assert.assertEquals(limit, wire.get("compute_memory_limit"));
        }
    }

    @Test
    public void testRequestCopyKeepsOmittedAndExplicitComputeLimitsDistinct() {
        GraphSpace source = new GraphSpace("shop");
        Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(new GraphSpace.GraphSpaceReq(source)), Map.class);
        Assert.assertFalse(wire.containsKey("compute_cpu_limit"));
        Assert.assertFalse(wire.containsKey("compute_memory_limit"));
        source.setComputeMemoryLimit(0);
        wire = JsonUtil.fromJson(JsonUtil.toJson(new GraphSpace.GraphSpaceReq(source)), Map.class);
        Assert.assertFalse(wire.containsKey("compute_cpu_limit"));
        Assert.assertEquals(0, wire.get("compute_memory_limit"));
    }

    private Map<?, ?> updateWire(GraphSpace space) {
        Mockito.clearInvocations(this.mockClient);
        RestResult result = Mockito.mock(RestResult.class);
        Mockito.when(this.mockClient.put(Mockito.eq("graphspaces"), Mockito.eq("shop"), Mockito.anyMap()))
               .thenReturn(result);
        this.graphSpaceAPI.update(space);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(this.mockClient).put(Mockito.eq("graphspaces"), Mockito.eq("shop"), body.capture());
        Map<?, ?> wire = JsonUtil.fromJson(JsonUtil.toJson(body.getValue()), Map.class);
        return (Map<?, ?>) wire.get("update");
    }

    @Test
    public void testDeleteDefaultRoleDoesNotReadEmptyResponseBody() {
        RestResult mockResult = Mockito.mock(RestResult.class);
        Mockito.when(mockResult.readObject(Map.class))
               .thenThrow(new AssertionError("204 response has no body"));

        ArgumentCaptor<String> pathCaptor =
                ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor =
                ArgumentCaptor.forClass(Map.class);

        Mockito.when(this.mockClient.delete(pathCaptor.capture(),
                                            paramsCaptor.capture()))
               .thenReturn(mockResult);

        Map<String, String> response = this.graphSpaceAPI.deleteDefaultRole(
                "DEFAULT", "admin", "SPACE", "");

        Assert.assertEquals("graphspaces/DEFAULT/role", pathCaptor.getValue());
        Assert.assertEquals("admin", paramsCaptor.getValue().get("user"));
        Assert.assertEquals("SPACE", paramsCaptor.getValue().get("role"));
        Assert.assertEquals("admin", response.get("user"));
        Assert.assertEquals("SPACE", response.get("role"));
        Assert.assertEquals("DEFAULT", response.get("graphSpace"));
    }
}
