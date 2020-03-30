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
import com.baidu.hugegraph.loader.test.functional.LoadTest;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.testutil.Assert;

public class LoadProgressTest extends LoadTest {

    @Test
    public void testTotalLoaded() {
        String json = "{\n"
                + "    \"1\":{\n"
                + "        \"type\":\"FILE\",\n"
                + "        \"loaded_items\":[\n"
                + "            {\n"
                + "                \"name\":\"vertex_person.csv\",\n"
                + "                \"last_modified\":1574346235000,\n"
                + "                \"checksum\":\"4250397517\",\n"
                + "                \"offset\":6\n"
                + "            }\n"
                + "        ],\n"
                + "        \"loading_item\":null\n"
                + "    },\n"
                + "    \"2\":{\n"
                + "        \"type\":\"FILE\",\n"
                + "        \"loaded_items\":[\n"
                + "            {\n"
                + "                \"name\":\"vertex_software.txt\",\n"
                + "                \"last_modified\":1575427304000,\n"
                + "                \"checksum\":\"2992253526\",\n"
                + "                \"offset\":2\n"
                + "            }\n"
                + "        ],\n"
                + "        \"loading_item\":null\n"
                + "    },\n"
                + "    \"3\":{\n"
                + "        \"type\":\"FILE\",\n"
                + "        \"loaded_items\":[\n"
                + "            {\n"
                + "                \"name\":\"edge_knows.json\",\n"
                + "                \"last_modified\":1576658150000,\n"
                + "                \"checksum\":\"3108779382\",\n"
                + "                \"offset\":2\n"
                + "            }\n"
                + "        ],\n"
                + "        \"loading_item\":null\n"
                + "    },\n"
                + "    \"4\":{\n"
                + "        \"type\":\"FILE\",\n"
                + "        \"loaded_items\":[\n"
                + "            {\n"
                + "                \"name\":\"edge_created.json\",\n"
                + "                \"last_modified\":1576659393000,\n"
                + "                \"checksum\":\"1026646359\",\n"
                + "                \"offset\":4\n"
                + "            }\n"
                + "        ],\n"
                + "        \"loading_item\":null\n"
                + "    }\n"
                + "}";
        LoadProgress progress = JsonUtil.fromJson(json, LoadProgress.class);
//        Assert.assertEquals(250, progress.totalLoaded(ElemType.VERTEX));
//        Assert.assertEquals(150, progress.totalLoaded(ElemType.EDGE));
    }
}
