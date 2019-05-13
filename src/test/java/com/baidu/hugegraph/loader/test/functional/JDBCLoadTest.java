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

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;

/**
 * TODO: add more test cases
 */
public class JDBCLoadTest extends LoadTest {

    // JDBC driver name and database URL
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DATABASE = "load_test";
    private static final String DB_URL = "jdbc:mysql://localhost";
    // Database credentials
    private static final String USER = "root";
    private static final String PASS = "";

    private static DBUtil dbUtil = new DBUtil(DRIVER, DB_URL, USER, PASS);

    @BeforeClass
    public static void setUp() {
        clearServerData();

        dbUtil.connect();
        // create database
        dbUtil.execute(String.format("CREATE DATABASE IF NOT EXISTS `%s`;",
                                     DATABASE));
        // create tables
        dbUtil.connect(DATABASE);
        // vertex person
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `person` (\n"
                + "  `id` int(10) unsigned NOT NULL,\n"
                + "  `name` varchar(20) NOT NULL,\n"
                + "  `age` int(3) DEFAULT NULL,\n"
                + "  `city` varchar(10) DEFAULT NULL,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // vertex software
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `software` (\n"
                + "  `id` int(10) unsigned NOT NULL,\n"
                + "  `name` varchar(20) NOT NULL,\n"
                + "  `lang` varchar(10) NOT NULL,\n"
                + "  `price` double(10,2) NOT NULL,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // edge knows
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `knows` (\n"
                + "  `id` int(10) unsigned NOT NULL,\n"
                + "  `source_id` int(10) unsigned NOT NULL,\n"
                + "  `target_id` int(10) unsigned NOT NULL,\n"
                + "  `date` varchar(10) NOT NULL,\n"
                + "  `weight` double(10,2) NOT NULL,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // edge created
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `created` (\n"
                + "  `id` int(10) unsigned NOT NULL,\n"
                + "  `source_id` int(10) unsigned NOT NULL,\n"
                + "  `target_id` int(10) unsigned NOT NULL,\n"
                + "  `date` varchar(10) NOT NULL,\n"
                + "  `weight` double(10,2) NOT NULL,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
    }

    @AfterClass
    public static void tearDown() {
        // drop tables
        dbUtil.execute("DROP TABLE IF EXISTS `person`");
        dbUtil.execute("DROP TABLE IF EXISTS `software`");
        dbUtil.execute("DROP TABLE IF EXISTS `knows`");
        dbUtil.execute("DROP TABLE IF EXISTS `created`");
        // drop database
        dbUtil.execute(String.format("DROP DATABASE `%s`", DATABASE));

        dbUtil.close();
    }

    @Before
    public void init() {
    }

    @After
    public void clear() {
        clearServerData();

        dbUtil.execute("TRUNCATE TABLE `person`");
        dbUtil.execute("TRUNCATE TABLE `software`");
        dbUtil.execute("TRUNCATE TABLE `knows`");
        dbUtil.execute("TRUNCATE TABLE `created`");
    }

    @Test
    public void testCustomizedSchema() {
        dbUtil.insert("INSERT INTO `person` VALUES "
                + "(1,'marko',29,'Beijing'),"
                + "(2,'vadas',27,'HongKong'),"
                + "(3,'josh',32,'Beijing'),"
                + "(4,'peter',35,'Shanghai'),"
                + "(5,'li,nary',26,'Wu,han'),"
                + "(6,'tom',NULL,NULL);");
        dbUtil.insert("INSERT INTO `software` VALUES "
                + "(100,'lop','java',328.00),"
                + "(200,'ripple','java',199.00);");

        dbUtil.insert("INSERT INTO `knows` VALUES "
                + "(1,1,2,'2016-01-10',0.50),"
                + "(2,1,3,'2013-02-20',1.00);");
        dbUtil.insert("INSERT INTO `created` VALUES "
                + "(1,1,100,'2017-12-10',0.40),"
                + "(2,3,100,'2009-11-11',0.40),"
                + "(3,3,200,'2017-12-10',1.00),"
                + "(4,4,100,'2017-03-24',0.20);");

        String[] args = new String[]{
                "-f", configPath("jdbc_customized_schema/struct.json"),
                "-s", configPath("jdbc_customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

        Assert.assertEquals(8, vertices.size());
        Assert.assertEquals(6, edges.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(Integer.class, vertex.id().getClass());
        }
        for (Edge edge : edges) {
            Assert.assertEquals(Integer.class, edge.sourceId().getClass());
            Assert.assertEquals(Integer.class, edge.targetId().getClass());
        }
    }

    @Test
    public void testValueMappingInJDBCSource() {
        dbUtil.insert("INSERT INTO `person` VALUES " +
                      "(1,'marko',29,'1')," +
                      "(2,'vadas',27,'2');");

        String[] args = new String[]{
                "-f", configPath("value_mapping_in_jdbc_source/struct.json"),
                "-s", configPath("value_mapping_in_jdbc_source/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas",
                       "age", 27, "city", "Shanghai");
    }
}
