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

package com.baidu.hugegraph.testutil;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class Utils {

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
        if (left.primaryKeys().size() != right.primaryKeys().size() ||
            !left.primaryKeys().containsAll(right.primaryKeys())) {
            return false;
        }
        if (left.indexNames().size() != right.indexNames().size() ||
            !left.indexNames().containsAll(right.indexNames())) {
            return false;
        }
        return true;
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
        if (left.sortKeys().size() != right.sortKeys().size() ||
            !left.sortKeys().containsAll(right.sortKeys())) {
            return false;
        }
        if (left.indexNames().size() != right.indexNames().size() ||
            !left.indexNames().containsAll(right.indexNames())) {
            return false;
        }
        return true;
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
        if (left.indexFields().size() != right.indexFields().size() ||
            !left.indexFields().containsAll(right.indexFields())) {
            return false;
        }
        return true;
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
        if (!left.source().equals(right.source())) {
            return false;
        }
        if (!left.target().equals(right.target())) {
            return false;
        }
        if (!left.sourceLabel().equals(right.sourceLabel())) {
            return false;
        }
        if (!left.targetLabel().equals(right.targetLabel())) {
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
}
