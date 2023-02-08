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

package org.apache.hugegraph.loader.test.unit;

import org.apache.hugegraph.loader.util.DateUtil;
import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;

public class DateUtilTest {

    @Test
    public void testCheckTimeZone() {
        Assert.assertTrue(DateUtil.checkTimeZone("JST"));
        Assert.assertTrue(DateUtil.checkTimeZone("UTC"));
        Assert.assertTrue(DateUtil.checkTimeZone("GMT"));
        // GMT+00:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT+0"));
        // GMT-00:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT-0"));
        // GMT+09:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT+9:00"));
        // GMT+10:30
        Assert.assertTrue(DateUtil.checkTimeZone("GMT+10:30"));
        // GMT-04:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT-0400"));
        // GMT+08:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT+8"));
        // GMT-13:00
        Assert.assertTrue(DateUtil.checkTimeZone("GMT-13"));
        // GMT-13:59
        Assert.assertTrue(DateUtil.checkTimeZone("GMT+13:59"));
        // NOTE: valid time zone IDs (see TimeZone.getAvailableIDs())
        // GMT-08:00
        Assert.assertTrue(DateUtil.checkTimeZone("America/Los_Angeles"));
        // GMT+09:00
        Assert.assertTrue(DateUtil.checkTimeZone("Japan"));
        // GMT+01:00
        Assert.assertTrue(DateUtil.checkTimeZone("Europe/Berlin"));
        // GMT+04:00
        Assert.assertTrue(DateUtil.checkTimeZone("Europe/Moscow"));
        // GMT+08:00
        Assert.assertTrue(DateUtil.checkTimeZone("Asia/Singapore"));

        Assert.assertFalse(DateUtil.checkTimeZone("JPY"));
        Assert.assertFalse(DateUtil.checkTimeZone("USD"));
        Assert.assertFalse(DateUtil.checkTimeZone("UTC+8"));
        Assert.assertFalse(DateUtil.checkTimeZone("UTC+09:00"));
        Assert.assertFalse(DateUtil.checkTimeZone("+09:00"));
        Assert.assertFalse(DateUtil.checkTimeZone("-08:00"));
        Assert.assertFalse(DateUtil.checkTimeZone("-1"));
        Assert.assertFalse(DateUtil.checkTimeZone("GMT+10:-30"));
        // hours 0-23 only
        Assert.assertFalse(DateUtil.checkTimeZone("GMT+24:00"));
        // minutes 00-59 only
        Assert.assertFalse(DateUtil.checkTimeZone("GMT+13:60"));
    }
}
