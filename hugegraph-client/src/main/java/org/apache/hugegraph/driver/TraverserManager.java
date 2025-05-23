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

package org.apache.hugegraph.driver;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.kvstore.KvStoreAPI;
import org.apache.hugegraph.api.traverser.AdamicAdarAPI;
import org.apache.hugegraph.api.traverser.AllShortestPathsAPI;
import org.apache.hugegraph.api.traverser.CountAPI;
import org.apache.hugegraph.api.traverser.CrosspointsAPI;
import org.apache.hugegraph.api.traverser.CustomizedCrosspointsAPI;
import org.apache.hugegraph.api.traverser.CustomizedPathsAPI;
import org.apache.hugegraph.api.traverser.EdgeExistenceAPI;
import org.apache.hugegraph.api.traverser.EdgesAPI;
import org.apache.hugegraph.api.traverser.EgonetAPI;
import org.apache.hugegraph.api.traverser.FusiformSimilarityAPI;
import org.apache.hugegraph.api.traverser.JaccardSimilarityAPI;
import org.apache.hugegraph.api.traverser.KneighborAPI;
import org.apache.hugegraph.api.traverser.KoutAPI;
import org.apache.hugegraph.api.traverser.MultiNodeShortestPathAPI;
import org.apache.hugegraph.api.traverser.NeighborRankAPI;
import org.apache.hugegraph.api.traverser.PathsAPI;
import org.apache.hugegraph.api.traverser.PersonalRankAPI;
import org.apache.hugegraph.api.traverser.RaysAPI;
import org.apache.hugegraph.api.traverser.ResourceAllocationAPI;
import org.apache.hugegraph.api.traverser.RingsAPI;
import org.apache.hugegraph.api.traverser.SameNeighborsAPI;
import org.apache.hugegraph.api.traverser.SameNeighborsBatchAPI;
import org.apache.hugegraph.api.traverser.ShortestPathAPI;
import org.apache.hugegraph.api.traverser.SingleSourceShortestPathAPI;
import org.apache.hugegraph.api.traverser.TemplatePathsAPI;
import org.apache.hugegraph.api.traverser.VariablesAPI;
import org.apache.hugegraph.api.traverser.VerticesAPI;
import org.apache.hugegraph.api.traverser.WeightedShortestPathAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Edges;
import org.apache.hugegraph.structure.graph.GraphIterator;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.graph.Vertices;
import org.apache.hugegraph.structure.traverser.CountRequest;
import org.apache.hugegraph.structure.traverser.CountResponse;
import org.apache.hugegraph.structure.traverser.CrosspointsRequest;
import org.apache.hugegraph.structure.traverser.CustomizedCrosspoints;
import org.apache.hugegraph.structure.traverser.CustomizedPathsRequest;
import org.apache.hugegraph.structure.traverser.Egonet;
import org.apache.hugegraph.structure.traverser.EgonetRequest;
import org.apache.hugegraph.structure.traverser.FusiformSimilarity;
import org.apache.hugegraph.structure.traverser.FusiformSimilarityRequest;
import org.apache.hugegraph.structure.traverser.JaccardSimilarity;
import org.apache.hugegraph.structure.traverser.Kneighbor;
import org.apache.hugegraph.structure.traverser.KneighborRequest;
import org.apache.hugegraph.structure.traverser.Kout;
import org.apache.hugegraph.structure.traverser.KoutRequest;
import org.apache.hugegraph.structure.traverser.MultiNodeShortestPathRequest;
import org.apache.hugegraph.structure.traverser.PathOfVertices;
import org.apache.hugegraph.structure.traverser.PathWithMeasure;
import org.apache.hugegraph.structure.traverser.PathsRequest;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.structure.traverser.Prediction;
import org.apache.hugegraph.structure.traverser.RanksWithMeasure;
import org.apache.hugegraph.structure.traverser.SameNeighbors;
import org.apache.hugegraph.structure.traverser.SameNeighborsBatch;
import org.apache.hugegraph.structure.traverser.SameNeighborsBatchRequest;
import org.apache.hugegraph.structure.traverser.SingleSourceJaccardSimilarityRequest;
import org.apache.hugegraph.structure.traverser.TemplatePathsRequest;
import org.apache.hugegraph.structure.traverser.WeightedPath;
import org.apache.hugegraph.structure.traverser.WeightedPaths;
import org.apache.hugegraph.util.E;

