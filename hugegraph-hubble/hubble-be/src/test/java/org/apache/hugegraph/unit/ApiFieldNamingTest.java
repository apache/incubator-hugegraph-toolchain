/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
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

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.service.graph.GraphService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.testutil.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiFieldNamingTest {

    @Test
    public void testExecuteHistoryUsesGraphField() throws Exception {
        ExecuteHistory history = new ExecuteHistory();
        history.setGraph("hugegraph");

        String json = new ObjectMapper().writeValueAsString(history);

        Assert.assertTrue(json.contains("\"graph\":\"hugegraph\""));
        Assert.assertFalse(json.contains("\"graphe\""));
    }

    @Test
    public void testGraphPropertiesUseLowerCamelCase() throws Exception {
        GraphService service = new GraphService();
        VertexLabelService vertexLabels = Mockito.mock(VertexLabelService.class);
        EdgeLabelService edgeLabels = Mockito.mock(EdgeLabelService.class);
        VertexLabelEntity vertex = Mockito.mock(VertexLabelEntity.class);
        EdgeLabelEntity edge = Mockito.mock(EdgeLabelEntity.class);
        HugeClient client = Mockito.mock(HugeClient.class);
        Mockito.when(vertexLabels.get("person", client)).thenReturn(vertex);
        Mockito.when(edgeLabels.get("knows", client)).thenReturn(edge);
        this.setField(service, "vlService", vertexLabels);
        this.setField(service, "elService", edgeLabels);

        Map<String, Object> vertexFields =
                service.getVertexProperties(client, "person");
        Map<String, Object> edgeFields =
                service.getEdgeProperties(client, "knows");

        Assert.assertTrue(vertexFields.containsKey("nullableProps"));
        Assert.assertFalse(vertexFields.containsKey("NullableProps"));
        Assert.assertTrue(edgeFields.containsKey("nullableProps"));
        Assert.assertFalse(edgeFields.containsKey("NullableProps"));
    }

    private void setField(Object target, String name, Object value)
                          throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
