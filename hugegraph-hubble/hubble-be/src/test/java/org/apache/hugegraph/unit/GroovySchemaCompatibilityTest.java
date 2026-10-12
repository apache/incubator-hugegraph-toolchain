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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.service.schema.GroovySchemaCompatibility;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;

public class GroovySchemaCompatibilityTest {

    @Test
    public void shouldPreserveLegacyGroovyResponse() {
        SchemaManager schema = mock(SchemaManager.class);
        when(schema.getGroovySchema()).thenReturn("legacy");

        org.junit.Assert.assertEquals("legacy",
                                      GroovySchemaCompatibility.export(schema));
    }

    @Test
    public void shouldRebuildGroovyFromStructuredSchema() {
        SchemaManager schema = mock(SchemaManager.class);
        when(schema.getGroovySchema()).thenReturn("");

        PropertyKey name = new PropertyKey.BuilderImpl("person's name", schema)
                           .asText().valueSet().ifNotExist().build();
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("regions", Arrays.asList("east;west", "north\\south"));
        nested.put("empty", Collections.emptyList());
        nested.put("emptyMap", Collections.emptyMap());
        name.userdata().put("constraints", nested);
        name.userdata().put("codes", new int[]{1, 2});
        VertexLabel person = new VertexLabel.BuilderImpl("person", schema)
                             .usePrimaryKeyId()
                             .properties("person's name")
                             .primaryKeys("person's name")
                             .enableLabelIndex(false).ifNotExist().build();
        EdgeLabel knows = new EdgeLabel.BuilderImpl("knows", schema)
                          .link("person", "person")
                          .properties("person's name")
                          .multiTimes().ifNotExist().build();
        IndexLabel byName = new IndexLabel.BuilderImpl("by_name", schema)
                            .onV("person").by("person's name")
                            .secondary().ifNotExist().build();
        when(schema.getPropertyKeys()).thenReturn(Arrays.asList(name));
        when(schema.getVertexLabels()).thenReturn(Arrays.asList(person));
        when(schema.getEdgeLabels()).thenReturn(Arrays.asList(knows));
        when(schema.getIndexLabels()).thenReturn(Arrays.asList(byName));

        String result = GroovySchemaCompatibility.export(schema);

        assertTrue(result.contains("propertyKey('person\\'s name').asText()"));
        assertTrue(result.contains(".valueSet()"));
        assertTrue(result.contains(".userdata('codes', [1, 2])"));
        assertTrue(result.contains("'regions': ['east;west', " +
                                   "'north\\\\south']"));
        assertTrue(result.contains("'empty': []"));
        assertTrue(result.contains("'emptyMap': [:]"));
        assertTrue(result.contains("vertexLabel('person').usePrimaryKeyId()"));
        assertTrue(result.contains(".primaryKeys('person\\'s name')"));
        assertTrue(result.contains("edgeLabel('knows').link('person', 'person')"));
        assertTrue(result.contains(".multiTimes()"));
        assertTrue(result.contains("indexLabel('by_name').onV('person')"));
        assertTrue(result.contains(".by('person\\'s name').secondary()"));
    }

    @Test
    public void shouldReturnEmptyForActuallyEmptyStructuredSchema() {
        SchemaManager schema = mock(SchemaManager.class);
        when(schema.getGroovySchema()).thenReturn("");
        when(schema.getPropertyKeys()).thenReturn(Collections.emptyList());
        when(schema.getVertexLabels()).thenReturn(Collections.emptyList());
        when(schema.getEdgeLabels()).thenReturn(Collections.emptyList());
        when(schema.getIndexLabels()).thenReturn(Collections.emptyList());

        org.junit.Assert.assertEquals("",
                                      GroovySchemaCompatibility.export(schema));
    }

    @Test
    public void shouldSplitOnlyOutsideQuotedGroovyStrings() {
        String content = "graph.schema().propertyKey('a;\\'b').userdata(" +
                         "'path', 'c:\\\\tmp;east\\nwest').create();\r\n" +
                         "graph.schema().propertyKey(\"second;key\").create();";

        List<String> statements =
                GroovySchemaCompatibility.splitStatements(content);

        org.junit.Assert.assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'a;\\'b'"));
        assertTrue(statements.get(0).contains("c:\\\\tmp;east\\nwest"));
        assertTrue(statements.get(1).contains("\"second;key\""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnterminatedGroovyStrings() {
        GroovySchemaCompatibility.splitStatements(
                "graph.schema().propertyKey('unfinished);");
    }
}
