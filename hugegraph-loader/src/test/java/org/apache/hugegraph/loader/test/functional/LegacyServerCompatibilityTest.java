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
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

/** Run by loader-ci against its official Server 1.7 fixture. */
public class LegacyServerCompatibilityTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void testOrdinaryImportKeepsLegacyBehavior() throws Exception {
        this.assertImport(false);
    }

    @Test
    public void testRestoreKeepsLegacyBehavior() throws Exception {
        this.assertImport(true);
    }

    private void assertImport(boolean restore) throws Exception {
        URI endpoint = URI.create(System.getProperty("hugegraph.loader.test-server-url", "http://127.0.0.1:8080"));
        String graph = "legacy_owner_" + UUID.randomUUID().toString().replace("-", "");
        try (HugeClient control = HugeClient.builder(endpoint.toString(), "hugegraph")
                                           .configUser("admin", "pa").build()) {
            control.graphs().createGraph(graph, JsonUtil.toJson(ImmutableMap.of(
                    "store", graph, "backend", "memory", "serializer", "text",
                    "gremlin.graph", "org.apache.hugegraph.auth.HugeFactoryAuthProxy")));
            try (HugeClient client = HugeClient.builder(endpoint.toString(), graph).configUser("admin", "pa").build()) {
                client.schema().propertyKey("name").asText().create();
                client.schema().vertexLabel("person").properties("name").primaryKeys("name").create();
                ServerException missing = (ServerException) Assert.assertThrows(ServerException.class,
                        () -> client.graphs().newLoadingOwner(graph));
                Assert.assertEquals(404, missing.status());
                Path input = this.temporary.newFile("input.csv").toPath();
                Files.write(input, "name\nincoming\n".getBytes(StandardCharsets.UTF_8));
                Path mapping = this.temporary.newFile("struct.json").toPath();
                Files.write(mapping, JsonUtil.toJson(ImmutableMap.of("vertices", new Object[]{ImmutableMap.of(
                        "label", "person", "input", ImmutableMap.of("type", "file", "path", input.toString(),
                                                                  "format", "CSV"))}))
                                           .getBytes(StandardCharsets.UTF_8));
                HugeGraphLoader loader = new HugeGraphLoader(new String[]{
                        "-f", mapping.toString(), "-g", graph, "-h", endpoint.getHost(),
                        "-p", String.valueOf(endpoint.getPort()), "--username", "admin", "--password", "pa",
                        "--test-mode", "true", "--restore", String.valueOf(restore)
                });
                try {
                    Assert.assertTrue(loader.load());
                } finally {
                    loader.shutdown();
                }
                Assert.assertTrue(loader.context().closed());
                Assert.assertEquals(1, client.graph().listVertices().size());
                Assert.assertEquals("incoming", client.graph().listVertices().get(0).properties().get("name"));
                Assert.assertEquals(restore ? GraphMode.RESTORING : GraphMode.NONE, client.graphs().mode(graph));
            } finally {
                control.graphs().dropGraph(graph, "I'm sure to drop the graph");
            }
        }
    }
}