public class TraverserManager {

    private final GraphManager graphManager;
    private final JaccardSimilarityAPI jaccardSimilarityAPI;
    private final SameNeighborsAPI sameNeighborsAPI;
    private final ShortestPathAPI shortestPathAPI;
    private final AllShortestPathsAPI allShortestPathsAPI;
    private final SingleSourceShortestPathAPI singleSourceShortestPathAPI;
    private final WeightedShortestPathAPI weightedShortestPathAPI;
    private final MultiNodeShortestPathAPI multiNodeShortestPathAPI;
    private final PathsAPI pathsAPI;
    private final CrosspointsAPI crosspointsAPI;
    private final KoutAPI koutAPI;
    private final KneighborAPI kneighborAPI;
    private final CountAPI countAPI;
    private final RingsAPI ringsAPI;
    private final RaysAPI raysAPI;
    private final CustomizedPathsAPI customizedPathsAPI;
    private final CustomizedCrosspointsAPI customizedCrosspointsAPI;
    private final TemplatePathsAPI templatePathsAPI;
    private final FusiformSimilarityAPI fusiformSimilarityAPI;
    private final AdamicAdarAPI adamicAdarAPI;
    private final ResourceAllocationAPI resourceAllocationAPI;
    private final SameNeighborsBatchAPI sameNeighborsBatchAPI;
    private final EgonetAPI egonetAPI;
    private final NeighborRankAPI neighborRankAPI;
    private final PersonalRankAPI personalRankAPI;
    private final VerticesAPI verticesAPI;
    private final EdgesAPI edgesAPI;
    private final EdgeExistenceAPI edgeExistenceAPI;
    private final VariablesAPI variablesAPI;
    private final KvStoreAPI kvStoreAPI;

    public TraverserManager(RestClient client, GraphManager graphManager) {
        this.graphManager = graphManager;
        String graphSpace = graphManager.graphSpace();
        String graph = graphManager.graph();
        this.jaccardSimilarityAPI = new JaccardSimilarityAPI(client, graphSpace, graph);
        this.sameNeighborsAPI = new SameNeighborsAPI(client, graphSpace, graph);
        this.shortestPathAPI = new ShortestPathAPI(client, graphSpace, graph);
        this.allShortestPathsAPI = new AllShortestPathsAPI(client, graphSpace, graph);
        this.singleSourceShortestPathAPI = new SingleSourceShortestPathAPI(
                client, graphSpace, graph);
        this.weightedShortestPathAPI = new WeightedShortestPathAPI(
                client, graphSpace, graph);
        this.multiNodeShortestPathAPI = new MultiNodeShortestPathAPI(
                client, graphSpace, graph);
        this.pathsAPI = new PathsAPI(client, graphSpace, graph);
        this.crosspointsAPI = new CrosspointsAPI(client, graphSpace, graph);
        this.koutAPI = new KoutAPI(client, graphSpace, graph);
        this.kneighborAPI = new KneighborAPI(client, graphSpace, graph);
        this.countAPI = new CountAPI(client, graphSpace, graph);
        this.ringsAPI = new RingsAPI(client, graphSpace, graph);
        this.raysAPI = new RaysAPI(client, graphSpace, graph);
        this.customizedPathsAPI = new CustomizedPathsAPI(client, graphSpace, graph);
        this.customizedCrosspointsAPI = new CustomizedCrosspointsAPI(
                client, graphSpace, graph);
        this.templatePathsAPI = new TemplatePathsAPI(client, graphSpace, graph);
        this.fusiformSimilarityAPI = new FusiformSimilarityAPI(client, graphSpace, graph);
        this.adamicAdarAPI = new AdamicAdarAPI(client, graphSpace, graph);
        this.resourceAllocationAPI = new ResourceAllocationAPI(client, graphSpace, graph);
        this.sameNeighborsBatchAPI = new SameNeighborsBatchAPI(client, graphSpace, graph);
        this.egonetAPI = new EgonetAPI(client, graphSpace, graph);
        this.neighborRankAPI = new NeighborRankAPI(client, graphSpace, graph);
        this.personalRankAPI = new PersonalRankAPI(client, graphSpace, graph);
        this.verticesAPI = new VerticesAPI(client, graphSpace, graph);
        this.edgesAPI = new EdgesAPI(client, graphSpace, graph);
        this.edgeExistenceAPI = new EdgeExistenceAPI(client, graphSpace, graph);
        this.variablesAPI = new VariablesAPI(client, graphSpace, graph);
        this.kvStoreAPI = new KvStoreAPI(client);
    }

