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

package com.baidu.hugegraph.loader.test.unit;

import org.junit.Test;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.testutil.Assert;

public class LoadProgressTest {

    @Test
    public void testTotalLoaded() {
        String json = "{\n"
                + "    \"vertex\":{\n"
                + "        \"user-e6d5731c\":{\n"
                + "            \"type\":\"FILE\",\n"
                + "            \"loaded_items\":[\n"
                + "                {\n"
                + "                    \"name\":\"users.dat\",\n"
                + "                    \"last_modified\":1046418788000,\n"
                + "                    \"checksum\":\"3405764271\",\n"
                + "                    \"offset\":100\n"
                + "                }\n"
                + "            ],\n"
                + "            \"loading_item\":null\n"
                + "        },\n"
                + "        \"movie-a917bbd1\":{\n"
                + "            \"type\":\"FILE\",\n"
                + "            \"loaded_items\":[\n"
                + "                {\n"
                + "                    \"name\":\"movies1.dat\",\n"
                + "                    \"last_modified\":1048663094000,\n"
                + "                    \"checksum\":\"2069887608\",\n"
                + "                    \"offset\":100\n"
                + "                }\n"
                + "            ],\n"
                + "            \"loading_item\":{\n"
                + "                \"name\":\"movies2.dat\",\n"
                + "                \"last_modified\":1049418788000,\n"
                + "                \"checksum\":\"3201759291\",\n"
                + "                \"offset\":50\n"
                + "            }\n"
                + "        }\n"
                + "    },\n"
                + "    \"edge\":{\n"
                + "        \"rating-6ec4d883\":{\n"
                + "            \"type\":\"FILE\",\n"
                + "            \"loaded_items\":[\n"
                + "                {\n"
                + "                    \"name\":\"ratings1.dat\",\n"
                + "                    \"last_modified\":1046418788000,\n"
                + "                    \"checksum\":\"4178689869\",\n"
                + "                    \"offset\":100\n"
                + "                }\n"
                + "            ],\n"
                + "            \"loading_item\":{\n"
                + "                \"name\":\"ratings2.dat\",\n"
                + "                \"last_modified\":1049418788000,\n"
                + "                \"checksum\":\"9642307424\",\n"
                + "                \"offset\":50\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";
        LoadProgress progress = JsonUtil.fromJson(json, LoadProgress.class);
        Assert.assertEquals(250, progress.totalLoaded(ElemType.VERTEX));
        Assert.assertEquals(150, progress.totalLoaded(ElemType.EDGE));
    }
}
