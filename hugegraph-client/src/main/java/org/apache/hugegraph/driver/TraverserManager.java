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

import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.api.traverser.AllShortestPathsAPI;
import org.apache.hugegraph.api.traverser.CountAPI;
import org.apache.hugegraph.api.traverser.CrosspointsAPI;
import org.apache.hugegraph.api.traverser.CustomizedCrosspointsAPI;
import org.apache.hugegraph.api.traverser.CustomizedPathsAPI;
import org.apache.hugegraph.api.traverser.EdgesAPI;
import org.apache.hugegraph.api.traverser.FusiformSimilarityAPI;
import org.apache.hugegraph.api.traverser.JaccardSimilarityAPI;
import org.apache.hugegraph.api.traverser.KneighborAPI;
import org.apache.hugegraph.api.traverser.KoutAPI;
import org.apache.hugegraph.api.traverser.MultiNodeShortestPathAPI;
import org.apache.hugegraph.api.traverser.NeighborRankAPI;
import org.apache.hugegraph.api.traverser.PathsAPI;
import org.apache.hugegraph.api.traverser.PersonalRankAPI;
import org.apache.hugegraph.api.traverser.RaysAPI;
import org.apache.hugegraph.api.traverser.RingsAPI;
import org.apache.hugegraph.api.traverser.SameNeighborsAPI;
import org.apache.hugegraph.api.traverser.ShortestPathAPI;
import org.apache.hugegraph.api.traverser.SingleSourceShortestPathAPI;
import org.apache.hugegraph.api.traverser.TemplatePathsAPI;
import org.apache.hugegraph.api.traverser.VerticesAPI;
import org.apache.hugegraph.api.traverser.WeightedShortestPathAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Edges;
import org.apache.hugegraph.structure.graph.GraphIterator;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.graph.Vertices;
import org.apache.hugegraph.structure.traverser.CountRequest;
import org.apache.hugegraph.structure.traverser.CrosspointsRequest;
import org.apache.hugegraph.structure.traverser.CustomizedCrosspoints;
import org.apache.hugegraph.structure.traverser.MultiNodeShortestPathRequest;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.structure.traverser.FusiformSimilarity;
import org.apache.hugegraph.structure.traverser.FusiformSimilarityRequest;
import org.apache.hugegraph.structure.traverser.SingleSourceJaccardSimilarityRequest;
import org.apache.hugegraph.structure.traverser.Kneighbor;
import org.apache.hugegraph.structure.traverser.KneighborRequest;
import org.apache.hugegraph.structure.traverser.Kout;
import org.apache.hugegraph.structure.traverser.KoutRequest;
import org.apache.hugegraph.structure.traverser.CustomizedPathsRequest;
import org.apache.hugegraph.structure.traverser.PathsRequest;
import org.apache.hugegraph.structure.traverser.Ranks;
import org.apache.hugegraph.structure.traverser.TemplatePathsRequest;
import org.apache.hugegraph.structure.traverser.WeightedPath;
import org.apache.hugegraph.structure.traverser.WeightedPaths;
import org.apache.hugegraph.util.E;

public class TraverserManager {

    private final GraphManager graphManager;
    private JaccardSimilarityAPI jaccardSimilarityAPI;
    private SameNeighborsAPI sameNeighborsAPI;
    private ShortestPathAPI shortestPathAPI;
    private AllShortestPathsAPI allShortestPathsAPI;
    private SingleSourceShortestPathAPI singleSourceShortestPathAPI;
    private WeightedShortestPathAPI weightedShortestPathAPI;
    private MultiNodeShortestPathAPI multiNodeShortestPathAPI;
    private PathsAPI pathsAPI;
    private CrosspointsAPI crosspointsAPI;
    private KoutAPI koutAPI;
    private KneighborAPI kneighborAPI;
    private CountAPI countAPI;
    private RingsAPI ringsAPI;
    private RaysAPI raysAPI;
    private CustomizedPathsAPI customizedPathsAPI;
    private CustomizedCrosspointsAPI customizedCrosspointsAPI;
    private TemplatePathsAPI templatePathsAPI;
    private FusiformSimilarityAPI fusiformSimilarityAPI;
    private NeighborRankAPI neighborRankAPI;
    private PersonalRankAPI personalRankAPI;
    private VerticesAPI verticesAPI;
    private EdgesAPI edgesAPI;

