/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.example;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.GremlinManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.gremlin.Result;
import com.baidu.hugegraph.structure.gremlin.ResultSet;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class SingleExample {

    public static void main(String[] args) throws IOException {
        // If connect failed will throw a exception.
        HugeClient hugeClient = HugeClient.open("http://localhost:8080",
                "hugegraph");

        SchemaManager schema = hugeClient.schema();

        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asText().ifNotExist().create();
        schema.propertyKey("price").asInt().ifNotExist().create();

        VertexLabel person = schema.vertexLabel("person")
                .useAutomaticId()
                .properties("name", "age")
//                .primaryKeys("name")
                .ifNotExist()
                .create();

        schema.vertexLabel("person").properties("price").append();

        VertexLabel software = schema.vertexLabel("software")
                .useCustomizeId()
                .properties("name", "lang", "price")
//                .primaryKeys("name")
                .ifNotExist()
                .create();

        schema.indexLabel("personByName")
                .onV("person").by("name")
                .secondary()
                .ifNotExist()
                .create();

        schema.indexLabel("softwareByPrice")
                .onV("software").by("price")
                .search()
                .ifNotExist()
                .create();

        EdgeLabel knows = schema.edgeLabel("knows")
                .link("person", "person")
                .properties("date")
                .ifNotExist()
                .create();

        EdgeLabel created = schema.edgeLabel("created")
                .link("person", "software")
                .properties("date")
                .ifNotExist()
                .create();

        schema.indexLabel("createdByDate")
                .onE("created").by("date")
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
        Vertex lop = graph.addVertex(T.label, "software", T.id, "software-lop",
                "name", "lop", "lang", "java", "price", 328);
        Vertex josh = graph.addVertex(T.label, "person",
                "name", "josh", "age", 32);
        Vertex ripple = graph.addVertex(T.label, "software", T.id, "123456",
                "name", "ripple", "lang", "java", "price", 199);
        Vertex peter = graph.addVertex(T.label, "person",
                "name", "peter", "age", 35);

        marko.addEdge("knows", vadas, "date", "20160110");
        marko.addEdge("knows", josh, "date", "20130220");
        marko.addEdge("created", lop, "date", "20171210");
        josh.addEdge("created", ripple, "date", "20171210");
        josh.addEdge("created", lop, "date", "20091111");
        peter.addEdge("created", lop, "date", "20170324");

        GremlinManager gremlin = hugeClient.gremlin();
        System.out.println("==== Vertex ====");
        ResultSet resultSet = gremlin.gremlin("g.V().outE().path()").execute();
        Iterator<Result> results = resultSet.iterator();
        results.forEachRemaining(result -> {
            System.out.println(result.getObject().getClass());
            Object object = result.getObject();
            if (object instanceof Vertex) {
                System.out.println(((Vertex) object).id());
            } else if (object instanceof Edge) {
                System.out.println(((Edge) object).id());
            } else if (object instanceof Path) {
                List<GraphElement> elements = ((Path) object).objects();
                elements.stream().forEach(element -> {
                    System.out.println(element.getClass());
                    System.out.println(element);
                });
            } else {
                System.out.println(object);
            }
        });
    }

}
