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

package com.baidu.hugegraph.serializer.direct.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.util.Strings;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.glassfish.jersey.internal.guava.Sets;

import com.baidu.hugegraph.backend.id.IdGenerator;
import com.baidu.hugegraph.backend.query.ConditionQuery;
import com.baidu.hugegraph.backend.query.QueryResults;
import com.baidu.hugegraph.config.CoreOptions;
import com.baidu.hugegraph.perf.PerfUtil.Watched;
import com.baidu.hugegraph.schema.PropertyKey;
import com.baidu.hugegraph.schema.VertexLabel;
import com.baidu.hugegraph.serializer.direct.BytesBuffer;
import com.baidu.hugegraph.serializer.direct.util.Id;
import com.baidu.hugegraph.serializer.direct.util.SplicingIdGenerator;
import com.baidu.hugegraph.serializer.direct.util.HugeException;
import com.baidu.hugegraph.type.define.Cardinality;
import com.baidu.hugegraph.type.define.Directions;
import com.baidu.hugegraph.type.define.IdStrategy;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * We need a simple vertex struct for direct encode
 * 1. id (only support number + string type) & treat as bytes
 * 2. property, save a map<k,v>
 *
 * So how could we deal with multi situation? (we judge before construct it), like:
 * 1. idStrategy should be set / check before load
 * 2. edges should do only in HugeEdge
 * 3. any transaction actions should be ignored
 **/
public class HugeVertex extends HugeElement implements Vertex, Cloneable {

    private static final List<HugeEdge> EMPTY_LIST = ImmutableList.of();

    private Id id;
    protected Collection<HugeEdge> edges;

    public HugeVertex(Id id, String label) {

        E.checkArgumentNotNull(label, "Vertex label can't be null");
        this.label = label;

        this.id = id;
        this.edges = EMPTY_LIST;
        if (this.id != null) {
            this.checkIdLength();
        }
    }


    @Override
    public Id id() {
        return this.id;
    }

    @Watched(prefix = "vertex")
    public void assignId(Id id) {
        IdStrategy strategy = this.label.idStrategy();
        // Generate an id and assign
        switch (strategy) {
            case CUSTOMIZE_STRING:
                assert !id.number();
                this.id = id;
                break;
            case CUSTOMIZE_NUMBER:
                assert id.number();
                this.id = id;
                break;
            case CUSTOMIZE_UUID:
                this.id = id.uuid() ? id : IdGenerator.of(id.asString(), true);
                break;
            case PRIMARY_KEY:
                this.id = SplicingIdGenerator.instance().generate(this);
                break;
            default:
                throw new HugeException("Unknown id strategy" + strategy);
        }
        this.checkIdLength();
    }

    protected void checkIdLength() {
        assert this.id != null;
        int len = this.id.asBytes().length;
        if (len > BytesBuffer.ID_LEN_MAX) {
            throw new HugeException("The max length of vertex id is 128, but got" + len);
        }

    }

    public void correctVertexLabel(VertexLabel correctLabel) {
        E.checkArgumentNotNull(correctLabel, "Vertex label can't be null");
        if (this.label != null && !this.label.undefined() &&
            !correctLabel.undefined()) {
            E.checkArgument(this.label.equals(correctLabel),
                            "Vertex label can't be changed from '%s' to '%s'",
                            this.label, correctLabel);
        }
        this.label = correctLabel;
    }

