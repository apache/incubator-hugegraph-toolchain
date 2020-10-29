/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.baidu.hugegraph.api.auth.AccessApiTest;
import com.baidu.hugegraph.api.auth.BelongApiTest;
import com.baidu.hugegraph.api.auth.GroupApiTest;
import com.baidu.hugegraph.api.auth.TargetApiTest;
import com.baidu.hugegraph.api.auth.UserApiTest;
import com.baidu.hugegraph.api.traverser.AllShortestPathsApiTest;
import com.baidu.hugegraph.api.traverser.CommonTraverserApiTest;
import com.baidu.hugegraph.api.traverser.CountApiTest;
import com.baidu.hugegraph.api.traverser.CustomizedPathsApiTest;
import com.baidu.hugegraph.api.traverser.FusiformSimilarityApiTest;
import com.baidu.hugegraph.api.traverser.JaccardSimilarityApiTest;
import com.baidu.hugegraph.api.traverser.KneighborApiTest;
import com.baidu.hugegraph.api.traverser.KoutApiTest;
import com.baidu.hugegraph.api.traverser.NeighborRankApiTest;
import com.baidu.hugegraph.api.traverser.PathsApiTest;
import com.baidu.hugegraph.api.traverser.PersonalRankApiTest;
import com.baidu.hugegraph.api.traverser.RingsRaysApiTest;
import com.baidu.hugegraph.api.traverser.SameNeighborsApiTest;
import com.baidu.hugegraph.api.traverser.ShortestPathApiTest;
import com.baidu.hugegraph.api.traverser.SingleSourceShortestPathApiTest;
import com.baidu.hugegraph.api.traverser.TemplatePathsApiTest;
import com.baidu.hugegraph.api.traverser.WeightedShortestPathApiTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    PropertyKeyApiTest.class,
    VertexLabelApiTest.class,
    EdgeLabelApiTest.class,
    IndexLabelApiTest.class,

    VertexApiTest.class,
    EdgeApiTest.class,
    BatchUpdateElementApiTest.class,

    GremlinApiTest.class,
    VariablesApiTest.class,
    TaskApiTest.class,
    JobApiTest.class,
    RestoreApiTest.class,

    CommonTraverserApiTest.class,
    KoutApiTest.class,
    KneighborApiTest.class,
    PathsApiTest.class,
    CountApiTest.class,
    RingsRaysApiTest.class,
    SameNeighborsApiTest.class,
    JaccardSimilarityApiTest.class,
    ShortestPathApiTest.class,
    AllShortestPathsApiTest.class,
    SingleSourceShortestPathApiTest.class,
    WeightedShortestPathApiTest.class,
    CustomizedPathsApiTest.class,
    TemplatePathsApiTest.class,
    FusiformSimilarityApiTest.class,
    NeighborRankApiTest.class,
    PersonalRankApiTest.class,

    TargetApiTest.class,
    GroupApiTest.class,
    UserApiTest.class,
    AccessApiTest.class,
    BelongApiTest.class
})
public class ApiTestSuite {
}
