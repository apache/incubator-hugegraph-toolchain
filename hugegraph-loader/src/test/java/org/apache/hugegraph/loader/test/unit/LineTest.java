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

import org.junit.Test;

import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.testutil.Assert;

public class LineTest {

    @Test
    public void testInvalidParam() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Line(null, new String[]{"id", "name"},
                     new Object[]{1, "marko"});
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Line("1,marko", null, new Object[]{1, "marko"});
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Line("1,marko", new String[]{"id", "name"}, null);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Line("1,marko", new String[]{"id", "name"}, new Object[]{1});
        });
    }

    @Test
    public void testNameValues() {
        Line line = new Line("1,marko,27",
                             new String[]{"id", "name", "age"},
                             new Object[]{1, "marko", 27});
        Assert.assertArrayEquals(new String[]{"id", "name", "age"},
                                 line.names());
        Assert.assertArrayEquals(new Object[]{1, "marko", 27}, line.values());
    }

    @Test
    public void testRetainAll() {
        Line line = new Line("1,marko,27",
                             new String[]{"id", "name", "age"},
                             new Object[]{1, "marko", 27});
        line.retainAll(new String[]{"id"});
        Assert.assertArrayEquals(new String[]{"id"}, line.names());
        Assert.assertArrayEquals(new Object[]{1}, line.values());
    }
}