    public JaccardSimilarity jaccardSimilarity(Object vertexId, Object otherId) {
        return this.jaccardSimilarity(vertexId, otherId, Traverser.DEFAULT_MAX_DEGREE);
    }

    public JaccardSimilarity jaccardSimilarity(Object vertexId, Object otherId,
                                               long degree) {
        return this.jaccardSimilarity(vertexId, otherId, Direction.BOTH,
                                      null, degree);
    }

    public JaccardSimilarity jaccardSimilarity(Object vertexId, Object otherId,
                                               Direction direction, String label,
                                               long degree) {
        return this.jaccardSimilarityAPI.get(vertexId, otherId, direction,
                                             label, degree);
    }

    public JaccardSimilarity jaccardSimilarity(
            SingleSourceJaccardSimilarityRequest request) {
        return this.jaccardSimilarityAPI.post(request);
    }

    public SameNeighbors sameNeighbors(Object vertexId, Object otherId) {
        return this.sameNeighbors(vertexId, otherId, Traverser.DEFAULT_MAX_DEGREE);
    }

    public SameNeighbors sameNeighbors(Object vertexId, Object otherId,
                                       long degree) {
        return this.sameNeighbors(vertexId, otherId, Direction.BOTH,
                                  null, degree);
    }

    public SameNeighbors sameNeighbors(Object vertexId, Object otherId,
                                       Direction direction, String label,
                                       long degree) {
        return this.sameNeighbors(vertexId, otherId, direction, label,
                                  degree, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public SameNeighbors sameNeighbors(Object vertexId, Object otherId,
                                       Direction direction, String label,
                                       long degree, long limit) {
        return this.sameNeighborsAPI.get(vertexId, otherId, direction,
                                         label, degree, limit);
    }

    public SameNeighborsBatch sameNeighbors(SameNeighborsBatchRequest request) {
        return this.sameNeighborsBatchAPI.post(request);
    }

    public PathOfVertices shortestPath(Object sourceId, Object targetId,
                                       int maxDepth) {
        return this.shortestPath(sourceId, targetId, Direction.BOTH, null,
                                 maxDepth);
    }

    public PathOfVertices shortestPath(Object sourceId, Object targetId,
                                       Direction direction, int maxDepth) {
        return this.shortestPath(sourceId, targetId, direction, null,
                                 maxDepth);
    }

    public PathOfVertices shortestPath(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth) {
        return this.shortestPath(sourceId, targetId, direction, label, maxDepth,
                                 Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY);
    }

    public PathOfVertices shortestPath(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth, long degree, long capacity) {
        return this.shortestPath(sourceId, targetId, direction, label,
                                 maxDepth, degree, 0L, capacity);
    }

    public PathOfVertices shortestPath(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth, long degree, long skipDegree,
                                       long capacity) {
        return this.shortestPathAPI.get(sourceId, targetId, direction, label,
                                        maxDepth, degree, skipDegree, capacity);
    }

    public PathWithMeasure allShortestPaths(Object sourceId, Object targetId,
                                            int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, Direction.BOTH,
                                     null, maxDepth);
    }

    public PathWithMeasure allShortestPaths(Object sourceId, Object targetId,
                                            Direction direction, int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, direction,
                                     null, maxDepth);
    }

