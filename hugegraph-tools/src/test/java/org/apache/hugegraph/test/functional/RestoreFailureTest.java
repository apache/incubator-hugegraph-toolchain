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

package org.apache.hugegraph.test.functional;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.cmd.SubCommands;
import org.apache.hugegraph.exception.ToolsException;
import org.apache.hugegraph.manager.RestoreManager;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.structure.constant.HugeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

public class RestoreFailureTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private HttpServer server;

    @Before
    public void setup() throws Exception {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            boolean schema = path.endsWith("/schema/vertexlabels");
            boolean versions = path.endsWith("/versions");
            String response = versions ? "{\"versions\":{\"core\":\"1.8.0\",\"api\":\"0.71\"}}" :
                              schema ? "{\"vertexlabels\":[]}" :
                              "{\"exception\":\"IllegalArgumentException\",\"message\":\"rejected batch\"}";
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(schema || versions ? 200 : 400, body.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        this.server.start();
    }

    @After
    public void teardown() {
        this.server.stop(0);
    }

    @Test
    public void testCorruptVertexJsonFailsRestore() throws Exception {
        this.assertRestoreFails(HugeType.VERTEX, "vertices-0", "{\"vertices\":[",
                                SerializeException.class);
    }

    @Test
    public void testCorruptEdgeJsonFailsRestore() throws Exception {
        this.assertRestoreFails(HugeType.EDGE, "edges-0", "{\"edges\":[",
                                SerializeException.class);
    }

    @Test
    public void testCorruptZipFailsRestore() throws Exception {
        this.assertRestoreFails(HugeType.VERTEX, "vertices-0.zip", "not a zip archive",
                                ClientException.class);
    }

    @Test
    public void testRejectedVertexBatchFailsRestore() throws Exception {
        this.assertRestoreFails(HugeType.VERTEX, "vertices-0",
                                "{\"vertices\":[{\"id\":1,\"label\":\"person\",\"properties\":{}}]}",
                                ToolsException.class);
    }

    @Test
    public void testNoDataFilesIsValidEmptyRestore() throws Exception {
        File backup = this.temporary.newFolder();
        RestoreManager manager = this.manager(backup);
        try {
            manager.restore(Arrays.asList(HugeType.VERTEX, HugeType.EDGE));
            Assert.assertFalse(backup.exists());
        } finally {
            manager.close();
        }
    }

    private void assertRestoreFails(HugeType type, String filename, String content,
                                    Class<? extends Throwable> cause) throws Exception {
        File backup = this.temporary.newFolder();
        File file = new File(backup, filename);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        RestoreManager manager = this.manager(backup);
        try {
            manager.restore(Collections.singletonList(type));
            Assert.fail("Invalid backup or rejected batch must fail restore");
        } catch (ToolsException e) {
            Assert.assertTrue(e.toString(), cause.isInstance(e.getCause()));
            Assert.assertTrue(file.exists());
        } finally {
            manager.close();
        }
    }

    private RestoreManager manager(File backup) throws Exception {
        String url = "http://127.0.0.1:" + this.server.getAddress().getPort();
        ToolClient.ConnectionInfo info = new ToolClient.ConnectionInfo(
                url, "hugegraph", null, null, 1000, null, null);
        RestoreManager manager = new RestoreManager(info);
        SubCommands.Restore command = new SubCommands.Restore();
        command.directory(backup.getAbsolutePath());
        command.logDir(this.temporary.newFolder().getAbsolutePath());
        command.threadsNum = 2;
        command.retry(1);
        command.clean(true);
        manager.init(command);
        manager.mode(GraphMode.RESTORING);
        return manager;
    }
}
