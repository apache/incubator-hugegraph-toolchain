package com.baidu.hugegraph.structure.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.driver.GraphManager;

/**
 * HugeGraph is a mirror of server-side data(vertex/edge), it used to speed up
 * data access. Note, however, the memory can't hold large amounts of data.
 */
public class Graph {

    private static final Logger logger =
                         LoggerFactory.getLogger(Graph.class);

    private Map<String, HugeVertex> hugeVerticesMap;
    private List<HugeEdge> hugeEdges;

    public Graph(GraphManager graph) {
        logger.debug("Loding Graph...");

        List<Vertex> vertices = graph.getVertices();
        logger.debug("Loded vertices: {}", vertices.size());

        List<Edge> edges = graph.getEdges();
        logger.debug("Loded edges: {}", edges.size());

        this.mergeEdges2Vertices(vertices, edges);
        logger.debug("Loded Graph");
    }

    public Graph(List<Vertex> vertices, List<Edge> edges) {
        this.mergeEdges2Vertices(vertices, edges);
    }

    public Iterator<HugeVertex> vertices() {
        return this.hugeVerticesMap.values().iterator();
    }

    public HugeVertex vertex(String id) {
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
            HugeVertex src = this.hugeVerticesMap.get(e.source());
            HugeVertex tgt = this.hugeVerticesMap.get(e.target());

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
            return String.format("HugeEdge{edge=%s, source=%s, target=%s}",
                                 this.edge,
                                 this.source.vertex(),
                                 this.target.vertex());
        }
    }
}
