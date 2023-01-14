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

package org.apache.hugegraph.functional;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class HugeClientTest {

    protected static final String BASE_URL = "http://127.0.0.1:8080";
    protected static final String GRAPH = "hugegraph";
    protected static final String USERNAME = "admin";
    protected static final String PASSWORD = "pa";

    @Test
    public void testContext() {
        HugeClient client = HugeClient.builder(BASE_URL, GRAPH)
                                      .configUser(USERNAME, PASSWORD)
                                      .build();

        String token = "Bearer token";
        client.setAuthContext(token);
        Assert.assertEquals(token, client.getAuthContext());

        client.resetAuthContext();
        Assert.assertNull(client.getAuthContext());
    }
}
