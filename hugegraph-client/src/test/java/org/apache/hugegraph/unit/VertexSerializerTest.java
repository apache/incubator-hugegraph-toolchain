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

package org.apache.hugegraph.unit;

import java.util.Map;

import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class VertexSerializerTest extends BaseUnitTest {

    @Test
    public void testSerializeAndDeserializeVertex() {
        Vertex vertex = new Vertex("person");
        vertex.id("person:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);
        vertex.property("city", "Beijing");

        String json = serialize(vertex);
        Vertex vertexCopy = deserialize(json, Vertex.class);

        Assert.assertEquals("person:marko", vertexCopy.id());
        Assert.assertEquals("person", vertexCopy.label());
        Assert.assertEquals("vertex", vertexCopy.type());
        Map<String, Object> props = ImmutableMap.of("name", "marko",
                                                    "age", 29,
                                                    "city", "Beijing");
        Assert.assertEquals(props, vertexCopy.properties());
    }

    @Test
    public void testSerializeAndDeserializeVertexWithListProp() {
        Vertex vertex = new Vertex("person");
        vertex.id("person:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);
        vertex.property("city", ImmutableList.of("Hefei", "Wuhan"));

        String json = serialize(vertex);
        Vertex vertexCopy = deserialize(json, Vertex.class);

        Assert.assertEquals("person:marko", vertexCopy.id());
        Assert.assertEquals("person", vertexCopy.label());
        Assert.assertEquals("vertex", vertexCopy.type());
        Map<String, Object> props = ImmutableMap.of("name", "marko", "age", 29,
                                                    "city", ImmutableList.of("Hefei", "Wuhan"));
        Assert.assertEquals(props, vertexCopy.properties());
    }

    @Test
    public void testSerializeAndDeserializeVertexWithSetProp() {
        Vertex vertex = new Vertex("person");
        vertex.id("person:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);
        vertex.property("city", ImmutableSet.of("Hefei", "Wuhan", "Wuhan"));

        String json = serialize(vertex);
        Vertex vertexCopy = deserialize(json, Vertex.class);

        Assert.assertEquals("person:marko", vertexCopy.id());
        Assert.assertEquals("person", vertexCopy.label());
        Assert.assertEquals("vertex", vertexCopy.type());
        // TODO: Set properties should deserialize to Set instead of List
        Map<String, Object> props = ImmutableMap.of("name", "marko", "age", 29,
                                                    "city", ImmutableList.of("Hefei", "Wuhan"));
        Assert.assertEquals(props, vertexCopy.properties());
    }
}
