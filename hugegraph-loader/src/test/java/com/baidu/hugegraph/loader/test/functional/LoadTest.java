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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;

public class LoadTest {

    protected static final String CONFIG_PATH_PREFIX = "target/test-classes";
    protected static final String GRAPH = "hugegraph";
    protected static final String SERVER = "127.0.0.1";
    protected static final int PORT = 8080;
    protected static final int HTTPS_PORT = 8443;
    protected static final String CONFIRM_CLEAR = "I'm sure to delete all data";
    protected static final String URL = String.format("http://%s:%s",
                                                      SERVER, PORT);
    protected static final String HTTPS_URL = String.format("https://%s:%s",
                                                            SERVER, HTTPS_PORT);
    protected static final String HTTPS_PROTOCOL = "https";
    protected static final String TRUST_STORE_FILE =
                                  "assembly/travis/conf/hugegraph.truststore";
    protected static final HugeClient CLIENT = HugeClient.builder(URL, GRAPH)
                                                         .build();

    public static String configPath(String fileName) {
        return Paths.get(CONFIG_PATH_PREFIX, fileName).toString();
    }

    public static void clearServerData() {
        CLIENT.graphs().clearGraph(GRAPH, CONFIRM_CLEAR);
    }

    public static void clearAndClose(HugeClient httpsClient, String graph) {
        if (httpsClient == null) {
            return;
        }
        httpsClient.graphs().clearGraph(graph, CONFIRM_CLEAR);
    }

    protected static void assertContains(List<Vertex> vertices, String label,
                                         Object... keyValues) {
        boolean matched = false;
        for (Vertex v : vertices) {
            if (v.label().equals(label) &&
                v.properties().equals(toMap(keyValues))) {
                matched = true;
                break;
            }
        }
        Assert.assertTrue(matched);
    }

    protected static void assertContains(List<Edge> edges, String label,
                                         Object sourceId, Object targetId,
                                         String sourceLabel, String targetLabel,
                                         Object... keyValues) {
        boolean matched = false;
        for (Edge e : edges) {
            if (e.label().equals(label) &&
                e.sourceId().equals(sourceId) &&
                e.targetId().equals(targetId) &&
                e.sourceLabel().equals(sourceLabel) &&
                e.targetLabel().equals(targetLabel) &&
                e.properties().equals(toMap(keyValues))) {
                matched = true;
                break;
            }
        }
        Assert.assertTrue(matched);
    }

    private static Map<String, Object> toMap(Object... properties) {
        Assert.assertTrue("The number of properties must be even",
                          (properties.length & 0x01) == 0);
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < properties.length; i = i + 2) {
            if (!properties[i].equals(T.ID) && !properties[i].equals(T.LABEL)) {
                map.put((String) properties[i], properties[i + 1]);
            }
        }
        return map;
    }

    public static void assertDateEquals(String expectDate, Object actualDate)
                                        throws java.text.ParseException {
        Assert.assertEquals("Date value must be String class",
                            String.class, actualDate.getClass());
        assertDateEquals(expectDate, TimeZone.getTimeZone("GMT+8"),
                         (String) actualDate, TimeZone.getDefault());
    }

    public static void assertDateEquals(List<String> expectDates,
                                        Object actualDates)
                                        throws java.text.ParseException {
        Assert.assertTrue("Date value must be List<String> class",
                          List.class.isAssignableFrom(actualDates.getClass()));
        List<String> actualDateList = (List<String>) actualDates;
        Assert.assertEquals("The size of expect and actual dates must be equal",
                            expectDates.size(), actualDateList.size());
        int n = expectDates.size();
        for (int i = 0; i < n; i++) {
            assertDateEquals(expectDates.get(i), TimeZone.getTimeZone("GMT+8"),
                             actualDateList.get(i), TimeZone.getDefault());
        }
    }

    public static void assertDateEquals(String expectDate, TimeZone expectZone,
                                        String actualDate, TimeZone actualZone)
                                        throws java.text.ParseException {
        DateFormat expectDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        expectDF.setTimeZone(expectZone);
        long expectTimeStamp = expectDF.parse(expectDate).getTime();

        DateFormat actualDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        actualDF.setTimeZone(actualZone);
        long actualTimeStamp = actualDF.parse(actualDate).getTime();

        Assert.assertEquals(expectTimeStamp, actualTimeStamp);
    }
}
