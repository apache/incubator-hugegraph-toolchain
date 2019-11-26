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

package com.baidu.hugegraph.loader.test.functional;

import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;

public class HDFSLoadTest extends FileLoadTest {

    @Override
    public String structPath(String fileName) {
        if (fileName.contains("struct")) {
            int idx = fileName.indexOf("/");
            String preifx = fileName.substring(0, idx);
            String suffix = fileName.substring(idx + 1);
            suffix = StringUtils.replace(suffix, "struct", "struct_hdfs");
            fileName = preifx + "/" + suffix;
        }
        return Paths.get(CONFIG_PATH_PREFIX, fileName).toString();
    }

    @Before
    public void init() {
        this.ioUtil = new HDFSUtil("hdfs://localhost:8020/files");
    }

    @Test
    public void testHDFSWithCoreSitePath() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", structPath("hdfs_with_core_site_path/struct.json"),
                "-s", configPath("hdfs_with_core_site_path/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }

    @Test
    public void testHDFSWithCoreSitePathEmpty() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", structPath("hdfs_with_empty_core_site_path/struct.json"),
                "-s", configPath("hdfs_with_empty_core_site_path/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testHDFSWithInvalidCoreSitePath() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", structPath("hdfs_with_invalid_core_site_path/struct.json"),
                "-s", configPath("hdfs_with_invalid_core_site_path/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        }, e -> {
            String message = "An exception occurred while checking HDFS path";
            Assert.assertTrue(e.getMessage().contains(message));
        });
    }
}
