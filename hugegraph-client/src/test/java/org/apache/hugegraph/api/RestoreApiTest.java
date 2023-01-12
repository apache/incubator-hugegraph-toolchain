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

package org.apache.hugegraph.api;

import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Before;
import org.junit.Test;

public class RestoreApiTest extends BaseApiTest {

    private static final String GRAPH = "hugegraph";

    @Before
    public void initGraphModeNone() {
        graphsAPI.mode(GRAPH, GraphMode.NONE);
    }

    @Test
    public void testGetGraphMode() {
        GraphMode mode = graphsAPI.mode(GRAPH);
        Assert.assertEquals(GraphMode.NONE, mode);
    }

    @Test
    public void testSetGraphMode() {
        GraphMode mode = graphsAPI.mode(GRAPH);
        Assert.assertEquals(GraphMode.NONE, mode);

        graphsAPI.mode(GRAPH, GraphMode.RESTORING);
        mode = graphsAPI.mode(GRAPH);
        Assert.assertEquals(GraphMode.RESTORING, mode);

        graphsAPI.mode(GRAPH, GraphMode.MERGING);
        mode = graphsAPI.mode(GRAPH);
        Assert.assertEquals(GraphMode.MERGING, mode);

        graphsAPI.mode(GRAPH, GraphMode.NONE);
        mode = graphsAPI.mode(GRAPH);
        Assert.assertEquals(GraphMode.NONE, mode);
    }
}
