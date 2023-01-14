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

import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CypherApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @Before
    public void prepareData() {
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testCreate() {
        String cypher = "CREATE (n:person { name : 'test', age: 20, city: 'Hefei' }) return n";
        ResultSet resultSet = cypher().execute(cypher);
        Assert.assertEquals(1, resultSet.size());
    }

    @Test
    public void testRelationQuery() {
        String cypher = "MATCH (n:person)-[r:knows]->(friend:person)\n" +
                        "WHERE n.name = 'marko'\n" +
                        "RETURN n, friend.name AS friend";

        ResultSet resultSet = cypher().execute(cypher);
        Assert.assertEquals(2, resultSet.size());
    }
}
