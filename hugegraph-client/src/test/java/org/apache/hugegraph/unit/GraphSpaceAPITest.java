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