    public TraverserManager(RestClient client, GraphManager graphManager) {
        this.graphManager = graphManager;
        String graph = graphManager.graph();
        this.jaccardSimilarityAPI = new JaccardSimilarityAPI(client, graph);
        this.sameNeighborsAPI = new SameNeighborsAPI(client, graph);
        this.shortestPathAPI = new ShortestPathAPI(client, graph);
        this.allShortestPathsAPI = new AllShortestPathsAPI(client, graph);
        this.singleSourceShortestPathAPI = new SingleSourceShortestPathAPI(client, graph);
        this.weightedShortestPathAPI = new WeightedShortestPathAPI(client, graph);
        this.multiNodeShortestPathAPI = new MultiNodeShortestPathAPI(client, graph);
        this.pathsAPI = new PathsAPI(client, graph);
        this.crosspointsAPI = new CrosspointsAPI(client, graph);
        this.koutAPI = new KoutAPI(client, graph);
        this.kneighborAPI = new KneighborAPI(client, graph);
        this.countAPI = new CountAPI(client, graph);
        this.ringsAPI = new RingsAPI(client, graph);
        this.raysAPI = new RaysAPI(client, graph);
        this.customizedPathsAPI = new CustomizedPathsAPI(client, graph);
        this.customizedCrosspointsAPI = new CustomizedCrosspointsAPI(client, graph);
        this.templatePathsAPI = new TemplatePathsAPI(client, graph);
        this.fusiformSimilarityAPI = new FusiformSimilarityAPI(client, graph);
        this.neighborRankAPI = new NeighborRankAPI(client, graph);
        this.personalRankAPI = new PersonalRankAPI(client, graph);
        this.verticesAPI = new VerticesAPI(client, graph);
        this.edgesAPI = new EdgesAPI(client, graph);
    }

    public double jaccardSimilarity(Object vertexId, Object otherId) {
        return this.jaccardSimilarity(vertexId, otherId, Traverser.DEFAULT_MAX_DEGREE);
    }

    public double jaccardSimilarity(Object vertexId, Object otherId,
                                    long degree) {
        return this.jaccardSimilarity(vertexId, otherId, Direction.BOTH,
                                      null, degree);
    }

    public double jaccardSimilarity(Object vertexId, Object otherId,
                                    Direction direction, String label,
                                    long degree) {
        return this.jaccardSimilarityAPI.get(vertexId, otherId, direction,
                                             label, degree);
    }

    public Map<Object, Double> jaccardSimilarity(SingleSourceJaccardSimilarityRequest request) {
        return this.jaccardSimilarityAPI.post(request);
    }

    public List<Object> sameNeighbors(Object vertexId, Object otherId) {
        return this.sameNeighbors(vertexId, otherId, Traverser.DEFAULT_MAX_DEGREE);
    }

    public List<Object> sameNeighbors(Object vertexId, Object otherId,
                                      long degree) {
        return this.sameNeighbors(vertexId, otherId, Direction.BOTH,
                                  null, degree);
    }

    public List<Object> sameNeighbors(Object vertexId, Object otherId,
                                      Direction direction, String label,
                                      long degree) {
        return this.sameNeighbors(vertexId, otherId, direction, label,
                                  degree, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public List<Object> sameNeighbors(Object vertexId, Object otherId,
                                      Direction direction, String label,
                                      long degree, int limit) {
        return this.sameNeighborsAPI.get(vertexId, otherId, direction,
                                         label, degree, limit);
    }

    public Path shortestPath(Object sourceId, Object targetId, int maxDepth) {
        return this.shortestPath(sourceId, targetId, Direction.BOTH, null,
                                 maxDepth);
    }

    public Path shortestPath(Object sourceId, Object targetId,
                             Direction direction, int maxDepth) {
        return this.shortestPath(sourceId, targetId, direction, null,
                                 maxDepth);
    }

    public Path shortestPath(Object sourceId, Object targetId,
                             Direction direction, String label, int maxDepth) {
        return this.shortestPath(sourceId, targetId, direction, label, maxDepth,
                                 Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY);
    }

    public Path shortestPath(Object sourceId, Object targetId,
                             Direction direction, String label, int maxDepth,
                             long degree, long capacity) {
        return this.shortestPath(sourceId, targetId, direction, label,
                                 maxDepth, degree, 0L, capacity);
    }

    public Path shortestPath(Object sourceId, Object targetId,
                             Direction direction, String label, int maxDepth,
                             long degree, long skipDegree, long capacity) {
        return this.shortestPathAPI.get(sourceId, targetId, direction, label,
                                        maxDepth, degree, skipDegree, capacity);
    }

    public List<Path> allShortestPaths(Object sourceId, Object targetId,
                                       int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, Direction.BOTH,
                                     null, maxDepth);
    }

    public List<Path> allShortestPaths(Object sourceId, Object targetId,
                                       Direction direction, int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, direction,
                                     null, maxDepth);
    }

