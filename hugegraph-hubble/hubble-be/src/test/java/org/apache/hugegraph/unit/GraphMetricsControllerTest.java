/*
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

package org.apache.hugegraph.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.apache.hugegraph.controller.saas.GraphMetricsController;
import org.apache.hugegraph.controller.saas.GraphMetricsController.SchemaCount;
import org.apache.hugegraph.testutil.Assert;

public class GraphMetricsControllerTest {

    @Test
    public void testWeeklyGrowthRateAcceptsSparseTypeCounts() {
        List<String> dateRange = Arrays.asList(
                "20260701", "20260702", "20260703", "20260704",
                "20260705", "20260706", "20260707", "20260708",
                "20260709", "20260710", "20260711", "20260712",
                "20260713", "20260714"
        );
        SchemaCount<Integer> today = new SchemaCount<>(1, 2, 3, 4);

        SchemaCount<Double> result = GraphMetricsController.weeklyGrowthRate(
                Collections.emptyMap(), dateRange, today);

        Assert.assertEquals(1.0, result.pk);
        Assert.assertEquals(1.0, result.vl);
        Assert.assertEquals(1.0, result.el);
        Assert.assertEquals(1.0, result.il);
    }
}
