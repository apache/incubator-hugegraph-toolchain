package com.baidu.hugegraph;

import com.baidu.hugegraph.impl.HugeGraph;

/**
 * Hello world!
 */
public class Example {
    public static void main(String[] args) {
        HugeGraph graph = HugeGraph.open("http://localhost:8080", "huge");
        graph.gremlin().execute("g.V()");
        graph.schema().getPropertyKeys();
        graph.graph().getVertices();

    }
}