    public List<Path> allShortestPaths(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth) {
        return this.allShortestPaths(sourceId, targetId, direction,
                                     label, maxDepth, Traverser.DEFAULT_MAX_DEGREE,
                                     Traverser.DEFAULT_CAPACITY);
    }

    public List<Path> allShortestPaths(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth, long degree,
                                       long capacity) {
        return this.allShortestPaths(sourceId, targetId, direction, label,
                                     maxDepth, degree, 0L, capacity);
    }

    public List<Path> allShortestPaths(Object sourceId, Object targetId,
                                       Direction direction, String label,
                                       int maxDepth, long degree,
                                       long skipDegree, long capacity) {
        return this.allShortestPathsAPI.get(sourceId, targetId, direction,
                                            label, maxDepth, degree,
                                            skipDegree, capacity);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  String weight,
                                                  boolean withVertex) {
        return this.singleSourceShortestPath(sourceId, Direction.BOTH, null,
                                             weight, withVertex);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  Direction direction,
                                                  String label, String weight,
                                                  boolean withVertex) {
        return this.singleSourceShortestPath(sourceId, direction, label, weight,
                                             Traverser.DEFAULT_MAX_DEGREE, 0L,
                                             Traverser.DEFAULT_CAPACITY,
                                             Traverser.DEFAULT_PATHS_LIMIT, withVertex);
    }

    public WeightedPaths singleSourceShortestPath(Object sourceId,
                                                  Direction direction,
                                                  String label, String weight,
                                                  long degree, long skipDegree,
                                                  long capacity, int limit,
                                                  boolean withVertex) {
        return this.singleSourceShortestPathAPI.get(sourceId, direction, label,
                                                    weight, degree, skipDegree,
                                                    capacity, limit,
                                                    withVertex);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             String weight, boolean withVertex) {
        return this.weightedShortestPath(sourceId, targetId, Direction.BOTH,
                                         null, weight, withVertex);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             Direction direction, String label,
                                             String weight,
                                             boolean withVertex) {
        return this.weightedShortestPath(sourceId, targetId, direction, label,
                                         weight, Traverser.DEFAULT_MAX_DEGREE, 0L,
                                         Traverser.DEFAULT_CAPACITY, withVertex);
    }

    public WeightedPath weightedShortestPath(Object sourceId, Object targetId,
                                             Direction direction,
                                             String label, String weight,
                                             long degree, long skipDegree,
                                             long capacity, boolean withVertex) {
        return this.weightedShortestPathAPI.get(sourceId, targetId, direction, label, weight,
                                                degree, skipDegree, capacity, withVertex);
    }

    public PathsWithVertices multiNodeShortestPath(MultiNodeShortestPathRequest request) {
        return this.multiNodeShortestPathAPI.post(request);
    }

