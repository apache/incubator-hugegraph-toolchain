package com.baidu.hugegraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.GraphsManager;
import com.baidu.hugegraph.driver.GremlinManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.driver.TaskManager;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.google.common.collect.ImmutableMap;

public class BaseClientTest {

    protected static String BASE_URL = "http://127.0.0.1:8080";
    protected static String GRAPH = "hugegraph";

    private static HugeClient client;

    protected static HugeClient open() {
        return new HugeClient(BASE_URL, GRAPH);
    }

    @BeforeClass
    public static void init() {
        client = open();
    }

    @AfterClass
    public static void clear() throws Exception {
        // client.close();
    }

    public static SchemaManager schema() {
        Assert.assertNotNull("Not opened client", client);
        return client.schema();
    }

    public static GraphManager graph() {
        Assert.assertNotNull("Not opened client", client);
        return client.graph();
    }

    public static GremlinManager gremlin() {
        Assert.assertNotNull("Not opened client", client);
        return client.gremlin();
    }

    public static TaskManager task() {
        Assert.assertNotNull("Not opened client", client);
        return client.task();
    }

    public static GraphsManager graphs() {
        Assert.assertNotNull("Not opened client", client);
        return client.graphs();
    }

    @Before
    public void setup() {
        // this.clearData();
    }

    @After
    public void teardown() throws Exception {
        // pass
    }

    protected static Object getVertexId(String label, String key, String value) {
        Map<String, Object> params = ImmutableMap.of(key, value);
        List<Vertex> vertices = graph().listVertices(label, params);
        assert vertices.size() == 1;
        return vertices.get(0).id();
    }

    protected static String getEdgeId(String label, String key, String value) {
        Map<String, Object> params = ImmutableMap.of(key, value);
        List<Edge> edges = graph().listEdges(label, params);
        assert edges.size() == 1;
        return edges.get(0).id();
    }

    protected static void initPropertyKey() {
        SchemaManager schema = schema();
        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("city").asText().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asText().ifNotExist().create();
        schema.propertyKey("price").asInt().ifNotExist().create();
        schema.propertyKey("weight").asDouble().ifNotExist().create();
    }

    protected static void initVertexLabel() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.vertexLabel("software")
              .properties("name", "lang", "price")
              .primaryKeys("name")
              .nullableKeys("price")
              .ifNotExist()
              .create();

        schema.vertexLabel("book")
              .useCustomizeStringId()
              .properties("name", "price")
              .nullableKeys("price")
              .ifNotExist()
              .create();

        schema.vertexLabel("log")
              .useCustomizeNumberId()
              .properties("date")
              .ifNotExist()
              .create();
    }

    protected static void initEdgeLabel() {
        SchemaManager schema = schema();

        schema.edgeLabel("knows")
              .sourceLabel("person")
              .targetLabel("person")
              .properties("date", "city")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.edgeLabel("created")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "city")
              .nullableKeys("city")
              .ifNotExist()
              .create();
    }

    protected static void initIndexLabel() {
        SchemaManager schema = schema();

        schema.indexLabel("personByCity")
              .onV("person")
              .by("city")
              .secondary()
              .ifNotExist()
              .create();

        schema.indexLabel("personByAge")
              .onV("person")
              .by("age")
              .range()
              .ifNotExist()
              .create();

        schema.indexLabel("knowsByDate")
              .onE("knows")
              .by("date")
              .secondary()
              .ifNotExist()
              .create();

        schema.indexLabel("createdByDate")
              .onE("created")
              .by("date")
              .secondary()
              .ifNotExist()
              .create();
    }

    protected static void initVertex() {
        graph().addVertex(T.label, "person", "name", "marko",
                          "age", 29, "city", "Beijing");
        graph().addVertex(T.label, "person", "name", "vadas",
                          "age", 27, "city", "Hongkong");
        graph().addVertex(T.label, "software", "name", "lop",
                          "lang", "java", "price", 328);
        graph().addVertex(T.label, "person", "name", "josh",
                          "age", 32, "city", "Beijing");
        graph().addVertex(T.label, "software", "name", "ripple",
                          "lang", "java", "price", 199);
        graph().addVertex(T.label, "person", "name", "peter",
                          "age", 29, "city", "Shanghai");
    }

    protected static void initEdge() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        graph().addEdge(markoId, "knows", vadasId, "date", "20120110");
        graph().addEdge(markoId, "knows", joshId, "date", "20130110");
        graph().addEdge(markoId, "created", lopId,
                        "date", "20140110", "city", "Shanghai");
        graph().addEdge(joshId, "created", rippleId,
                        "date", "20150110", "city", "Beijing");
        graph().addEdge(joshId, "created", lopId,
                        "date", "20160110", "city", "Beijing");
        graph().addEdge(peterId, "created", lopId,
                        "date", "20170110", "city", "Hongkong");
    }

    protected List<Vertex> create100PersonBatch() {
        List<Vertex> vertices = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Vertex vertex = new Vertex("person");
            vertex.property("name", "Person" + "-" + i);
            vertex.property("city", "Beijing");
            vertex.property("age", 30);
            vertices.add(vertex);
        }
        return vertices;
    }

    protected List<Vertex> create50SoftwareBatch() {
        List<Vertex> vertices = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Vertex vertex = new Vertex("software");
            vertex.property("name", "Software" + "-" + i);
            vertex.property("lang", "java");
            vertex.property("price", 328);
            vertices.add(vertex);
        }
        return vertices;
    }

    protected List<Edge> create50CreatedBatch() {
        VertexLabel person = schema().getVertexLabel("person");
        VertexLabel software = schema().getVertexLabel("software");

        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("created");
            edge.sourceLabel("person");
            edge.targetLabel("software");
            edge.sourceId(person.id() + ":Person-" + i);
            edge.targetId(software.id() + ":Software-" + i);
            edge.property("date", "20170324");
            edge.property("city", "Hongkong");
            edges.add(edge);
        }
        return edges;
    }

    protected List<Edge> create50KnowsBatch() {
        VertexLabel person = schema().getVertexLabel("person");

        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("knows");
            edge.sourceLabel("person");
            edge.targetLabel("person");
            edge.sourceId(person.id() + ":Person-" + i);
            edge.targetId(person.id() + ":Person-" + (i + 50));
            edge.property("date", "20170324");
            edges.add(edge);
        }
        return edges;
    }
}
