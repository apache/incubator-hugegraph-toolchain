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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.Property;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.SchemaService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.IdStrategy;

public class SchemaServiceViewTest {

    @Test
    public void testGetSchemaViewReturnsVertexPropertyTypes() throws Exception {
        PropertyKeyEntity name = PropertyKeyEntity.builder()
                                                  .name("name")
                                                  .dataType(DataType.TEXT)
                                                  .cardinality(Cardinality.SINGLE)
                                                  .build();
        VertexLabelEntity person = VertexLabelEntity.builder()
                                                     .name("person")
                                                     .idStrategy(IdStrategy.PRIMARY_KEY)
                                                     .primaryKeys(Collections.singletonList(
                                                             "name"))
                                                     .properties(Collections.singleton(
                                                             new Property(
                                                                     "name", false,
                                                                     DataType.TEXT,
                                                                     Cardinality.SINGLE)))
                                                     .build();
        SchemaService service = service(Collections.singletonList(name),
                                        Collections.singletonList(person));

        SchemaService.SchemaView view = service.getSchemaView(
                                                Mockito.mock(HugeClient.class));

        Assert.assertEquals(1, view.getVertices().size());
        Map<String, Object> vertex = view.getVertices().get(0);
        Assert.assertEquals("person", vertex.get("id"));
        Assert.assertEquals(Collections.singletonList("name"),
                            vertex.get("primary_keys"));
        Assert.assertEquals(Collections.singletonMap("name", "text"),
                            vertex.get("properties"));
        Assert.assertTrue(view.getEdges().isEmpty());
    }

    @Test
    public void testGetSchemaViewRejectsMissingPropertyKey() throws Exception {
        VertexLabelEntity person = VertexLabelEntity.builder()
                                                     .name("person")
                                                     .idStrategy(IdStrategy.DEFAULT)
                                                     .properties(Collections.singleton(
                                                             new Property(
                                                                     "missing", true,
                                                                     DataType.TEXT,
                                                                     Cardinality.SINGLE)))
                                                     .build();
        SchemaService service = service(Collections.emptyList(),
                                        Collections.singletonList(person));

        org.apache.hugegraph.testutil.Assert.assertThrows(
                InternalException.class, () -> {
            service.getSchemaView(Mockito.mock(HugeClient.class));
        });
    }

    private static SchemaService service(List<PropertyKeyEntity> propertyKeys,
                                         List<VertexLabelEntity> vertexLabels)
                                         throws Exception {
        PropertyKeyService pkService = Mockito.mock(PropertyKeyService.class);
        VertexLabelService vlService = Mockito.mock(VertexLabelService.class);
        EdgeLabelService elService = Mockito.mock(EdgeLabelService.class);
        Mockito.when(pkService.list(Mockito.any(HugeClient.class)))
               .thenReturn(propertyKeys);
        Mockito.when(vlService.list(Mockito.any(HugeClient.class)))
               .thenReturn(vertexLabels);
        Mockito.when(elService.list(Mockito.any(HugeClient.class)))
               .thenReturn(Collections.emptyList());

        SchemaService service = new SchemaService();
        inject(service, "pkService", pkService);
        inject(service, "vlService", vlService);
        inject(service, "elService", elService);
        return service;
    }

    private static void inject(Object target, String name, Object value)
                               throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
