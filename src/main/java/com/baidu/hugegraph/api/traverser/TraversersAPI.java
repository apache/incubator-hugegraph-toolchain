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
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.api.traverser;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.util.E;

public class TraversersAPI extends API {

    private static final String PATH = "graphs/%s/traversers/%s";

    public TraversersAPI(RestClient client, String graph) {
        super(client);
        this.path(String.format(PATH, graph, type()));
    }

    @Override
    protected String type() {
        return "traversers";
    }

    protected static void checkPositive(int value, String name) {
        E.checkArgument(value > 0,
                        "%s must be > 0, but got '%s'", name, value);
    }

    protected static void checkDegree(long degree) {
        checkLimit(degree, "Degree");
    }

    protected static void checkCapacity(long capacity) {
        checkLimit(capacity, "Capacity");
    }

    protected static void checkLimit(long limit) {
        checkLimit(limit, "Limit");
    }
}
