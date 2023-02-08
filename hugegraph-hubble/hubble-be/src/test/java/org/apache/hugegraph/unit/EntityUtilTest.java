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

import java.util.Date;

import org.junit.Test;

import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.EntityUtil;

public class EntityUtilTest {

    @Test
    public void testMerge() throws InterruptedException {
        GraphConnection oldEntity;
        GraphConnection newEntity;
        oldEntity = new GraphConnection(1, "conn1", "graph1", "host1", 8001,
                                        30, "", "", true, "",
                                        new Date(),"http","","");
        Thread.sleep(10);
        newEntity = new GraphConnection(2, "conn2", "graph2", "host2", 8002,
                                        40, "u", "p", false, "xxx",
                                        new Date(), "http", "", "");

        GraphConnection entity = EntityUtil.merge(oldEntity, newEntity);
        Assert.assertEquals(oldEntity.getId(), entity.getId());
        Assert.assertEquals(newEntity.getName(), entity.getName());
        Assert.assertEquals(newEntity.getGraph(), entity.getGraph());
        Assert.assertEquals(newEntity.getHost(), entity.getHost());
        Assert.assertEquals(newEntity.getPort(), entity.getPort());
        Assert.assertEquals(oldEntity.getTimeout(), entity.getTimeout());
        Assert.assertEquals(newEntity.getUsername(), entity.getUsername());
        Assert.assertEquals(newEntity.getPassword(), entity.getPassword());
        Assert.assertEquals(oldEntity.getCreateTime(), entity.getCreateTime());
    }
}
