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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtilTest {

    @Test
    public void testToJson() {
        Cat cat = new Cat();
        cat.setName("alice");
        cat.setAge(7);
        Assert.assertEquals("{\"name\":\"alice\",\"age\":7}", JsonUtil.toJson(cat));
    }

    @Test
    public void testFromJson() {
        Cat cat = JsonUtil.fromJson("{\"name\":\"alice\",\"age\":7}", Cat.class);
        Assert.assertEquals("alice", cat.getName());
        Assert.assertEquals(Integer.valueOf(7), cat.getAge());
    }

    @Test
    public void testConvertValue() throws IOException {
        Cat cat = JsonUtil.convertValue(new ObjectMapper().readTree(
                "{\"name\":\"alice\",\"age\":7}"), Cat.class);
        Assert.assertEquals("alice", cat.getName());
        Assert.assertEquals(Integer.valueOf(7), cat.getAge());
    }

    @Test
    public void testFromJson2ListWithPojo() {
        List<Cat> cats = JsonUtil.fromJson2List("[{\"name\":\"alice\",\"age\":7}]", Cat.class);
        Assert.assertEquals(1, cats.size());
        Assert.assertEquals(Cat.class, cats.get(0).getClass());
        Assert.assertEquals("alice", cats.get(0).getName());
        Assert.assertEquals(Integer.valueOf(7), cats.get(0).getAge());
        Cat expected = new Cat();
        expected.setName("alice");
        expected.setAge(7);
        Assert.assertEquals(expected, cats.get(0));
        Assert.assertEquals(expected.hashCode(), cats.get(0).hashCode());
    }

    @Test
    public void testFromJson2ListWithEmptyAndNull() {
        Assert.assertEquals(Collections.emptyList(), JsonUtil.fromJson2List("[]", Cat.class));
        Assert.assertNull(JsonUtil.fromJson2List("null", Cat.class));
        Assert.assertEquals(Collections.singletonList(null),
                            JsonUtil.fromJson2List("[null]", Cat.class));
        try {
            JsonUtil.fromJson2List(null, Cat.class);
            Assert.fail("Expected IllegalArgumentException for null input");
        } catch (IllegalArgumentException expected) {
            // Match ObjectMapper and fromJson null-input behavior.
        }
    }

    @Test
    public void testFromJson2ListWithInvalidInput() {
        String[] invalid = {"", "   ", "[{", "{\"name\":\"alice\",\"age\":7}",
                            "\"alice\"", "7", "true", "[\"wrong\"]",
                            "[{\"name\":\"alice\",\"age\":\"not-an-int\"}]"};
        for (String json : invalid) {
            try {
                JsonUtil.fromJson2List(json, Cat.class);
                Assert.fail("Expected SerializeException for: " + json);
            } catch (SerializeException exception) {
                Assert.assertTrue(exception.getCause() instanceof IOException);
                Assert.assertTrue(exception.getMessage().contains(json));
            }
        }
    }

    @Test
    public void testVertexRoundTrip() {
        Vertex vertex = new Vertex("person");
        vertex.id("1:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);

        Vertex copy = JsonUtil.fromJson(JsonUtil.toJson(vertex), Vertex.class);

        Assert.assertEquals("1:marko", copy.id());
        Assert.assertEquals("person", copy.label());
        Assert.assertEquals("marko", copy.property("name"));
        Assert.assertEquals(29, copy.property("age"));
    }

    @Test
    public void testFromJsonWithMalformedJson() {
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.fromJson("{\"id\": ", Map.class);
        }, e -> {
            Assert.assertContains("Failed to deserialize json", e.getMessage());
        });
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.fromJson("", Map.class);
        });
    }

    @Test
    public void testFromJsonWithMismatchedType() {
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.fromJson("[1, 2]", Vertex.class);
        });
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.fromJson("\"text\"", Integer.class);
        });
    }

    @Test
    public void testToJsonWithUnserializableObject() {
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.toJson(new Object());
        }, e -> {
            Assert.assertContains("Failed to serialize object", e.getMessage());
        });
    }

    @Test
    public void testConvertValueWithVertexAndMismatchedType() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("label", "person");
        node.put("type", "vertex");

        Vertex vertex = JsonUtil.convertValue(node, Vertex.class);
        Assert.assertEquals("person", vertex.label());

        JsonNode text = JsonNodeFactory.instance.textNode("not a number");
        Assert.assertThrows(SerializeException.class, () -> {
            JsonUtil.convertValue(text, Integer.class);
        }, e -> {
            Assert.assertContains("Failed to deserialize json node", e.getMessage());
        });
    }

    public static class Cat {

        private String name;
        private Integer age;

        public Cat() {
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return this.age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Cat)) {
                return false;
            }
            Cat other = (Cat) object;
            return Objects.equals(this.name, other.name) && Objects.equals(this.age, other.age);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.age);
        }
    }
}
