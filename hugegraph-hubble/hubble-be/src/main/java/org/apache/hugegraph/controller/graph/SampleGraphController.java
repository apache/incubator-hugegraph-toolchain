/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
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

package org.apache.hugegraph.controller.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.Frequency;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.Ex;

@RestController
@Log4j2
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs/{graph}/sample")
public class SampleGraphController extends BaseController {

    private static final String IDEMPOTENT_TRAVERSAL_FALLBACK_MARKER =
            "// hugegraph-client:idempotent-traversal-fallback\n";
    public static final String LOADER_SOURCE = "hugegraph-loader/example/file";
    public static final String RANK_SOURCE =
            "hugegraph-doc/rank-api/neighbor-rank-example";
    public static final String HLM_SOURCE =
            "hubble-be/src/main/resources/demo/hlm.txt";

    public static final String LOADER_SCHEMA =
            "schema = graph.schema();\n" +
            "schema.propertyKey('name').asText().ifNotExist().create();\n" +
            "schema.propertyKey('age').asInt().ifNotExist().create();\n" +
            "schema.propertyKey('city').asText().ifNotExist().create();\n" +
            "schema.propertyKey('weight').asDouble().ifNotExist().create();\n" +
            "schema.propertyKey('lang').asText().ifNotExist().create();\n" +
            "schema.propertyKey('date').asText().ifNotExist().create();\n" +
            "schema.propertyKey('price').asDouble().ifNotExist().create();\n" +
            "schema.vertexLabel('person').properties('name', 'age', 'city')" +
            ".primaryKeys('name').nullableKeys('age', 'city')" +
            ".ifNotExist().create();\n" +
            "schema.vertexLabel('software').useCustomizeNumberId()" +
            ".properties('name', 'lang', 'price').ifNotExist().create();\n" +
            "schema.edgeLabel('knows').sourceLabel('person')" +
            ".targetLabel('person').properties('date', 'weight')" +
            ".ifNotExist().create();\n" +
            "schema.edgeLabel('created').sourceLabel('person')" +
            ".targetLabel('software').properties('date', 'weight')" +
            ".ifNotExist().create();";

    public static final String LOADER_DATA =
            vertex("marko", "marko", 29, "Beijing") +
            vertex("vadas", "vadas", 27, "Hongkong") +
            vertex("josh", "josh", 32, "Beijing") +
            vertex("peter", "peter", 35, "Shanghai") +
            vertex("linary", "li,nary", 26, "Wu,han") +
            nullableVertex("tom", "tom") +
            software(1, "lop", 328) + software(2, "ripple", 199) +
            edge("marko", "knows", "vadas", "20160110", 0.5) +
            edge("marko", "knows", "josh", "20130220", 1.0) +
            created("marko", 1, "2017-12-10", 0.4) +
            created("josh", 1, "2009-11-11", 0.4) +
            created("josh", 2, "2017-12-10", 1.0) +
            created("peter", 1, "2017-03-24", 0.2);

    public static final String RANK_SCHEMA =
            "schema = graph.schema();\n" +
            "schema.propertyKey('name').asText().ifNotExist().create();\n" +
            "schema.vertexLabel('person').properties('name')" +
            ".useCustomizeStringId().ifNotExist().create();\n" +
            "schema.vertexLabel('movie').properties('name')" +
            ".useCustomizeStringId().ifNotExist().create();\n" +
            "schema.edgeLabel('follow').sourceLabel('person')" +
            ".targetLabel('person').ifNotExist().create();\n" +
            "schema.edgeLabel('like').sourceLabel('person')" +
            ".targetLabel('movie').ifNotExist().create();\n" +
            "schema.edgeLabel('directedBy').sourceLabel('movie')" +
            ".targetLabel('person').ifNotExist().create();";

