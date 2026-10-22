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

package org.apache.hugegraph.unit;

import java.util.Collections;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.graph.Vertices;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class VertexAPITest {

    @Test
    public void testZeroLimitSendsAZeroResultRequest() {
        RestClient client = Mockito.mock(RestClient.class);
        RestResult response = Mockito.mock(RestResult.class);
        Mockito.when(response.readObject(Vertices.class))
               .thenReturn(new Vertices(Collections.emptyList(), null));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        Mockito.when(client.get(Mockito.eq("graphspaces/space/graphs/graph/graph/vertices"),
                                params.capture())).thenReturn(response);

        Assert.assertTrue(new GraphManager(client, "space", "graph").listVertices(0).isEmpty());
        Assert.assertEquals(0, params.getValue().get("limit"));
        Mockito.verify(client).get(Mockito.anyString(), Mockito.anyMap());
    }
}