    @Watched(prefix = "vertex")
    protected List<Object> primaryValues() {
        E.checkArgument(this.label.idStrategy() == IdStrategy.PRIMARY_KEY,
                        "The id strategy '%s' don't have primary keys",
                        this.label.idStrategy());
        List<Id> primaryKeys = this.label.primaryKeys();
        E.checkArgument(!primaryKeys.isEmpty(),
                        "Primary key can't be empty for id strategy '%s'",
                        IdStrategy.PRIMARY_KEY);

        boolean encodeNumber = this.graph()
                                   .option(CoreOptions.VERTEX_ENCODE_PK_NUMBER);
        List<Object> propValues = new ArrayList<>(primaryKeys.size());
        for (Id pk : primaryKeys) {
            HugeProperty<?> property = this.getProperty(pk);
            E.checkState(property != null,
                         "The value of primary key '%s' can't be null",
                         this.graph().propertyKey(pk).name());
            Object propValue = property.serialValue(encodeNumber);
            if (Strings.EMPTY.equals(propValue)) {
                propValue = ConditionQuery.INDEX_VALUE_EMPTY;
            }
            propValues.add(propValue);
        }
        return propValues;
    }

    public boolean existsEdges() {
        return this.edges.size() > 0;
    }

    public Collection<HugeEdge> getEdges() {
        return Collections.unmodifiableCollection(this.edges);
    }

    public void addEdge(HugeEdge edge) {
        if (this.edges == EMPTY_LIST) {
            this.edges = newList();
        }
        this.edges.add(edge);
    }

    /**
     * Add edge with direction OUT
     * @param edge the out edge
     */
    @Watched
    public void addOutEdge(HugeEdge edge) {
        if (edge.ownerVertex() == null) {
            edge.sourceVertex(this);
        }
        E.checkState(edge.isDirection(Directions.OUT),
                     "The owner vertex('%s') of OUT edge '%s' should be '%s'",
                     edge.ownerVertex().id(), edge, this.id());
        this.addEdge(edge);
    }

    /**
     * Add edge with direction IN
     * @param edge the in edge
     */
    @Watched
    public void addInEdge(HugeEdge edge) {
        if (edge.ownerVertex() == null) {
            edge.targetVertex(this);
        }
        E.checkState(edge.isDirection(Directions.IN),
                     "The owner vertex('%s') of IN edge '%s' should be '%s'",
                     edge.ownerVertex().id(), edge, this.id());
        this.addEdge(edge);
    }

    public Iterator<Edge> getEdges(Directions direction, String... edgeLabels) {
        List<Edge> list = new LinkedList<>();
        for (HugeEdge edge : this.edges) {
            if (edge.matchDirection(direction) &&
                edge.belongToLabels(edgeLabels)) {
                list.add(edge);
            }
        }
        return list.iterator();
    }

    public Iterator<Vertex> getVertices(Directions direction,
                                        String... edgeLabels) {
        List<Vertex> list = new LinkedList<>();
        Iterator<Edge> edges = this.getEdges(direction, edgeLabels);
        while (edges.hasNext()) {
            HugeEdge edge = (HugeEdge) edges.next();
            list.add(edge.otherVertex(this));
        }
        return list.iterator();
    }

    @Watched(prefix = "vertex")
    @Override
    public Iterator<Vertex> vertices(Direction direction,
                                     String... edgeLabels) {
        Iterator<Edge> edges = this.edges(direction, edgeLabels);
        return this.graph().adjacentVertices(edges);
    }

    @Watched(prefix = "vertex")
    @Override
    public <V> VertexProperty<V> property(
               VertexProperty.Cardinality cardinality,
               String key, V value, Object... objects) {
        if (objects.length != 0 && objects[0].equals(T.id)) {
            throw VertexProperty.Exceptions.userSuppliedIdsNotSupported();
        }
        // TODO: extra props: objects
        if (objects.length != 0) {
            throw VertexProperty.Exceptions.metaPropertiesNotSupported();
        }

        PropertyKey propertyKey = this.graph().propertyKey(key);
        /*
         * g.AddV("xxx").property("key1", val1).property("key2", val2)
         * g.AddV("xxx").property(single, "key1", val1)
         *              .property(list, "key2", val2)
         *
         * The cardinality single may be user supplied single, it may also be
         * that user doesn't supplied cardinality, when it is latter situation,
         * we shouldn't check it. Because of this reason, we are forced to
         * give up the check of user supplied cardinality single.
         * The cardinality not single must be user supplied, so should check it
         */
        if (cardinality != VertexProperty.Cardinality.single) {
            E.checkArgument(propertyKey.cardinality() ==
                            Cardinality.convert(cardinality),
                            "Invalid cardinality '%s' for property key '%s', " +
                            "expect '%s'", cardinality, key,
                            propertyKey.cardinality().string());
        }

        // Check key in vertex label
        E.checkArgument(VertexLabel.OLAP_VL.equals(this.label) ||
                        this.label.properties().contains(propertyKey.id()),
                        "Invalid property '%s' for vertex label '%s'",
                        key, this.label);
        // Primary-Keys can only be set once
        if (this.schemaLabel().primaryKeys().contains(propertyKey.id())) {
            E.checkArgument(!this.hasProperty(propertyKey.id()),
                            "Can't update primary key: '%s'", key);
        }

        @SuppressWarnings("unchecked")
        VertexProperty<V> prop = (VertexProperty<V>) this.addProperty(
                                 propertyKey, value, !this.fresh());
        return prop;
    }