    public static final String RANK_DATA =
            rankVertex("O", "person") + rankVertex("A", "person") +
            rankVertex("B", "person") + rankVertex("C", "person") +
            rankVertex("D", "person") + rankVertex("E", "movie") +
            rankVertex("F", "movie") + rankVertex("G", "movie") +
            rankVertex("H", "movie") + rankVertex("I", "movie") +
            rankVertex("J", "movie") + rankVertex("K", "person") +
            rankVertex("L", "person") + rankVertex("M", "person") +
            rankEdge("O", "follow", "A") + rankEdge("O", "follow", "B") +
            rankEdge("O", "follow", "C") + rankEdge("D", "follow", "O") +
            rankEdge("A", "follow", "B") + rankEdge("A", "like", "E") +
            rankEdge("A", "like", "F") + rankEdge("B", "like", "G") +
            rankEdge("B", "like", "H") + rankEdge("C", "like", "I") +
            rankEdge("C", "like", "J") +
            rankEdge("E", "directedBy", "K") +
            rankEdge("F", "directedBy", "B") +
            rankEdge("F", "directedBy", "L") +
            rankEdge("G", "directedBy", "M");

    public static final String HLM_SCHEMA =
            "schema = graph.schema();\n" +
            "schema.propertyKey('name').asText().ifNotExist().create();\n" +
            "schema.propertyKey('gender').asText().ifNotExist().create();\n" +
            "schema.propertyKey('age').asInt().ifNotExist().create();\n" +
            "schema.propertyKey('title').asText().ifNotExist().create();\n" +
            "schema.propertyKey('feature').asText().ifNotExist().create();\n" +
            "schema.propertyKey('intimacy').asText().ifNotExist().create();\n" +
            "schema.vertexLabel('人物').properties('name','gender','age'," +
            "'title','feature').primaryKeys('name')" +
            ".ifNotExist().create();\n" +
            "schema.edgeLabel('关系').sourceLabel('人物')" +
            ".targetLabel('人物').properties('intimacy')" +
            ".ifNotExist().create();";