    public List<Path> paths(Object sourceId, Object targetId, int maxDepth) {
        return this.paths(sourceId, targetId, Direction.BOTH, null,
                          maxDepth, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public List<Path> paths(Object sourceId, Object targetId,
                            Direction direction, int maxDepth, int limit) {
        return this.paths(sourceId, targetId, direction, null,
                          maxDepth, limit);
    }

    public List<Path> paths(Object sourceId, Object targetId,
                            Direction direction, String label,
                            int maxDepth, int limit) {
        return this.paths(sourceId, targetId, direction, label, maxDepth,
                          Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY, limit);
    }

    public List<Path> paths(Object sourceId, Object targetId,
                            Direction direction, String label, int maxDepth,
                            long degree, long capacity, int limit) {
        return this.pathsAPI.get(sourceId, targetId, direction, label,
                                 maxDepth, degree, capacity, limit);
    }

    public PathsWithVertices paths(PathsRequest request) {
        return this.pathsAPI.post(request);
    }

    public List<Path> crosspoint(Object sourceId, Object targetId,
                                 int maxDepth) {
        return this.crosspoint(sourceId, targetId, Direction.BOTH, null,
                               maxDepth, Traverser.DEFAULT_PATHS_LIMIT);
    }

    public List<Path> crosspoint(Object sourceId, Object targetId,
                                 Direction direction, int maxDepth,
                                 int limit) {
        return this.crosspoint(sourceId, targetId, direction, null,
                               maxDepth, limit);
    }

    public List<Path> crosspoint(Object sourceId, Object targetId,
                                 Direction direction, String label,
                                 int maxDepth, int limit) {
        return this.crosspoint(sourceId, targetId, direction, label, maxDepth,
                               Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY, limit);
    }

    public List<Path> crosspoint(Object sourceId, Object targetId,
                                 Direction direction, String label,
                                 int maxDepth, long degree, long capacity,
                                 int limit) {
        return this.crosspointsAPI.get(sourceId, targetId, direction, label,
                                       maxDepth, degree, capacity, limit);
    }

    public List<Object> kout(Object sourceId, int depth) {
        return this.kout(sourceId, Direction.BOTH, depth);
    }

    public List<Object> kout(Object sourceId, Direction direction, int depth) {
        return this.kout(sourceId, direction, null, depth, true);
    }

    public List<Object> kout(Object sourceId, Direction direction,
                             String label, int depth, boolean nearest) {
        return this.kout(sourceId, direction, label, depth, nearest,
                         Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                         Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public List<Object> kout(Object sourceId, Direction direction,
                             String label, int depth, boolean nearest,
                             long degree, long capacity, int limit) {
        return this.koutAPI.get(sourceId, direction, label, depth, nearest,
                                degree, capacity, limit);
    }

    public Kout kout(KoutRequest request) {
        return this.koutAPI.post(request);
    }

    public List<Object> kneighbor(Object sourceId, int depth) {
        return this.kneighbor(sourceId, Direction.BOTH, null, depth);
    }

    public List<Object> kneighbor(Object sourceId, Direction direction,
                                  int depth) {
        return this.kneighbor(sourceId, direction, null, depth);
    }

    public List<Object> kneighbor(Object sourceId, Direction direction,
                                  String label, int depth) {
        return this.kneighbor(sourceId, direction, label, depth,
                              Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public List<Object> kneighbor(Object sourceId, Direction direction,
                                  String label, int depth,
                                  long degree, int limit) {
        return this.kneighborAPI.get(sourceId, direction, label, depth,
                                     degree, limit);
    }

    public Kneighbor kneighbor(KneighborRequest request) {
        return this.kneighborAPI.post(request);
    }

    public long count(CountRequest request) {
        return this.countAPI.post(request);
    }

    public List<Path> rings(Object sourceId, int depth) {
        return this.rings(sourceId, Direction.BOTH, null, depth, true,
                          Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                          Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public List<Path> rings(Object sourceId, Direction direction, String label,
                            int depth, boolean sourceInRing, long degree,
                            long capacity, int limit) {
        return this.ringsAPI.get(sourceId, direction, label, depth,
                                 sourceInRing, degree, capacity, limit);
    }

    public List<Path> rays(Object sourceId, int depth) {
        return this.rays(sourceId, Direction.BOTH, null, depth,
                         Traverser.DEFAULT_MAX_DEGREE, Traverser.DEFAULT_CAPACITY,
                         Traverser.DEFAULT_ELEMENTS_LIMIT);
    }

    public List<Path> rays(Object sourceId, Direction direction, String label,
                           int depth, long degree, long capacity, int limit) {
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

    public List<Ranks> neighborRank(NeighborRankAPI.Request request) {
        return this.neighborRankAPI.post(request);
    }

    public Ranks personalRank(PersonalRankAPI.Request request) {
        return this.personalRankAPI.post(request);
    }

    public List<Shard> vertexShards(long splitSize) {
        return this.verticesAPI.shards(splitSize);
    }

    public List<Shard> edgeShards(long splitSize) {
        return this.edgesAPI.shards(splitSize);
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
}
