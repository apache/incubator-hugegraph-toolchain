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

package org.apache.hugegraph.loader.spark;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.util.JsonUtil;

public final class SparkLoaderArtifactFixture {

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !Set.of("prepare", "verify-vertices", "verify-all", "cleanup").contains(args[0])) {
            throw new IllegalArgumentException("Expected fixture mode and work directory");
        }
        String mode = args[0];
        Path dir = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(dir);
        String prefix = mode.equals("prepare") ? "spark_java17_" + UUID.randomUUID().toString().substring(0, 8)
                                              : Files.readString(dir.resolve("prefix.txt")).trim();
        if (!prefix.matches("spark_java17_[0-9a-f]{8}")) {
            throw new IllegalArgumentException("Unexpected fixture prefix");
        }
        String name = prefix + "_name";
        String age = prefix + "_age";
        String person = prefix + "_person";
        String knows = prefix + "_knows";
        String first = prefix + "_a";
        String second = prefix + "_\u5317\u4eac";
        var builder = HugeClient.builder(required("HUGEGRAPH_TEST_URL"), required("HUGEGRAPH_TEST_GRAPH"));
        if (!"anonymous".equals(System.getenv("HUGEGRAPH_TEST_AUTH_MODE"))) {
            builder.configUser(required("HUGEGRAPH_TEST_USERNAME"), required("HUGEGRAPH_TEST_PASSWORD"));
        }
        try (HugeClient client = builder.build()) {
            if (mode.equals("prepare")) {
                Files.writeString(dir.resolve("prefix.txt"), prefix, StandardCharsets.UTF_8);
                client.schema().propertyKey(name).asText().create();
                client.schema().propertyKey(age).asInt().create();
                client.schema().vertexLabel(person).properties(name, age).useCustomizeStringId().create();
                client.schema().edgeLabel(knows).sourceLabel(person).targetLabel(person).create();
                Path vertices = dir.resolve("vertices.json");
                Path edges = dir.resolve("edges.json");
                Files.writeString(vertices, JsonUtil.toJson(Map.of(name, first, age, 21)) + "\n" +
                                            JsonUtil.toJson(Map.of(name, second, age, 32)) + "\n",
                                  StandardCharsets.UTF_8);
                Files.writeString(edges, JsonUtil.toJson(Map.of("source_name", first, "target_name", second)) + "\n",
                                  StandardCharsets.UTF_8);
                write(dir.resolve("vertex-struct.json"), Map.of("vertices", List.of(Map.of(
                      "label", person, "id", name, "input", input(vertices, List.of(name, age))))));
                write(dir.resolve("edge-struct.json"), Map.of("edges", List.of(Map.of(
                      "label", knows, "source", List.of("source_name"), "target", List.of("target_name"),
                      "input", input(edges, List.of("source_name", "target_name")),
                      "field_mapping", Map.of("source_name", name, "target_name", name)))));
            } else if (mode.startsWith("verify")) {
                if (client.graph().listVertices(person).size() != 2 ||
                    ((Number) client.graph().getVertex(first).property(age)).intValue() != 21 ||
                    ((Number) client.graph().getVertex(second).property(age)).intValue() != 32) {
                    throw new AssertionError("Vertex readback mismatch");
                }
                if (mode.equals("verify-all")) {
                    var edges = client.graph().listEdges(knows);
                    if (edges.size() != 1 || !first.equals(edges.get(0).sourceId()) ||
                        !second.equals(edges.get(0).targetId())) {
                        throw new AssertionError("Edge endpoint readback mismatch");
                    }
                }
            } else {
                for (var label : client.schema().getEdgeLabels()) {
                    if (label.name().equals(knows)) {
                        client.schema().removeEdgeLabel(knows);
                    }
                }
                for (var label : client.schema().getVertexLabels()) {
                    if (label.name().equals(person)) {
                        client.schema().removeVertexLabel(person);
                    }
                }
                for (var key : client.schema().getPropertyKeys()) {
                    if (key.name().equals(name) || key.name().equals(age)) {
                        client.schema().removePropertyKey(key.name());
                    }
                }
                if (client.schema().getEdgeLabels().stream().anyMatch(x -> x.name().startsWith(prefix)) ||
                    client.schema().getVertexLabels().stream().anyMatch(x -> x.name().startsWith(prefix)) ||
                    client.schema().getPropertyKeys().stream().anyMatch(x -> x.name().startsWith(prefix))) {
                    throw new AssertionError("Fixture schema cleanup incomplete");
                }
            }
            write(dir.resolve(mode + ".json"), Map.of("mode", mode, "prefix", prefix, "passed", true));
        }
    }

    private static Map<String, Object> input(Path path, List<String> header) {
        return Map.of("type", "file", "format", "JSON", "path", path.toString(),
                      "header", header, "charset", "UTF-8");
    }

    private static void write(Path path, Object value) throws Exception {
        Files.writeString(path, JsonUtil.toJson(value), StandardCharsets.UTF_8);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }
}