    @PostMapping
    public Map<String, Object> load(@PathVariable("graphspace") String graphSpace,
                                    @PathVariable("graph") String graph,
                                    @RequestParam(name = "dataset",
                                                  defaultValue = "loader")
                                    String dataset) {
        boolean loader = "loader".equals(dataset);
        boolean rank = "rank".equals(dataset);
        boolean hlm = "hlm".equals(dataset);
        Ex.check(loader || rank || hlm, "common.param.should-belong-to",
                 "dataset", "[loader, hlm, rank]");
        HugeClient client = this.authGremlinClient(graphSpace, graph);
        SchemaManager schema = client.schema();
        validateSchemaCompatibility(schema, dataset);
        try {
            createSchema(schema, dataset);
            String script = IDEMPOTENT_TRAVERSAL_FALLBACK_MARKER +
                            data(dataset);
            client.gremlin().gremlin(script).execute();
        } catch (RuntimeException e) {
            log.warn("Failed to load sample dataset '{}' into {}/{}",
                     dataset, graphSpace, graph, e);
            throw new ExternalException("graph.sample.load-failed",
                                        dataset, graphSpace, graph);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataset", dataset);
        result.put("source", source(dataset));
        result.put("graphspace", graphSpace);
        result.put("graph", graph);
        result.put("vertices", hlm ? 14 : rank ? 14 : 8);
        result.put("edges", hlm ? 15 : rank ? 15 : 6);
        result.put("idempotent", true);
        result.put("clears_existing_data", false);
        return result;
    }

    private static void createSchema(SchemaManager schema, String dataset) {
        Set<String> propertyKeys = new HashSet<>();
        schema.getPropertyKeys().forEach(key -> propertyKeys.add(key.name()));
        Set<String> vertexLabels = new HashSet<>();
        schema.getVertexLabels().forEach(label -> vertexLabels.add(label.name()));
        Set<String> edgeLabels = new HashSet<>();
        schema.getEdgeLabels().forEach(label -> edgeLabels.add(label.name()));
        if ("loader".equals(dataset)) {
            createLoaderSchema(schema, propertyKeys, vertexLabels, edgeLabels);
        } else if ("rank".equals(dataset)) {
            createRankSchema(schema, propertyKeys, vertexLabels, edgeLabels);
        } else {
            createHlmSchema(schema, propertyKeys, vertexLabels, edgeLabels);
        }
    }

    private static void validateSchemaCompatibility(SchemaManager schema,
                                                    String dataset) {
        Map<String, PropertyKey> propertyKeys = new HashMap<>();
        schema.getPropertyKeys().forEach(key -> propertyKeys.put(key.name(), key));
        Map<String, VertexLabel> vertexLabels = new HashMap<>();
        schema.getVertexLabels().forEach(label ->
                vertexLabels.put(label.name(), label));
        Map<String, EdgeLabel> edgeLabels = new HashMap<>();
        schema.getEdgeLabels().forEach(label ->
                edgeLabels.put(label.name(), label));

        if ("loader".equals(dataset)) {
            validateLoaderSchema(dataset, propertyKeys, vertexLabels, edgeLabels);
        } else if ("rank".equals(dataset)) {
            validateRankSchema(dataset, propertyKeys, vertexLabels, edgeLabels);
        } else {
            validateHlmSchema(dataset, propertyKeys, vertexLabels, edgeLabels);
        }
    }

    private static void validateLoaderSchema(
            String dataset, Map<String, PropertyKey> propertyKeys,
            Map<String, VertexLabel> vertexLabels,
            Map<String, EdgeLabel> edgeLabels) {
        requireProperty(dataset, propertyKeys, "name", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "age", DataType.INT);
        requireProperty(dataset, propertyKeys, "city", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "weight", DataType.DOUBLE);
        requireProperty(dataset, propertyKeys, "lang", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "date", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "price", DataType.DOUBLE);
        requireVertex(dataset, vertexLabels, "person", IdStrategy.PRIMARY_KEY,
                      names("name", "age", "city"),
                      Collections.singletonList("name"),
                      names("age", "city"));
        requireVertex(dataset, vertexLabels, "software",
                      IdStrategy.CUSTOMIZE_NUMBER,
                      names("name", "lang", "price"),
                      Collections.emptyList(), Collections.emptySet());
        requireEdge(dataset, edgeLabels, "knows", "person", "person",
                    names("date", "weight"));
        requireEdge(dataset, edgeLabels, "created", "person", "software",
                    names("date", "weight"));
    }

    private static void validateRankSchema(
            String dataset, Map<String, PropertyKey> propertyKeys,
            Map<String, VertexLabel> vertexLabels,
            Map<String, EdgeLabel> edgeLabels) {
        requireProperty(dataset, propertyKeys, "name", DataType.TEXT);
        requireVertex(dataset, vertexLabels, "person",
                      IdStrategy.CUSTOMIZE_STRING,
                      names("name"), Collections.emptyList(),
                      Collections.emptySet());
        requireVertex(dataset, vertexLabels, "movie",
                      IdStrategy.CUSTOMIZE_STRING,
                      names("name"), Collections.emptyList(),
                      Collections.emptySet());
        requireEdge(dataset, edgeLabels, "follow", "person", "person",
                    Collections.emptySet());
        requireEdge(dataset, edgeLabels, "like", "person", "movie",
                    Collections.emptySet());
        requireEdge(dataset, edgeLabels, "directedBy", "movie", "person",
                    Collections.emptySet());
    }

    private static void validateHlmSchema(
            String dataset, Map<String, PropertyKey> propertyKeys,
            Map<String, VertexLabel> vertexLabels,
            Map<String, EdgeLabel> edgeLabels) {
        requireProperty(dataset, propertyKeys, "name", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "gender", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "age", DataType.INT);
        requireProperty(dataset, propertyKeys, "title", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "feature", DataType.TEXT);
        requireProperty(dataset, propertyKeys, "intimacy", DataType.TEXT);
        requireVertex(dataset, vertexLabels, "人物", IdStrategy.PRIMARY_KEY,
                      names("name", "gender", "age", "title", "feature"),
                      Collections.singletonList("name"),
                      Collections.emptySet());
        requireEdge(dataset, edgeLabels, "关系", "人物", "人物",
                    names("intimacy"));
    }

    private static void requireProperty(String dataset,
                                        Map<String, PropertyKey> existing,
                                        String name, DataType dataType) {
        PropertyKey propertyKey = existing.get(name);
        if (propertyKey != null &&
            (propertyKey.dataType() != dataType ||
             propertyKey.cardinality() != Cardinality.SINGLE)) {
            incompatible(dataset, "property key", name);
        }
    }

    private static void requireVertex(String dataset,
                                      Map<String, VertexLabel> existing,
                                      String name, IdStrategy idStrategy,
                                      Set<String> properties,
                                      List<String> primaryKeys,
                                      Set<String> nullableKeys) {
        VertexLabel vertexLabel = existing.get(name);
        if (vertexLabel != null &&
            (vertexLabel.idStrategy() != idStrategy ||
             !vertexLabel.properties().equals(properties) ||
             !vertexLabel.primaryKeys().equals(primaryKeys) ||
             !vertexLabel.nullableKeys().containsAll(nullableKeys) ||
             !hasDefaultTtl(vertexLabel.ttl(), vertexLabel.ttlStartTime()))) {
            incompatible(dataset, "vertex label", name);
        }
    }

    private static void requireEdge(String dataset,
                                    Map<String, EdgeLabel> existing,
                                    String name, String source, String target,
                                    Set<String> properties) {
        EdgeLabel edgeLabel = existing.get(name);
        Map<String, String> expectedLink = new HashMap<>();
        expectedLink.put(source, target);
        if (edgeLabel != null &&
            (!edgeLabel.properties().equals(properties) ||
             edgeLabel.links().size() != 1 ||
             !edgeLabel.links().contains(expectedLink) ||
             edgeLabel.frequency() == Frequency.MULTIPLE ||
             !edgeLabel.sortKeys().isEmpty() ||
             !hasDefaultTtl(edgeLabel.ttl(), edgeLabel.ttlStartTime()))) {
            incompatible(dataset, "edge label", name);
        }
    }

    private static boolean hasDefaultTtl(long ttl, String ttlStartTime) {
        return ttl == 0L && ttlStartTime == null;
    }

    private static Set<String> names(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static void incompatible(String dataset, String type, String name) {
        throw new ExternalException("graph.sample.schema-incompatible",
                                    dataset, type, name);
    }

    private static void createLoaderSchema(SchemaManager schema,
                                           Set<String> propertyKeys,
                                           Set<String> vertexLabels,
                                           Set<String> edgeLabels) {
        createIfMissing(propertyKeys, "name", () ->
                schema.propertyKey("name").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "age", () ->
                schema.propertyKey("age").asInt().ifNotExist().create());
        createIfMissing(propertyKeys, "city", () ->
                schema.propertyKey("city").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "weight", () ->
                schema.propertyKey("weight").asDouble().ifNotExist().create());
        createIfMissing(propertyKeys, "lang", () ->
                schema.propertyKey("lang").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "date", () ->
                schema.propertyKey("date").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "price", () ->
                schema.propertyKey("price").asDouble().ifNotExist().create());
        createIfMissing(vertexLabels, "person", () ->
                schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("age", "city")
              .ifNotExist().create());
        createIfMissing(vertexLabels, "software", () ->
                schema.vertexLabel("software")
              .useCustomizeNumberId()
              .properties("name", "lang", "price")
              .ifNotExist().create());
        createIfMissing(edgeLabels, "knows", () ->
                schema.edgeLabel("knows")
              .sourceLabel("person").targetLabel("person")
              .properties("date", "weight")
              .ifNotExist().create());
        createIfMissing(edgeLabels, "created", () ->
                schema.edgeLabel("created")
              .sourceLabel("person").targetLabel("software")
              .properties("date", "weight")
              .ifNotExist().create());
    }

    private static void createRankSchema(SchemaManager schema,
                                         Set<String> propertyKeys,
                                         Set<String> vertexLabels,
                                         Set<String> edgeLabels) {
        createIfMissing(propertyKeys, "name", () ->
                schema.propertyKey("name").asText().ifNotExist().create());
        createIfMissing(vertexLabels, "person", () ->
                schema.vertexLabel("person")
              .properties("name").useCustomizeStringId()
              .ifNotExist().create());
        createIfMissing(vertexLabels, "movie", () ->
                schema.vertexLabel("movie")
              .properties("name").useCustomizeStringId()
              .ifNotExist().create());
        createIfMissing(edgeLabels, "follow", () ->
                schema.edgeLabel("follow")
              .sourceLabel("person").targetLabel("person")
              .ifNotExist().create());
        createIfMissing(edgeLabels, "like", () ->
                schema.edgeLabel("like")
              .sourceLabel("person").targetLabel("movie")
              .ifNotExist().create());
        createIfMissing(edgeLabels, "directedBy", () ->
                schema.edgeLabel("directedBy")
              .sourceLabel("movie").targetLabel("person")
              .ifNotExist().create());
    }

    private static void createHlmSchema(SchemaManager schema,
                                        Set<String> propertyKeys,
                                        Set<String> vertexLabels,
                                        Set<String> edgeLabels) {
        createIfMissing(propertyKeys, "name", () ->
                schema.propertyKey("name").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "gender", () ->
                schema.propertyKey("gender").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "age", () ->
                schema.propertyKey("age").asInt().ifNotExist().create());
        createIfMissing(propertyKeys, "title", () ->
                schema.propertyKey("title").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "feature", () ->
                schema.propertyKey("feature").asText().ifNotExist().create());
        createIfMissing(propertyKeys, "intimacy", () ->
                schema.propertyKey("intimacy").asText().ifNotExist().create());
        createIfMissing(vertexLabels, "人物", () ->
                schema.vertexLabel("人物")
              .properties("name", "gender", "age", "title", "feature")
              .primaryKeys("name")
              .ifNotExist().create());
        createIfMissing(edgeLabels, "关系", () ->
                schema.edgeLabel("关系")
              .sourceLabel("人物").targetLabel("人物")
              .properties("intimacy")
              .ifNotExist().create());
    }

    private static void createIfMissing(Set<String> existing, String name,
                                        Runnable create) {
        if (existing.contains(name)) {
            return;
        }
        create.run();
        existing.add(name);
    }

    private static String vertex(String variable, String name, int age,
                                 String city) {
        return String.format("%1$s = g.V().hasLabel('person').has('name','%2$s')" +
                             ".fold().coalesce(unfold(),addV('person')" +
                             ".property('name','%2$s').property('age',%3$d)" +
                             ".property('city','%4$s')).next();\n",
                             variable, name, age, city);
    }

    private static String nullableVertex(String variable, String name) {
        return String.format("%1$s = g.V().hasLabel('person').has('name','%2$s')" +
                             ".fold().coalesce(unfold(),addV('person')" +
                             ".property('name','%2$s')).next();\n", variable, name);
    }

    private static String software(int id, String name, int price) {
        return String.format("software%1$d = g.V(%1$d).hasLabel('software')" +
                             ".fold().coalesce(unfold(),addV('software')" +
                             ".property(T.id,%1$d).property('name','%2$s')" +
                             ".property('lang','java').property('price',%3$d))" +
                             ".next();\n", id, name, price);
    }

    private static String edge(String source, String label, String target,
                               String date, double weight) {
        return String.format("if (!g.V(%1$s.id()).outE('%2$s')" +
                             ".where(inV().hasId(%3$s.id())).hasNext()) { " +
                             "%1$s.addEdge('%2$s',%3$s,'date','%4$s'," +
                             "'weight',%5$s); };\n",
                             source, label, target, date, weight);
    }

    private static String created(String source, int target, String date,
                                  double weight) {
        return edge(source, "created", "software" + target, date, weight);
    }

    private static String rankVertex(String id, String label) {
        return String.format("v%1$s = g.V('%1$s').hasLabel('%2$s').fold()" +
                             ".coalesce(unfold(),addV('%2$s')" +
                             ".property(T.id,'%1$s').property('name','%1$s'))" +
                             ".next();\n", id, label);
    }

    private static String rankEdge(String source, String label, String target) {
        return String.format("if (!g.V(v%1$s.id()).outE('%2$s')" +
                             ".where(inV().hasId(v%3$s.id())).hasNext()) { " +
                             "v%1$s.addEdge('%2$s',v%3$s); };\n",
                             source, label, target);
    }

    private static String schema(String dataset) {
        if ("hlm".equals(dataset)) {
            return HLM_SCHEMA;
        }
        return "rank".equals(dataset) ? RANK_SCHEMA : LOADER_SCHEMA;
    }

    private static String data(String dataset) {
        if ("hlm".equals(dataset)) {
            return hlmData();
        }
        return "rank".equals(dataset) ? RANK_DATA : LOADER_DATA;
    }

    public static String hlmData() {
        return HlmDataHolder.DATA;
    }

    private static class HlmDataHolder {

        private static final String DATA = loadHlmData();
    }

    private static String source(String dataset) {
        if ("hlm".equals(dataset)) {
            return HLM_SOURCE;
        }
        return "rank".equals(dataset) ? RANK_SOURCE : LOADER_SOURCE;
    }

    private static String loadHlmData() {
        Map<String, String[]> vertices = new LinkedHashMap<>();
        List<String[]> edges = new ArrayList<>();
        try (InputStream stream = SampleGraphController.class
                .getResourceAsStream("/demo/hlm.txt")) {
            if (stream == null) {
                throw new IOException("Missing /demo/hlm.txt");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split(",", -1);
                    if (fields.length != 11) {
                        throw new IOException("Invalid Red Chamber record");
                    }
                    vertices.putIfAbsent(fields[0], person(fields, 0));
                    vertices.putIfAbsent(fields[5], person(fields, 5));
                    edges.add(new String[]{fields[0], fields[5], fields[10]});
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        StringBuilder gremlin = new StringBuilder();
        vertices.forEach((name, fields) -> gremlin.append(hlmVertex(fields)));
        edges.forEach(edge -> gremlin.append(hlmEdge(edge)));
        return gremlin.toString();
    }

    private static String[] person(String[] fields, int offset) {
        return new String[]{fields[offset], fields[offset + 1],
                            fields[offset + 2], fields[offset + 3],
                            fields[offset + 4]};
    }

    private static String hlmVertex(String[] person) {
        return String.format("g.V().hasLabel('人物').has('name','%1$s').fold()" +
                             ".coalesce(unfold(),addV('人物')" +
                             ".property('name','%1$s').property('gender','%2$s')" +
                             ".property('age',%3$s).property('title','%4$s')" +
                             ".property('feature','%5$s')).next();\n",
                             escape(person[0]), escape(person[1]), person[2],
                             escape(person[3]), escape(person[4]));
    }

    private static String hlmEdge(String[] edge) {
        return String.format("if (!g.V().hasLabel('人物')" +
                             ".has('name','%1$s').outE('关系')" +
                             ".where(inV().has('name','%2$s'))" +
                             ".has('intimacy','%3$s').hasNext()) { g.V()" +
                             ".hasLabel('人物').has('name','%1$s').next()" +
                             ".addEdge('关系',g.V().hasLabel('人物')" +
                             ".has('name','%2$s').next()," +
                             "'intimacy','%3$s'); };\n",
                             escape(edge[0]), escape(edge[1]), escape(edge[2]));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
