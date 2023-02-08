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

import java.util.Map;

import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class MetricsManagerTest extends BaseFuncTest {

    @Test
    public void testSystemMetrics() {
        Map<String, Map<String, Object>> results = metrics().system();
        Assert.assertEquals(ImmutableSet.of("basic", "heap", "nonheap",
                                            "thread", "class_loading",
                                            "garbage_collector"),
                            results.keySet());
    }

    @Test
    public void testBackendMetrics() {
        Map<String, Map<String, Object>> results = metrics().backend();
        Assert.assertEquals(ImmutableSet.of("hugegraph"), results.keySet());

        Map<String, Object> graphResults = metrics().backend("hugegraph");
        Assert.assertFalse(graphResults.isEmpty());
    }

    @Test
    public void testAllMetrics() {
        Map<String, Map<String, Object>> results = metrics().all();
        Assert.assertEquals(ImmutableSet.of("gauges", "counters", "histograms",
                                            "meters", "timers"),
                            results.keySet());
    }
}
