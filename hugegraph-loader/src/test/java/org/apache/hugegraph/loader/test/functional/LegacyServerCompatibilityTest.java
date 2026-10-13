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

package org.apache.hugegraph.loader.test.functional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

/** Runs explicitly against the official Server 1.7 fixture, outside the import profiles. */
public class LegacyServerCompatibilityTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void testMissingOwnershipRefusesImportBeforeMutation() throws Exception {
        String url = System.getProperty("hugegraph.loader.test-server-url", "http://127.0.0.1:8080");
        String graph = "legacy_owner_" + UUID.randomUUID().toString().replace("-", "");
        try (HugeClient control = HugeClient.builder(url, "hugegraph").configUser("admin", "pa").build()) {
            control.graphs().createGraph(graph, JsonUtil.toJson(ImmutableMap.of(
                    "store", graph, "backend", "memory", "serializer", "text")));
            try (HugeClient client = HugeClient.builder(url, graph).configUser("admin", "pa").build()) {
                client.schema().propertyKey("name").asText().create();
                client.schema().vertexLabel("sentinel").properties("name").primaryKeys("name").create();
                Vertex sentinel = client.graph().addVertex(T.LABEL, "sentinel", "name", "kept");
                Assert.assertEquals(GraphMode.NONE, client.graphs().mode(graph));

                ServerException capability = (ServerException) Assert.assertThrows(ServerException.class,
                        () -> client.graphs().newLoadingOwner(graph));
                Assert.assertEquals(404, capability.status());

                Path input = this.temporary.newFile("input.csv").toPath();
                Files.write(input, "name\nincoming\n".getBytes(StandardCharsets.UTF_8));
                Path mapping = this.temporary.newFile("struct.json").toPath();
                String struct = JsonUtil.toJson(ImmutableMap.of("vertices", new Object[]{ImmutableMap.of(
                        "label", "sentinel", "input", ImmutableMap.of(
                                "type", "file", "path", input.toString(), "format", "CSV"))}));
                Files.write(mapping, struct.getBytes(StandardCharsets.UTF_8));
                Path schema = this.temporary.newFile("schema.groovy").toPath();
                Files.write(schema, "schema.propertyKey('blocked_import_key').asText().create();"
                                    .getBytes(StandardCharsets.UTF_8));
                URI endpoint = URI.create(url);
                HugeGraphLoader loader = new HugeGraphLoader(new String[]{
                        "-f", mapping.toString(), "-s", schema.toString(), "-g", graph,
                        "-h", endpoint.getHost(), "-p", String.valueOf(endpoint.getPort()),
                        "--username", "admin", "--password", "pa", "--clear-all-data", "true",
                        "--test-mode", "true"
                });
                try {
                    ServerException failure = (ServerException) Assert.assertThrows(ServerException.class,
                            loader::load);
                    Assert.assertEquals(404, failure.status());
                } finally {
                    loader.shutdown();
                }

                Assert.assertEquals(GraphMode.NONE, client.graphs().mode(graph));
                Assert.assertEquals(1, client.schema().getPropertyKeys().size());
                Assert.assertEquals("name", client.schema().getPropertyKeys().get(0).name());
                Assert.assertEquals(1, client.schema().getVertexLabels().size());
                Assert.assertEquals("sentinel", client.schema().getVertexLabels().get(0).name());
                Assert.assertTrue(client.schema().getEdgeLabels().isEmpty());
                Assert.assertTrue(client.schema().getIndexLabels().isEmpty());
                Assert.assertEquals(1, client.graph().listVertices().size());
                Assert.assertEquals(sentinel.id(), client.graph().getVertex(sentinel.id()).id());
                Assert.assertEquals("kept", client.graph().getVertex(sentinel.id()).properties().get("name"));
                Assert.assertTrue(client.graph().listEdges().isEmpty());
            } finally {
                control.graphs().dropGraph(graph, "I'm sure to drop the graph");
            }
        }
    }
}