    public PathWithMeasure allShortestPaths(Object sourceId, Object targetId,
                                            Direction direction, String label,
                                            int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, direction,
                                     label, maxDepth, Traverser.DEFAULT_MAX_DEGREE,
                                     Traverser.DEFAULT_CAPACITY);
    }

    public PathWithMeasure allShortestPaths(Object sourceId, Object targetId,
                                            Direction direction, String label,
                                            int maxDepth, long degree,
                                            long capacity) {
        return this.allShortestPaths(sourceId, targetId, direction, label,
                                     maxDepth, degree, 0L, capacity);
    }

    public PathWithMeasure allShortestPaths(Object sourceId, Object targetId,
                                            Direction direction, String label,
                                            int maxDepth, long degree,
                                            long skipDegree, long capacity) {
        return this.allShortestPathsAPI.get(sourceId, targetId, direction,
                                            label, maxDepth, degree,
                                            skipDegree, capacity);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  String weight,
                                                  boolean withVertex, boolean withEdge) {
        return this.singleSourceShortestPath(sourceId, Direction.BOTH, null,
                                             weight, withVertex, withEdge);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  Direction direction,
                                                  String label, String weight,
                                                  boolean withVertex, boolean withEdge) {
        return this.singleSourceShortestPath(sourceId, direction, label, weight,
                                             Traverser.DEFAULT_MAX_DEGREE, 0L,
                                             Traverser.DEFAULT_CAPACITY,
                                             Traverser.DEFAULT_PATHS_LIMIT, withVertex, withEdge);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  Direction direction,
                                                  String label, String weight,
                                                  long degree, long skipDegree,
                                                  long capacity, long limit,
                                                  boolean withVertex, boolean withEdge) {
        return this.singleSourceShortestPathAPI.get(sourceId, direction, label,
                                                    weight, degree, skipDegree,
                                                    capacity, limit,
                                                    withVertex, withEdge);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             String weight, boolean withVertex, boolean withEdge) {
        return this.weightedShortestPath(sourceId, targetId, Direction.BOTH,
                                         null, weight, withVertex, withEdge);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             Direction direction, String label,
                                             String weight,
                                             boolean withVertex, boolean withEdge) {
        return this.weightedShortestPath(sourceId, targetId, direction, label,
                                         weight, Traverser.DEFAULT_MAX_DEGREE, 0L,
                                         Traverser.DEFAULT_CAPACITY, withVertex, withEdge);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             Direction direction,
                                             String label, String weight,
                                             long degree, long skipDegree,
                                             long capacity, boolean withVertex, boolean withEdge) {
        return this.weightedShortestPathAPI.get(sourceId, targetId, direction,
                                                label, weight, degree,
                                                skipDegree, capacity,
                                                withVertex, withEdge);
    }

    public PathsWithVertices multiNodeShortestPath(
            MultiNodeShortestPathRequest request) {
        return this.multiNodeShortestPathAPI.post(request);
    }

    public PathWithMeasure paths(Object sourceId, Object targetId,
                                 int maxDepth) {
        return this.paths(sourceId, targetId, Direction.BOTH, null,
                          maxDepth, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public PathWithMeasure paths(Object sourceId, Object targetId,
                                 Direction direction, int maxDepth,
                                 long limit) {
        return this.paths(sourceId, targetId, direction, null,
                          maxDepth, limit);
    }

    public PathWithMeasure paths(Object sourceId, Object targetId,
                                 Direction direction, String label,
                                 int maxDepth, long limit) {
        return this.paths(sourceId, targetId, direction, label, maxDepth,
                          Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY, limit);
    }

    public PathWithMeasure paths(Object sourceId, Object targetId,
                                 Direction direction, String label,
                                 int maxDepth, long degree, long capacity,
                                 long limit) {
        return this.pathsAPI.get(sourceId, targetId, direction, label,
                                 maxDepth, degree, capacity, limit);
    }

    public PathsWithVertices paths(PathsRequest request) {
        return this.pathsAPI.post(request);
    }

    public PathWithMeasure crosspoint(Object sourceId, Object targetId,
                                      int maxDepth) {
        return this.crosspoint(sourceId, targetId, Direction.BOTH, null,
                               maxDepth, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public PathWithMeasure crosspoint(Object sourceId, Object targetId,
                                      Direction direction, int maxDepth,
                                      long limit) {
        return this.crosspoint(sourceId, targetId, direction, null,
                               maxDepth, limit);
    }

    public PathWithMeasure crosspoint(Object sourceId, Object targetId,
                                      Direction direction, String label,
                                      int maxDepth, long limit) {
        return this.crosspoint(sourceId, targetId, direction, label, maxDepth,
                               Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY, limit);
    }

    public PathWithMeasure crosspoint(Object sourceId, Object targetId,
                                      Direction direction, String label,
                                      int maxDepth, long degree, long capacity,
                                      long limit) {
        return this.crosspointsAPI.get(sourceId, targetId, direction, label,
                                       maxDepth, degree, capacity, limit);
    }

    public Map<String, Object> kout(Object sourceId, int depth) {
        return this.kout(sourceId, Direction.BOTH, depth);
    }

    public Map<String, Object> kout(Object sourceId, Direction direction,
                                    int depth) {
        return this.kout(sourceId, direction, null, depth, true);
    }

    public Map<String, Object> kout(Object sourceId, Direction direction,
                                    String label, int depth, boolean nearest) {
        return this.kout(sourceId, direction, label, depth, nearest,
                         Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                         Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public Map<String, Object> kout(Object sourceId, Direction direction,
                                    String label, int depth, boolean nearest,
                                    long degree, long capacity, long limit) {
        return this.koutAPI.get(sourceId, direction, label, depth, nearest,
                                degree, capacity, limit);
    }

    public Kout kout(KoutRequest request) {
        return this.koutAPI.post(request);
    }

    public Map<String, Object> kneighbor(Object sourceId, int depth) {
        return this.kneighbor(sourceId, Direction.BOTH, null, depth);
    }

    public Map<String, Object> kneighbor(Object sourceId, Direction direction,
                                         int depth) {
        return this.kneighbor(sourceId, direction, null, depth);
    }

    public Map<String, Object> kneighbor(Object sourceId, Direction direction,
                                         String label, int depth) {
        return this.kneighbor(sourceId, direction, label, depth,
                              Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public Map<String, Object> kneighbor(Object sourceId, Direction direction,
                                         String label, int depth,
                                         long degree, long limit) {
        return this.kneighborAPI.get(sourceId, direction, label, depth,
                                     degree, limit);
    }

    public Kneighbor kneighbor(KneighborRequest request) {
        return this.kneighborAPI.post(request);
    }

    public CountResponse count(CountRequest request) {
        return this.countAPI.post(request);
    }

    public PathWithMeasure rings(Object sourceId, int depth) {
        return this.rings(sourceId, Direction.BOTH, null, depth, true,
                          Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                          Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public PathWithMeasure rings(Object sourceId, Direction direction, String label,
                                 int depth, boolean sourceInRing, long degree,
                                 long capacity, long limit) {
        return this.ringsAPI.get(sourceId, direction, label, depth,
                                 sourceInRing, degree, capacity, limit);
    }

    public PathWithMeasure rays(Object sourceId, int depth) {
        return this.rays(sourceId, Direction.BOTH, null, depth,
                         Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                         Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public PathWithMeasure rays(Object sourceId, Direction direction, String label,
                                int depth, long degree, long capacity, long limit) {
        return this.raysAPI.get(sourceId, direction, label, depth, degree,
                                capacity, limit);
    }

    public PathsWithVertices customizedPaths(CustomizedPathsRequest request) {
        return this.customizedPathsAPI.post(request);
    }

    public CustomizedCrosspoints customizedCrosspointss(CrosspointsRequest request) {
        return this.customizedCrosspointsAPI.post(request);
    }

    public PathsWithVertices count(TemplatePathsRequest request) {
        return this.templatePathsAPI.post(request);
    }

    public FusiformSimilarity fusiformSimilarity(FusiformSimilarityRequest request) {
        return this.fusiformSimilarityAPI.post(request);
    }

    public Prediction adamicadar(Object vertexId, Object otherId,
                                 Direction direction, String label, long degree) {
        return this.adamicAdarAPI.get(vertexId, otherId, direction,
                                      label, degree);
    }

    public Prediction resourceAllocation(Object vertexId, Object otherId,
                                         Direction direction, String label,
                                         long degree) {
        return this.resourceAllocationAPI.get(vertexId, otherId, direction,
                                              label, degree);
    }

    public Egonet egonet(EgonetRequest request) {
        return this.egonetAPI.post(request);
    }

    public RanksWithMeasure neighborRank(NeighborRankAPI.Request request) {
        return this.neighborRankAPI.post(request);
    }

    public RanksWithMeasure personalRank(PersonalRankAPI.Request request) {
        return this.personalRankAPI.post(request);
    }

    public List<Shard> vertexShards(long splitSize) {
        return this.verticesAPI.shards(splitSize);
    }

    public List<Shard> edgeShards(long splitSize) {
        return this.edgesAPI.shards(splitSize);
    }

    public List<Shard> variablesShards(long splitSize) {
        return this.variablesAPI.shards(splitSize);
    }

    public List<Shard> kvStoreShards(long splitSize) {
        return this.kvStoreAPI.shards(splitSize);
    }

    public List<Vertex> vertices(List<Object> ids) {
        List<Vertex> vertices = this.verticesAPI.list(ids);
        for (Vertex vertex : vertices) {
            vertex.attachManager(this.graphManager);
        }
        return vertices;
    }

    public Vertices vertices(Shard shard) {
        Vertices vertices = this.vertices(shard, null, 0L);
        E.checkState(vertices.page() == null,
                     "Can't contains page when not in paging");
        return vertices;
    }

    public Vertices vertices(Shard shard, String page) {
        E.checkArgument(page != null, "Page can't be null");
        return this.vertices(shard, page, Traverser.DEFAULT_PAGE_LIMIT);
    }

    public Vertices vertices(Shard shard, String page, long pageLimit) {
        E.checkArgument(page == null || pageLimit >= 0,
                        "Page limit must be >= 0 when page is not null");
        Vertices vertices = this.verticesAPI.scan(shard, page, pageLimit);

        for (Vertex vertex : vertices.results()) {
            vertex.attachManager(this.graphManager);
        }
        return vertices;
    }

    public Vertices variables(Shard shard) {
        Vertices vertices = this.variables(shard, null, 0L);
        E.checkState(vertices.page() == null,
                     "Can't contains page when not in paging");
        return vertices;
    }

    public Vertices variables(Shard shard, String page) {
        E.checkArgument(page != null, "Page can't be null");
        return this.variables(shard, page, Traverser.DEFAULT_PAGE_LIMIT);
    }

    public Vertices variables(Shard shard, String page, long pageLimit) {
        E.checkArgument(page == null || pageLimit >= 0,
                        "Page limit must be >= 0 when page is not null");
        Vertices vertices = this.variablesAPI.scan(shard, page, pageLimit);

        for (Vertex vertex : vertices.results()) {
            vertex.attachManager(this.graphManager);
        }
        return vertices;
    }

    public Vertices kvStores(Shard shard, String page, long pageLimit) {
        E.checkArgument(page == null || pageLimit >= 0,
                        "Page limit must be >= 0 when page is not null");
        Vertices vertices = this.kvStoreAPI.scan(shard, page, pageLimit);

        for (Vertex vertex : vertices.results()) {
            vertex.attachManager(this.graphManager);
        }
        return vertices;
    }

    public Iterator<Vertex> iteratorVertices(Shard shard, int sizePerPage) {
        return new GraphIterator<>(this.graphManager, sizePerPage, (page) -> {
            return this.vertices(shard, page, sizePerPage);
        });
    }

    public List<Edge> edges(List<String> ids) {
        List<Edge> edges = this.edgesAPI.list(ids);
        for (Edge edge : edges) {
            edge.attachManager(this.graphManager);
        }
        return edges;
    }

    public Edges edges(Shard shard) {
        Edges edges = this.edges(shard, null, 0L);
        E.checkState(edges.page() == null,
                     "Can't contains page when not in paging");
        return edges;
    }

    public Edges edges(Shard shard, String page) {
        E.checkArgument(page != null, "Page can't be null");
        return this.edges(shard, page, Traverser.DEFAULT_PAGE_LIMIT);
    }

    public Edges edges(Shard shard, String page, long pageLimit) {
        E.checkArgument(page == null || pageLimit >= 0,
                        "Page limit must be >= 0 when page is not null");
        Edges edges = this.edgesAPI.scan(shard, page, pageLimit);
        for (Edge edge : edges.results()) {
            edge.attachManager(this.graphManager);
        }
        return edges;
    }

    public Iterator<Edge> iteratorEdges(Shard shard, int sizePerPage) {
        return new GraphIterator<>(this.graphManager, sizePerPage, (page) -> {
            return this.edges(shard, page, sizePerPage);
        });
    }

    public List<Edge> edgeExistence(Object sourceId, Object targetId, String edgeLabel,
                                    String sortValues, int limit) {
        return this.edgeExistenceAPI.get(sourceId, targetId, edgeLabel, sortValues, limit);
    }

    public List<Edge> edgeExistence(Object sourceId, Object targetId) {
        return this.edgeExistenceAPI.get(sourceId, targetId, "", "", -1);
    }

    public List<Edge> edgeExistence(Object sourceId, Object targetId, String edgeLabel) {
        return this.edgeExistenceAPI.get(sourceId, targetId, edgeLabel, null, -1);
    }
}
