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

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import org.apache.hugegraph.api.graph.GraphMetricsAPI.TypeCount;
import org.apache.hugegraph.controller.saas.GraphMetricsController;
import org.apache.hugegraph.controller.saas.GraphMetricsController.SchemaCount;
import org.apache.hugegraph.testutil.Assert;

public class GraphMetricsControllerTest {

    @Test
    public void testGrowthRatesAreIndependentOfLocaleAndAcceptPartialSchemas() {
        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            List<String> dates = GraphMetricsController.getRangeDate("20260714", -13);
            Map<String, TypeCount> counts = new HashMap<>();
            counts.put(dates.get(0), TypeCount.builder().schemas(
                    Map.of("pk_count", 8L, "vl_count", 4L, "el_count", 3L)).build());
            counts.put(dates.get(1), new TypeCount());
            counts.put(dates.get(7), TypeCount.builder().schemas(Map.of("pk_count", 2L)).build());
            SchemaCount<Double> result = GraphMetricsController.weeklyGrowthRate(
                    counts, dates, new SchemaCount<>(8, 3, 2, 1));

            Assert.assertEquals(0.25, result.pk, 0.0);
            Assert.assertEquals(-0.25, result.vl, 0.0);
            Assert.assertEquals(-0.33, result.el, 0.0);
            Assert.assertEquals(1.0, result.il, 0.0);
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    public void testDateRangesCrossLeapDayAndYearBoundary() {
        Assert.assertEquals(List.of("20240228", "20240229", "20240301"),
                            GraphMetricsController.getRangeDate("20240301", -2));
        Assert.assertEquals(List.of("20251231", "20260101", "20260102"),
                            GraphMetricsController.getRangeDate("20251231", 2));
        Assert.assertEquals(List.of("20260101"),
                            GraphMetricsController.getRangeDate("20260101", 0));
    }

    @Test(expected = DateTimeParseException.class)
    public void testInvalidDateIsRejected() {
        GraphMetricsController.getRangeDate("20260229", -1);
    }

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
