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

package org.apache.hugegraph.unit;

import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.CommonUtil;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class CommonUtilTest {

    @Test
    public void testCheckMapClass() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkMapClass(null, Integer.class, String.class);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkMapClass("Not map", Integer.class, String.class);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkMapClass(ImmutableMap.of(1, "1"), null,
                                     String.class);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkMapClass(ImmutableMap.of(1, "1"), Integer.class,
                                     null);
        });

        CommonUtil.checkMapClass(ImmutableMap.of(1, "1", 2, "2"),
                                 Integer.class, String.class);
    }
}
