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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.controller.schema;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;

public class SchemaControllerSecurityTest {

    @Test
    public void testSchemaGroovyExportUsesSafeDownloadHeaders()
           throws Exception {
        String schema = "graph.schema().propertyKey('name').asText().create();";
        SchemaController controller = new TestSchemaController(schema);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.schemaGroovyExport("DEFAULT\r\nX-Bad: injected",
                                      "图 graph\";x", response);

        String header = response.getHeader("Content-Disposition");
        Assert.assertNotNull(header);
        Assert.assertTrue(header.startsWith("attachment;"));
        Assert.assertTrue(header.contains("filename=\""));
        Assert.assertTrue(header.contains("filename*=UTF-8''"));
        Assert.assertFalse(header.contains("fileName="));
        Assert.assertFalse(header.contains("\r"));
        Assert.assertFalse(header.contains("\n"));
        Assert.assertFalse(header.contains("\";x"));
        Assert.assertFalse(header.contains("图"));
        Assert.assertEquals("application/octet-stream",
                            response.getContentType());
        Assert.assertEquals(schema, new String(response.getContentAsByteArray(),
                                              StandardCharsets.UTF_8));
    }

    private static class TestSchemaController extends SchemaController {

        private final HugeClient client;

        TestSchemaController(String schema) {
            SchemaManager schemaManager = Mockito.mock(SchemaManager.class);
            Mockito.when(schemaManager.getGroovySchema()).thenReturn(schema);
            this.client = Mockito.mock(HugeClient.class);
            Mockito.when(this.client.schema()).thenReturn(schemaManager);
        }

        @Override
        protected HugeClient authClient(String graphSpace, String graph) {
            return this.client;
        }
    }
}
