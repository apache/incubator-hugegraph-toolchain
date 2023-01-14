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

package org.apache.hugegraph.api.traverser;

import org.apache.hugegraph.api.BaseApiTest;
import org.junit.BeforeClass;

public class TraverserApiTest extends BaseApiTest {

    protected static SameNeighborsAPI sameNeighborsAPI;
    protected static JaccardSimilarityAPI jaccardSimilarityAPI;
    protected static ShortestPathAPI shortestPathAPI;
    protected static AllShortestPathsAPI allShortestPathsAPI;
    protected static SingleSourceShortestPathAPI singleSourceShortestPathAPI;
    protected static WeightedShortestPathAPI weightedShortestPathAPI;
    protected static MultiNodeShortestPathAPI multiNodeShortestPathAPI;
    protected static PathsAPI pathsAPI;
    protected static CrosspointsAPI crosspointsAPI;
    protected static KoutAPI koutAPI;
    protected static KneighborAPI kneighborAPI;

    protected static RingsAPI ringsAPI;
    protected static RaysAPI raysAPI;

    protected static CountAPI countAPI;

    protected static CustomizedPathsAPI customizedPathsAPI;
    protected static CustomizedCrosspointsAPI customizedCrosspointsAPI;
    protected static FusiformSimilarityAPI fusiformSimilarityAPI;
    protected static TemplatePathsAPI templatePathsAPI;

    protected static NeighborRankAPI neighborRankAPI;
    protected static PersonalRankAPI personalRankAPI;

    protected static VerticesAPI verticesAPI;
    protected static EdgesAPI edgesAPI;

    @BeforeClass
    public static void init() {
        BaseApiTest.init();

        sameNeighborsAPI = new SameNeighborsAPI(client, GRAPH);
        jaccardSimilarityAPI = new JaccardSimilarityAPI(client, GRAPH);
        shortestPathAPI = new ShortestPathAPI(client, GRAPH);
        allShortestPathsAPI = new AllShortestPathsAPI(client, GRAPH);
        singleSourceShortestPathAPI = new SingleSourceShortestPathAPI(client,
                                                                      GRAPH);
        weightedShortestPathAPI = new WeightedShortestPathAPI(client, GRAPH);
        multiNodeShortestPathAPI = new MultiNodeShortestPathAPI(client, GRAPH);
        pathsAPI = new PathsAPI(client, GRAPH);
        crosspointsAPI = new CrosspointsAPI(client, GRAPH);
        koutAPI = new KoutAPI(client, GRAPH);
        kneighborAPI = new KneighborAPI(client, GRAPH);

        ringsAPI = new RingsAPI(client, GRAPH);
        raysAPI = new RaysAPI(client, GRAPH);

        countAPI = new CountAPI(client, GRAPH);

        customizedPathsAPI = new CustomizedPathsAPI(client, GRAPH);
        customizedCrosspointsAPI = new CustomizedCrosspointsAPI(client, GRAPH);
        fusiformSimilarityAPI = new FusiformSimilarityAPI(client, GRAPH);
        templatePathsAPI = new TemplatePathsAPI(client, GRAPH);

        neighborRankAPI = new NeighborRankAPI(client, GRAPH);
        personalRankAPI = new PersonalRankAPI(client, GRAPH);

        verticesAPI = new VerticesAPI(client, GRAPH);
        edgesAPI = new EdgesAPI(client, GRAPH);
    }
}
