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

package org.apache.hugegraph.structure.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.util.Log;

/**
 * HugeGraph is a mirror of server-side data(vertex/edge), it used to speed up
 * data access. Note, however, the memory can't hold large amounts of data.
 */
public class Graph {

    private static final Logger LOG = Log.logger(Graph.class);

    private Map<Object, HugeVertex> hugeVerticesMap;
    private List<HugeEdge> hugeEdges;

    public Graph(GraphManager graph) {
        LOG.debug("Loading Graph...");

        List<Vertex> vertices = graph.listVertices();
        LOG.debug("Loaded vertices: {}", vertices.size());

        List<Edge> edges = graph.listEdges();
        LOG.debug("Loaded edges: {}", edges.size());

        this.mergeEdges2Vertices(vertices, edges);
        LOG.debug("Loaded Graph");
    }

    public Graph(List<Vertex> vertices, List<Edge> edges) {
        this.mergeEdges2Vertices(vertices, edges);
    }

    public Iterator<HugeVertex> vertices() {
        return this.hugeVerticesMap.values().iterator();
    }

    public HugeVertex vertex(Object id) {
        return this.hugeVerticesMap.get(id);
    }

    public Iterator<HugeEdge> edges() {
        return this.hugeEdges.iterator();
    }

    private void mergeEdges2Vertices(List<Vertex> vertices,
                                     List<Edge> edges) {
        this.hugeVerticesMap = new HashMap<>(vertices.size());
        for (Vertex v : vertices) {
            this.hugeVerticesMap.put(v.id(), new HugeVertex(v));
        }

        this.hugeEdges = new ArrayList<>(edges.size());
        for (Edge e : edges) {
            HugeVertex src = this.hugeVerticesMap.get(e.sourceId());
            HugeVertex tgt = this.hugeVerticesMap.get(e.targetId());

            HugeEdge edge = new HugeEdge(e);
            edge.source(src);
            edge.target(tgt);

            src.addEdge(edge);
            tgt.addEdge(edge);

            this.hugeEdges.add(edge);
        }
    }

    public static class HugeVertex {

        private Vertex vertex;
        private List<HugeEdge> edges;

        public HugeVertex(Vertex v) {
            this.vertex = v;
            this.edges = new ArrayList<>();
        }

        public Vertex vertex() {
            return this.vertex;
        }

        public void vertex(Vertex vertex) {
            this.vertex = vertex;
        }

        public void addEdge(HugeEdge e) {
            this.edges.add(e);
        }

        public List<HugeEdge> getEdges() {
            return this.edges;
        }

        @Override
        public String toString() {
            return String.format("HugeVertex{vertex=%s, edges=%s}",
                                 this.vertex, this.edges);
        }
    }

    public static class HugeEdge {

        private Edge edge;
        private HugeVertex source;
        private HugeVertex target;

        public HugeEdge(Edge e) {
            this.edge = e;
        }

        public Edge edge() {
            return this.edge;
        }

        public HugeVertex source() {
            return this.source;
        }

        public void source(HugeVertex source) {
            this.source = source;
        }

        public HugeVertex target() {
            return this.target;
        }

        public void target(HugeVertex target) {
            this.target = target;
        }

        public HugeVertex other(HugeVertex vertex) {
            return vertex == this.source ? this.target : this.source;
        }

        @Override
        public String toString() {
            return String.format("HugeEdge{edge=%s, sourceId=%s, targetId=%s}",
                                 this.edge,
                                 this.source.vertex(),
                                 this.target.vertex());
        }
    }
}
