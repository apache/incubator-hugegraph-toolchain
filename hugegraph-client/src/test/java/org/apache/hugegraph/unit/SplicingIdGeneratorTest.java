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

import java.util.List;

import org.apache.hugegraph.util.SplicingIdGenerator;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class SplicingIdGeneratorTest {

    @Test
    public void testConcatIds() {
        Assert.assertEquals("a>b>c>d",
                            SplicingIdGenerator.concat("a", "b", "c", "d"));
        // > => `>
        Assert.assertEquals("a>`>>c>`>",
                            SplicingIdGenerator.concat("a", ">", "c", ">"));
        Assert.assertEquals("a>b`>c>d",
                            SplicingIdGenerator.concat("a", "b>c", "d"));
    }

    @Test
    public void testSplitIds() {
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d"},
                                 SplicingIdGenerator.split("a>b>c>d"));
        Assert.assertArrayEquals(new String[]{"a", ">", "c", ">"},
                                 SplicingIdGenerator.split("a>`>>c>`>"));
        Assert.assertArrayEquals(new String[]{"a", "b>c", "d"},
                                 SplicingIdGenerator.split("a>b`>c>d"));
    }

    @Test
    public void testConcatValues() {
        Assert.assertEquals("a!1!c!d",
                            SplicingIdGenerator.concatValues("a", 1, 'c', "d"));
        Assert.assertEquals("a!`!!1!d",
                            SplicingIdGenerator.concatValues("a", "!", 1, 'd'));
        Assert.assertEquals("a!b`!c!d",
                            SplicingIdGenerator.concatValues("a", "b!c", "d"));

        List<Object> values = ImmutableList.of("a", 1, 'c', "d");
        Assert.assertEquals("a!1!c!d",
                            SplicingIdGenerator.concatValues(values));
        values = ImmutableList.of("a", "!", 1, 'd');
        Assert.assertEquals("a!`!!1!d",
                            SplicingIdGenerator.concatValues(values));
        values = ImmutableList.of("a", "b!c", "d");
        Assert.assertEquals("a!b`!c!d",
                            SplicingIdGenerator.concatValues(values));
    }
}
