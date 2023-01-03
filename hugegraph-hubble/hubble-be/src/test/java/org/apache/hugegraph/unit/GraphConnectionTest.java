/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hugegraph.unit;

import org.apache.hugegraph.HugeGraphHubble;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
@SpringBootTest(classes = HugeGraphHubble.class, webEnvironment =
        SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphConnectionTest {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void testGraphConnect() {
        GraphConnection entry =
                GraphConnection.builder().host(HOST).port(PORT).name("test").graph(
                        "hugegraph").build();
        Response response = testRestTemplate.postForObject(
                Constant.API_VERSION + "graph-connections",
                entry, Response.class);
        Assert.assertEquals(response.getMessage(), 200, response.getStatus());
    }
}
