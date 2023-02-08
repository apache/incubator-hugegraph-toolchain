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

package org.apache.hugegraph.api;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class SchemaApiTest extends BaseApiTest {

    @Test
    public void testList() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();

        Map<String, List<SchemaElement>> schemas = schemaAPI.list();

        Assert.assertEquals(4, schemas.size());
        Assert.assertTrue(schemas.containsKey("propertykeys"));
        Assert.assertTrue(schemas.containsKey("vertexlabels"));
        Assert.assertTrue(schemas.containsKey("edgelabels"));
        Assert.assertTrue(schemas.containsKey("indexlabels"));
        Assert.assertEquals(7, schemas.get("propertykeys").size());
        Assert.assertEquals(3, schemas.get("vertexlabels").size());
        Assert.assertEquals(2, schemas.get("edgelabels").size());
        Assert.assertTrue(schemas.get("indexlabels").isEmpty());
    }
}
