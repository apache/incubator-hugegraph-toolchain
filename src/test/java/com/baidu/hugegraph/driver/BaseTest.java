/*
 * Copyright 2017 HugeGraph Authors
 *
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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.driver;

import org.junit.After;
import org.junit.Before;


public class BaseTest {

    public static String BASE_URL = "http://127.0.0.1:8080";
    public static String GRAPH = "hugegraph";

    private HugeClient client;

    @Before
    public void init() {
        this.client = HugeClient.open(BASE_URL, GRAPH);
    }

    @After
    public void clear() {
        //        this.client.close();
    }

    public HugeClient client() {
        return this.client;
    }

    public static HugeClient newClient() {
        return new HugeClient(BASE_URL, GRAPH);
    }

//    public GraphManager graph() {
//        return client.graph();
//    }
}
