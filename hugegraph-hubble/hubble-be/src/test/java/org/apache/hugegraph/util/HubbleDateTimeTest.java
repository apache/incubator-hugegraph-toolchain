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
package org.apache.hugegraph.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class HubbleDateTimeTest {

    @Test
    public void testInitializationDoesNotChangeJvmTimeZone() throws Exception {
        TimeZone original = TimeZone.getDefault();
        URL classes = HubbleUtil.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes},
                                                       ClassLoader.getPlatformClassLoader())) {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            // Initialize a fresh copy even when another test already used HubbleUtil.
            Class.forName(HubbleUtil.class.getName(), true, loader);
            Assert.assertEquals("UTC", TimeZone.getDefault().getID());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void testBusinessDatesDoNotDependOnJvmTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            Date date = date("2024-01-31T20:00:00Z");
            Assert.assertEquals("20240201", HubbleUtil.dateFormatDay(date));
            Assert.assertEquals("2024-02-01", HubbleUtil.dateFormatDay("20240201"));
            Assert.assertEquals("America/Los_Angeles", TimeZone.getDefault().getID());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void testElasticsearchTimestampUsesBusinessTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Assert.assertEquals("2024-02-01 04:00:00", ESUtil.parseTimestamp("2024-01-31T20:00:00Z"));
            Assert.assertEquals("2024-02-01 04:00:00", ESUtil.parseTimestamp("2024-02-01T05:00:00+09:00"));
            Assert.assertNull(ESUtil.parseTimestamp(null));
            Assert.assertNull(ESUtil.parseTimestamp(""));
            Assert.assertEquals("UTC", TimeZone.getDefault().getID());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void testPreviousDayUsesBusinessMidnight() {
        Assert.assertEquals("20231231", HubbleUtil.dateFormatLastDay(clock("2023-12-31T16:00:00Z")));
        Assert.assertEquals("20240229", HubbleUtil.dateFormatLastDay(clock("2024-02-29T16:00:00Z")));
    }

    @Test
    public void testInvalidDatesAreNotSilentlyNormalized() {
        for (String invalid : new String[]{"20230229", "20240230", "20240101extra", "20241301"}) {
            Assert.assertThrows(DateTimeParseException.class, () -> HubbleUtil.dateFormatDay(invalid));
        }
        Assert.assertThrows(DateTimeParseException.class, () -> {
            HubbleUtil.getTimestampsBefore24Hours("20240230 12:00:00");
        });
        Assert.assertThrows(DateTimeParseException.class, () -> {
            HubbleUtil.getTimestampsBefore24Hours("20240229 24:00:00");
        });
        Assert.assertArrayEquals(seconds("2024-02-28T04:00:00Z", "2024-02-29T04:00:00Z"),
                                 HubbleUtil.getTimestampsBefore24Hours("20240229 12:00:00"));
    }

    private static Date date(String value) {
        return Date.from(Instant.parse(value));
    }

    private static Clock clock(String value) {
        return Clock.fixed(Instant.parse(value), ZoneOffset.UTC);
    }

    private static long[] seconds(String start, String end) {
        return new long[]{Instant.parse(start).getEpochSecond(), Instant.parse(end).getEpochSecond()};
    }
}