    @Watched(prefix = "vertex")
    @Override
    protected <V> HugeVertexProperty<V> newProperty(PropertyKey pkey, V val) {
        return new HugeVertexProperty<>(this, pkey, val);
    }

    @Watched(prefix = "vertex")
    @Override
    protected boolean ensureFilledProperties(boolean throwIfNotExist) {
        if (this.isPropLoaded()) {
            this.updateToDefaultValueIfNone();
            return true;
        }

        // Skip query if there is no any property key in schema
        if (this.schemaLabel().properties().isEmpty()) {
            this.propLoaded();
            return true;
        }

        // NOTE: only adjacent vertex will reach here
        Iterator<Vertex> vertices = this.graph().adjacentVertex(this.id());
        HugeVertex vertex = (HugeVertex) QueryResults.one(vertices);
        if (vertex == null && !throwIfNotExist) {
            return false;
        }
        E.checkState(vertex != null, "Vertex '%s' does not exist", this.id);
        if (vertex.schemaLabel().undefined() ||
            !vertex.schemaLabel().equals(this.schemaLabel())) {
            // Update vertex label of dangling edge to undefined
            this.correctVertexLabel(VertexLabel.undefined(this.graph()));
            vertex.resetProperties();
        }
        this.copyProperties(vertex);
        this.updateToDefaultValueIfNone();
        return true;
    }

    @Watched(prefix = "vertex")
    @SuppressWarnings("unchecked") // (VertexProperty<V>) prop
    @Override
    public <V> Iterator<VertexProperty<V>> properties(String... keys) {
        // TODO: Compatible with TinkerPop properties() (HugeGraph-742)
        this.ensureFilledProperties(true);

        // Capacity should be about the following size
        int propsCapacity = keys.length == 0 ?
                            this.sizeOfProperties() :
                            keys.length;
        List<VertexProperty<V>> props = new ArrayList<>(propsCapacity);

        if (keys.length == 0) {
            for (HugeProperty<?> prop : this.getProperties()) {
                assert prop instanceof VertexProperty;
                props.add((VertexProperty<V>) prop);
            }
        } else {
            for (String key : keys) {
                Id pkeyId;
                try {
                    pkeyId = this.graph().propertyKey(key).id();
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                HugeProperty<?> prop = this.getProperty(pkeyId);
                if (prop == null) {
                    // Not found
                    continue;
                }
                assert prop instanceof VertexProperty;
                props.add((VertexProperty<V>) prop);
            }
        }

        return props.iterator();
    }

    public boolean valid() {
        try {
            return this.ensureFilledProperties(false);
        } catch (Throwable e) {
            // Generally the program can't get here
            return false;
        }
    }

    public static final Id getIdValue(Object idValue) {
        return HugeElement.getIdValue(idValue);
    }

    // we don't use origin sets now
    private static <V> Set<V> newSet() {
        return Sets.newHashSet();
    }

    private static <V> List<V> newList() {
        return Lists.newArrayList();
    }
}
