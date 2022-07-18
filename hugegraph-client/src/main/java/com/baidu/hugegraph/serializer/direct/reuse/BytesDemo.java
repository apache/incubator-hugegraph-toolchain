package com.baidu.hugegraph.serializer.direct.reuse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.serializer.direct.BinaryEntry;
import com.baidu.hugegraph.serializer.direct.RocksDBSerializer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;

/**
 * @author jin
 * This class is a demo for rocksdb put(rowkey, values) which use Client-Side's graph struct
 * And we don't need to construct the graph element, just use it and transfer them to bytes array
 * instead of json format
 */
public class BytesDemo {

    static HugeClient client;
    boolean bypassServer = true;
    RocksDBSerializer ser;

    public static void main(String[] args) {
        BytesDemo ins = new BytesDemo();
        ins.initGraph();
    }

    void initGraph() {
        // If connect failed will throw an exception.
        client = HugeClient.builder("http://localhost:8080", "hugegraph").build();

        SchemaManager schema = client.schema();

        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asDate().ifNotExist().create();
        schema.propertyKey("price").asInt().ifNotExist().create();

        schema.vertexLabel("person")
              .properties("name", "age")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        schema.vertexLabel("person")
              .properties("price")
              .nullableKeys("price")
              .append();

        schema.vertexLabel("software")
              .properties("name", "lang", "price")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        schema.indexLabel("softwareByPrice")
              .onV("software").by("price")
              .range()
              .ifNotExist()
              .create();

        schema.edgeLabel("knows")
              .link("person", "person")
              .properties("date")
              .ifNotExist()
              .create();

        schema.edgeLabel("created")
              .link("person", "software")
              .properties("date")
              .ifNotExist()
              .create();

        schema.indexLabel("createdByDate")
              .onE("created").by("date")
              .secondary()
              .ifNotExist()
              .create();

        ser = new RocksDBSerializer(client);
        writeGraphElements();

        client.close();
    }

    private void writeGraphElements() {
        GraphManager graph = client.graph();

        // construct some vertexes & edges
        Vertex marko = new Vertex("person").property("name", "marko").property("age", 29);
        Vertex vadas = new Vertex("person").property("name", "vadas").property("age", 27);
        Vertex lop = new Vertex("software").property("name", "lop").property("lang", "java")
                                                                        .property("price", 328);
        Vertex josh = new Vertex("person").property("name", "josh").property("age", 32);
        Vertex ripple = new Vertex("software").property("name", "ripple")
                                                   .property("lang", "java")
                                                   .property("price", 199);
        Vertex peter = new Vertex("person").property("name", "peter").property("age", 35);

        Edge markoKnowsVadas = new Edge("knows").source(marko).target(vadas)
                                                .property("date", "2016-01-10");
        Edge markoKnowsJosh = new Edge("knows").source(marko).target(josh)
                                               .property("date", "2013-02-20");
        Edge markoCreateLop = new Edge("created").source(marko).target(lop)
                                                 .property("date", "2017-12-10");
        Edge joshCreateRipple = new Edge("created").source(josh).target(ripple)
                                                        .property("date", "2017-12-10");
        Edge joshCreateLop = new Edge("created").source(josh).target(lop)
                                                                  .property("date", "2009-11-11");
        Edge peterCreateLop = new Edge("created").source(peter).target(lop)
                                                                    .property("date", "2017-03-24");

        List<Vertex> vertices = new ArrayList<Vertex>(){{
            add(marko);add(vadas);add(lop);add(josh);add(ripple);add(peter);
        }};


        List<Edge> edges = new ArrayList<Edge>(){{
            add(markoKnowsVadas);add(markoKnowsJosh);add(markoCreateLop);add(joshCreateRipple);
            add(joshCreateLop);add(peterCreateLop);
        }};

        // Old way: encode to json then send to server
        if (bypassServer) {
            writeDirectly(vertices, edges);
        } else {
            writeByServer(graph, vertices, edges);
        }
    }

    /* we transfer the vertex & edge into bytes array
     * TODO: use a batch and send them together
     * */
    void writeDirectly(List<Vertex> vertices, List<Edge> edges) {
        for (Vertex vertex : vertices) {
            BinaryEntry entry = ser.writeVertex(vertex);
            byte[] rowkey = getKeyBytes(vertex);
            byte[] values = getValueBytes(vertex);
            sendRpcToRocksDB(rowkey, values);
        }

        for (Edge edge: edges) {
            byte[] rowkey = getKeyBytes(edge);
            byte[] values = getValueBytes(edge);
            sendRpcToRocksDB(rowkey, values);
        }
    }

    byte[] getKeyBytes(GraphElement e) {
        Object id = e.id();
        String type = e.type();
        return id2Bytes(id, type);
    }

    byte[] id2Bytes(Object id, String type) {
        byte[] res = null;

        if ("vertex".equals(type)) {
            ser.writeVertex()
        } else if ("edge".equals(type)) {

        }

        return res;
    }

    byte[] getValueBytes(GraphElement e) {
        Map<String, Object> properties = e.properties();
        return propertyToBytes(properties);
    }

    byte[] propertyToBytes(Map<String, Object> properties) {
        byte[] res = null;

        return res;
    }

    boolean sendRpcToRocksDB(byte[] rowkey, byte[] values) {
        // here we call the rpc
        boolean flag = false;
        //flag = put(rowkey, values);
        return flag;
    }

    void writeByServer(GraphManager graph, List<Vertex> vertices, List<Edge> edges) {
        vertices = graph.addVertices(vertices);
        vertices.forEach(System.out::println);

        edges = graph.addEdges(edges, false);
        edges.forEach(System.out::println);
    }

}
