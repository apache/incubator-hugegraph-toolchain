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

package org.apache.hugegraph.controller.algorithm;

import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.client.api.traverser.NeighborRankAPI;
import org.apache.hugegraph.client.api.traverser.PersonalRankAPI;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.*;
import org.apache.hugegraph.entity.query.*;
import org.apache.hugegraph.service.algorithm.OltpAlgoService;
import org.apache.hugegraph.structure.traverser.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/algorithms/oltp")
public class OltpAlgoController extends BaseController {

    @Autowired
    private OltpAlgoService service;

    @PostMapping("shortestPath")
    public GremlinResult shortPath(@PathVariable("graphspace") String graphSpace,
                                   @PathVariable("graph") String graph,
                                   @RequestBody ShortestPathEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.shortestPath(client, body);
    }

    @PostMapping("rings")
    public GremlinResult rings(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @RequestBody RingsEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.rings(client, body);
    }

    @PostMapping("advancedPaths")
    public GremlinResult advancedPaths(@PathVariable("graphspace") String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestBody PathsRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.advancedpaths(client, body);
    }

    @PostMapping("sameNeighbors")
    public GremlinResult sameNeighbors(@PathVariable("graphspace") String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestBody SameNeighborsEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.sameNeighbors(client, body);
    }

    @PostMapping("kout")
    public GremlinResult kout(@PathVariable("graphspace") String graphSpace,
                              @PathVariable("graph") String graph,
                              @RequestBody KoutEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.kout(client, body);
    }

    @PostMapping("kout_post")
    public GremlinResult koutPost(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody KoutRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.koutPost(client, body);
    }

    @PostMapping("kneighbor")
    public GremlinResult kneighbor(@PathVariable("graphspace") String graphSpace,
                                   @PathVariable("graph") String graph,
                                   @RequestBody KneighborEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.kneighbor(client, body);
    }

    @PostMapping("kneighbor_post")
    public GremlinResult kneighborPost(@PathVariable("graphspace") String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestBody KneighborRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.kneighborPost(client, body);
    }

    @PostMapping("jaccardSimilarity")
    public JaccardsimilarityView jaccardSimilarity(@PathVariable("graphspace") String graphSpace,
                                                   @PathVariable("graph") String graph,
                                                   @RequestBody JaccardSimilarityEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.jaccardSimilarity(client, body);
    }

    @PostMapping("jaccardSimilarity_post")
    public JaccardsimilarityView jaccardSimilarityPost(@PathVariable("graphspace") String graphSpace,
                                                       @PathVariable("graph") String graph,
                                                       @RequestBody SingleSourceJaccardSimilarityRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.jaccardSimilarityPost(client, body);
    }

    @PostMapping("personalrank")
    public RanksView personalRank(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody PersonalRankAPI.Request body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.personalRank(client, body);
    }

    @PostMapping("neighborrank")
    public RanksView neighborRank(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody NeighborRankAPI.Request body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.neighborRank(client, body);
    }

    @PostMapping("allshortestpaths")
    public GremlinResult allShortPaths(@PathVariable("graphspace") String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestBody AllShortestPathsEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.allShortestPaths(client, body);
    }

    @PostMapping("weightedshortestpath")
    public GremlinResult weightedShortestPath(@PathVariable("graphspace") String graphSpace,
                                              @PathVariable("graph") String graph,
                                              @RequestBody WeightedShortestPathEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.weightedShortestPath(client, body);
    }

    @PostMapping("singlesourceshortestpath")
    public GremlinResult singleSourceShortestPath(@PathVariable("graphspace") String graphSpace,
                                                  @PathVariable("graph") String graph,
                                                  @RequestBody SingleSourceShortestPathEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.singleSourceShortestPath(client, body);
    }

    @PostMapping("multinodeshortestpath")
    public GremlinResult multiNodeShortestPath(@PathVariable("graphspace") String graphSpace,
                                               @PathVariable("graph") String graph,
                                               @RequestBody MultiNodeShortestPathRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.multiNodeShortestPath(client, body);
    }

    @PostMapping("paths")
    public GremlinResult paths(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @RequestBody PathsEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.paths(client, body);
    }

    @PostMapping("customizedpaths")
    public GremlinResult customizedPaths(@PathVariable("graphspace") String graphSpace,
                                         @PathVariable("graph") String graph,
                                         @RequestBody CustomizedPathsRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.customizedPaths(client, body);
    }

    @PostMapping("templatepaths")
    public GremlinResult templatePaths(@PathVariable("graphspace") String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestBody TemplatePathsRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.templatePaths(client, body);
    }

    @PostMapping("crosspoints")
    public GremlinResult crosspoints(@PathVariable("graphspace") String graphSpace,
                                     @PathVariable("graph") String graph,
                                     @RequestBody CrossPointsEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.crosspoints(client, body);
    }

    @PostMapping("customizedcrosspoints")
    public GremlinResult customizedcrosspoints(@PathVariable("graphspace") String graphSpace,
                                               @PathVariable("graph") String graph,
                                               @RequestBody CrosspointsRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.customizedcrosspoints(client, body);
    }

    @PostMapping("rays")
    public GremlinResult rays(@PathVariable("graphspace") String graphSpace,
                              @PathVariable("graph") String graph,
                              @RequestBody RaysEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.rays(client, body);
    }

    @PostMapping("fusiformsimilarity")
    public FusiformsimilarityView fusiformsimilarity(@PathVariable("graphspace") String graphSpace,
                                                     @PathVariable("graph") String graph,
                                                     @RequestBody FusiformSimilarityRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.fusiformsimilarity(client, body);
    }

    @PostMapping("adamicadar")
    public Map<String, Double> adamicadar(@PathVariable("graphspace") String graphSpace,
                                          @PathVariable("graph") String graph,
                                          @RequestBody AdamicadarEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.adamicadar(client, body);
    }

    @PostMapping("resourceallocation")
    public Map<String, Double> resourceallocation(@PathVariable("graphspace") String graphSpace,
                                                  @PathVariable("graph") String graph,
                                                  @RequestBody ResourceallocationEntity body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.resourceallocation(client, body);
    }

    @PostMapping("sameneighborsbatch")
    public GremlinResult sameneighborsbatch(@PathVariable("graphspace") String graphSpace,
                                            @PathVariable("graph") String graph,
                                            @RequestBody SameNeighborsBatchRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.sameneighborsbatch(client, body);
    }

    @PostMapping("egonet")
    public EgonetView egonet(@PathVariable("graphspace") String graphSpace,
                             @PathVariable("graph") String graph,
                             @RequestBody EgonetRequest body) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.egonet(client, body);
    }
}
