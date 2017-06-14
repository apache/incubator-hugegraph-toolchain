package com.baidu.hugegraph;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.GremlinManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class Example {

    public static void main(String[] args) {
        // If connect failed will throw a exception.
        HugeClient hugeClient = HugeClient.open("http://localhost:8080",
                "hugegraph");

        SchemaManager schema = hugeClient.schema();

        schema.makePropertyKey("name").asText().ifNotExist().create();
        schema.makePropertyKey("age").asInt().ifNotExist().create();
        schema.makePropertyKey("lang").asText().ifNotExist().create();
        schema.makePropertyKey("date").asText().ifNotExist().create();
        schema.makePropertyKey("price").asInt().ifNotExist().create();

        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age")
                .primaryKeys("name")
                .ifNotExist()
                .create();

        schema.makeVertexLabel("person")
                .properties("price")
                .append();

        VertexLabel software = schema.makeVertexLabel("software")
                .properties("name", "lang", "price")
                .primaryKeys("name")
                .ifNotExist()
                .create();

        schema.makeIndexLabel("personByName")
                .on(person).by("name")
                .secondary()
                .ifNotExist()
                .create();

        schema.makeIndexLabel("softwareByPrice")
                .on(software).by("price")
                .search()
                .ifNotExist()
                .create();

        EdgeLabel knows = schema.makeEdgeLabel("knows")
                .link("person", "person")
                .properties("date")
                .ifNotExist()
                .create();

        schema.makeEdgeLabel("knows")
                .link("software", "person")
                .properties("price")
                .append();

        EdgeLabel created = schema.makeEdgeLabel("created")
                .link("person", "software")
                .properties("date")
                .ifNotExist()
                .create();

        schema.makeIndexLabel("createdByDate")
                .on(created).by("date")
                .secondary()
                .ifNotExist()
                .create();

        // get schema object by name
        System.out.println(schema.getPropertyKey("name"));
        System.out.println(schema.getVertexLabel("person"));
        System.out.println(schema.getEdgeLabel("knows"));
        System.out.println(schema.getIndexLabel("createdByDate"));

        // list all schema objects
        System.out.println(schema.getPropertyKeys());
        System.out.println(schema.getVertexLabels());
        System.out.println(schema.getEdgeLabels());
        System.out.println(schema.getIndexLabels());

//        schema.removePropertyKey("name");
//        schema.removeVertexLabel("person");
//        schema.removeEdgeLabel("knows");
//        schema.removeIndexLabel("createdByDate");

        GraphManager graph = hugeClient.graph();

        Vertex marko = graph.addVertex(T.label, "person",
                "name", "marko", "age", 29);
        Vertex vadas = graph.addVertex(T.label, "person",
                "name", "vadas", "age", 27);
        Vertex lop = graph.addVertex(T.label, "software",
                "name", "lop", "lang", "java", "price", 328);
        Vertex josh = graph.addVertex(T.label, "person",
                "name", "josh", "age", 32);
        Vertex ripple = graph.addVertex(T.label, "software",
                "name", "ripple", "lang", "java", "price", 199);
        Vertex peter = graph.addVertex(T.label, "person",
                "name", "peter", "age", 35);

        Edge knows1 = marko.addEdge("knows", vadas);
        System.out.println(knows1);
        Edge knows2 = marko.addEdge("knows", josh);
        System.out.println(knows2);

        marko.addEdge("knows", vadas, "date", "20160110");
        marko.addEdge("knows", josh, "date", "20130220");
        marko.addEdge("created", lop, "date", "20171210");
        josh.addEdge("created", ripple, "date", "20171210");
        josh.addEdge("created", lop, "date", "20091111");
        peter.addEdge("created", lop, "date", "20170324");

        GremlinManager gremlin = hugeClient.gremlin();
        String result = gremlin.gremlin("g.V()")
                .language("gremlin-groovy").execute();

        System.out.println(result);

    }

}
