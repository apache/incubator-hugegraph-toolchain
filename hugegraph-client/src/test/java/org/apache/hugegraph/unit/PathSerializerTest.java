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

import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PathSerializerTest extends BaseUnitTest {

    @Test
    public void testSerializeAndDeserializePathWithVertexAndEdge() {
        Vertex vertex = new Vertex("person");
        vertex.id("person:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);
        vertex.property("city", "Beijing");

        Edge edge = new Edge("knows");
        edge.id("person:marko>knows>>person:vadas");
        edge.sourceId("person:marko");
        edge.sourceLabel("person");
        edge.targetId("person:vadas");
        edge.targetLabel("person");
        edge.property("date", "2016-01-10");
        edge.property("weight", 0.5);

        Path path = new Path();
        path.labels(ImmutableList.of());
        path.labels(ImmutableList.of());
        path.objects(vertex);
        path.objects(edge);

        String json = serialize(path);
        Path pathCopy = deserialize(json, Path.class);

        Assert.assertEquals(2, pathCopy.objects().size());
        Utils.assertGraphEqual(ImmutableList.of(vertex),
                               ImmutableList.of(edge),
                               path.objects());
    }

    @Test
    public void testDeserializePathWithSimpleType() {
        String json = "{"
                      + "\"labels\":["
                      + "[],"
                      + "[]"
                      + "],"
                      + "\"objects\":["
                      + "\"marko\","
                      + "\"lop\""
                      + "]"
                      + "}";

        Path path = deserialize(json, Path.class);

        Assert.assertEquals(2, path.labels().size());
        Assert.assertEquals(ImmutableList.of(), path.labels().get(0));
        Assert.assertEquals(ImmutableList.of(), path.labels().get(1));

        Assert.assertEquals(2, path.objects().size());
        Assert.assertArrayEquals(new Object[]{"marko", "lop"},
                                 path.objects().toArray());

        json = "{"
               + "\"labels\":["
               + "[],"
               + "[]"
               + "],"
               + "\"objects\":["
               + "29,"
               + "32"
               + "]"
               + "}";

        path = deserialize(json, Path.class);

        Assert.assertEquals(2, path.objects().size());
        Assert.assertArrayEquals(new Object[]{29, 32},
                                 path.objects().toArray());
    }

    @Test
    public void testDeserializePathWithListType() {
        String json = "{"
                      + "\"labels\":["
                      + "[],"
                      + "[]"
                      + "],"
                      + "\"objects\":["
                      + "[\"Beijing\", \"Beijing\"],"
                      + "[\"Wuhan\", \"Hongkong\"]"
                      + "]"
                      + "}";

        Path path = BaseUnitTest.deserialize(json, Path.class);

        Assert.assertEquals(2, path.labels().size());
        Assert.assertEquals(ImmutableList.of(), path.labels().get(0));
        Assert.assertEquals(ImmutableList.of(), path.labels().get(1));

        Assert.assertEquals(2, path.objects().size());
        Assert.assertArrayEquals(new Object[]{ImmutableList.of("Beijing", "Beijing"),
                                              ImmutableList.of("Wuhan", "Hongkong")},
                                 path.objects().toArray());
    }
}
