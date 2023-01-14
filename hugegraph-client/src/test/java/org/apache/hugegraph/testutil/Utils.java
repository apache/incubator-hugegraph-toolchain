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

package org.apache.hugegraph.testutil;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hugegraph.date.SafeDateFormat;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert.ThrowableRunnable;
import org.apache.hugegraph.util.DateUtil;

import com.google.common.collect.ImmutableList;

public final class Utils {

    private static final String DF = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final SafeDateFormat DATE_FORMAT = new SafeDateFormat(DF);

    public static void assertResponseError(int status, ThrowableRunnable run) {
        Assert.assertThrows(ServerException.class, run, (e) -> {
            if (e instanceof ServerException) {
                Assert.assertEquals("The rest status code is not matched",
                                    status, ((ServerException) e).status());
            }
        });
    }

    public static void assertGraphEqual(ImmutableList<Vertex> vertices,
                                        ImmutableList<Edge> edges,
                                        List<Object> objects) {
        for (Object object : objects) {
            Assert.assertTrue(object instanceof GraphElement);
            if (object instanceof Vertex) {
                Assert.assertTrue(Utils.contains(vertices, (Vertex) object));
            } else {
                Assert.assertTrue(object instanceof Edge);
                Assert.assertTrue(Utils.contains(edges, (Edge) object));
            }
        }
    }

    public static boolean contains(List<PropertyKey> propertyKeys,
                                   PropertyKey propertyKey) {
        for (PropertyKey pk : propertyKeys) {
            if (equalPropertyKey(pk, propertyKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<VertexLabel> vertexLabels,
                                   VertexLabel vertexLabel) {
        for (VertexLabel vl : vertexLabels) {
            if (equalVertexLabel(vl, vertexLabel)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<EdgeLabel> edgeLabels,
                                   EdgeLabel edgeLabel) {
        for (EdgeLabel el : edgeLabels) {
            if (equalEdgeLabel(el, edgeLabel)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<IndexLabel> indexLabels,
                                   IndexLabel indexLabel) {
        for (IndexLabel il : indexLabels) {
            if (equalIndexLabel(il, indexLabel)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<Vertex> vertices, Vertex vertex) {
        for (Vertex v : vertices) {
            if (equalVertex(v, vertex)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<Edge> edges, Edge edge) {
        for (Edge e : edges) {
            if (equalEdge(e, edge)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalPropertyKey(PropertyKey left,
                                           PropertyKey right) {
        if (!left.name().equals(right.name())) {
            return false;
        }
        if (left.dataType() != right.dataType()) {
            return false;
        }
        if (left.cardinality() != right.cardinality()) {
            return false;
        }
        return true;
    }

    public static boolean equalVertexLabel(VertexLabel left,
                                           VertexLabel right) {
        assert left != null;
        assert right != null;

        if (!left.name().equals(right.name())) {
            return false;
        }
        if (left.idStrategy() != right.idStrategy()) {
            return false;
        }
        if (left.properties().size() != right.properties().size() ||
            !left.properties().containsAll(right.properties())) {
            return false;
        }
        return left.primaryKeys().size() == right.primaryKeys().size() &&
               left.primaryKeys().containsAll(right.primaryKeys());
    }

    public static boolean equalEdgeLabel(EdgeLabel left, EdgeLabel right) {
        assert left != null;
        assert right != null;

        if (!left.name().equals(right.name())) {
            return false;
        }
        if (!left.sourceLabel().equals(right.sourceLabel())) {
            return false;
        }
        if (!left.targetLabel().equals(right.targetLabel())) {
            return false;
        }
        if (left.frequency() != right.frequency()) {
            return false;
        }
        if (left.properties().size() != right.properties().size() ||
            !left.properties().containsAll(right.properties())) {
            return false;
        }
        return left.sortKeys().size() == right.sortKeys().size() &&
               left.sortKeys().containsAll(right.sortKeys());
    }

    private static boolean equalIndexLabel(IndexLabel left,
                                           IndexLabel right) {
        assert left != null;
        assert right != null;

        if (!left.name().equals(right.name())) {
            return false;
        }
        if (left.baseType() != right.baseType()) {
            return false;
        }
        if (!left.baseValue().equals(right.baseValue())) {
            return false;
        }
        if (left.indexType() != right.indexType()) {
            return false;
        }
        return left.indexFields().size() == right.indexFields().size() &&
               left.indexFields().containsAll(right.indexFields());
    }

    private static boolean equalVertex(Vertex left, Vertex right) {
        assert left != null;
        assert right != null;

        if (!left.label().equals(right.label())) {
            return false;
        }
        Map<String, Object> leftProps = left.properties();
        Map<String, Object> rightProps = right.properties();
        if (leftProps.size() != rightProps.size() ||
            !leftProps.keySet().containsAll(rightProps.keySet())) {
            return false;
        }
        for (String key : leftProps.keySet()) {
            if (!leftProps.get(key).equals(rightProps.get(key)) &&
                leftProps.get(key) != rightProps.get(key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalEdge(Edge left, Edge right) {
        assert left != null;
        assert right != null;

        if (!left.label().equals(right.label())) {
            return false;
        }
        if (!left.sourceId().equals(right.sourceId())) {
            return false;
        }
        if (!left.targetId().equals(right.targetId())) {
            return false;
        }

        // Only compare source label when the expected `right` passed
        if (right.sourceLabel() != null) {
            if (!left.sourceLabel().equals(right.sourceLabel())) {
                return false;
            }
        }
        // Only compare target label when the expected `right` passed
        if (right.targetLabel() != null) {
            if (!left.targetLabel().equals(right.targetLabel())) {
                return false;
            }
        }

        Map<String, Object> leftProps = left.properties();
        Map<String, Object> rightProps = right.properties();
        if (leftProps.size() != rightProps.size() ||
            !leftProps.keySet().containsAll(rightProps.keySet())) {
            return false;
        }
        for (String key : leftProps.keySet()) {
            if (!leftProps.get(key).equals(rightProps.get(key)) &&
                leftProps.get(key) != rightProps.get(key)) {
                return false;
            }
        }
        return true;
    }

    public static Optional<String> getLabelValue(final Object... keyValues) {
        for (int i = 0; i < keyValues.length; i = i + 2) {
            if (keyValues[i].equals(T.LABEL)) {
                return Optional.of((String) keyValues[i + 1]);
            }
        }
        return Optional.empty();
    }

    public static Map<String, Object> asMap(Object... keyValues) {
        return Utils.asPairs(keyValues).stream()
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    public static List<Pair<String, Object>> asPairs(Object... keyValues) {
        final List<Object> list = Arrays.asList(keyValues);
        return IntStream.range(1, list.size())
                        .filter(i -> i % 2 != 0)
                        .mapToObj(i -> Pair.of(list.get(i - 1).toString(),
                                               list.get(i)))
                        .collect(Collectors.toList());
    }

    public static long date(String date) {
        return date(date, "yyyy-MM-dd");
    }

    public static long date(String date, String pattern) {
        return DateUtil.parse(date, pattern).getTime();
    }

    public static String formatDate(String date) {
        return DATE_FORMAT.format(DateUtil.parse(date));
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static void assertBeforeNow(Date createTime) {
        Date now = DateUtil.now();
        Assert.assertTrue(createTime.before(now) || createTime.equals(now));
    }
}
