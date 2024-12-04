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
    protected static EdgeExistenceAPI edgeExistenceAPI;

    @BeforeClass
    public static void init() {
        BaseApiTest.init();

        sameNeighborsAPI = new SameNeighborsAPI(client, GRAPHSPACE, GRAPH);
        jaccardSimilarityAPI = new JaccardSimilarityAPI(client, GRAPHSPACE, GRAPH);
        shortestPathAPI = new ShortestPathAPI(client, GRAPHSPACE, GRAPH);
        allShortestPathsAPI = new AllShortestPathsAPI(client, GRAPHSPACE, GRAPH);
        singleSourceShortestPathAPI = new SingleSourceShortestPathAPI(client, GRAPHSPACE,
                                                                      GRAPH);
        weightedShortestPathAPI = new WeightedShortestPathAPI(client, GRAPHSPACE, GRAPH);
        multiNodeShortestPathAPI = new MultiNodeShortestPathAPI(client, GRAPHSPACE, GRAPH);
        pathsAPI = new PathsAPI(client, GRAPHSPACE, GRAPH);
        crosspointsAPI = new CrosspointsAPI(client, GRAPHSPACE, GRAPH);
        koutAPI = new KoutAPI(client, GRAPHSPACE, GRAPH);
        kneighborAPI = new KneighborAPI(client, GRAPHSPACE, GRAPH);

        ringsAPI = new RingsAPI(client, GRAPHSPACE, GRAPH);
        raysAPI = new RaysAPI(client, GRAPHSPACE, GRAPH);

        countAPI = new CountAPI(client, GRAPHSPACE, GRAPH);

        customizedPathsAPI = new CustomizedPathsAPI(client, GRAPHSPACE, GRAPH);
        customizedCrosspointsAPI = new CustomizedCrosspointsAPI(client, GRAPHSPACE, GRAPH);
        fusiformSimilarityAPI = new FusiformSimilarityAPI(client, GRAPHSPACE, GRAPH);
        templatePathsAPI = new TemplatePathsAPI(client, GRAPHSPACE, GRAPH);

        neighborRankAPI = new NeighborRankAPI(client, GRAPHSPACE, GRAPH);
        personalRankAPI = new PersonalRankAPI(client, GRAPHSPACE, GRAPH);

        verticesAPI = new VerticesAPI(client, GRAPHSPACE, GRAPH);
        edgesAPI = new EdgesAPI(client, GRAPHSPACE, GRAPH);

        edgeExistenceAPI = new EdgeExistenceAPI(client, GRAPH);
    }
}
