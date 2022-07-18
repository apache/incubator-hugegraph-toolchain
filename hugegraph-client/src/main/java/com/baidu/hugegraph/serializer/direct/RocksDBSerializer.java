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

package com.baidu.hugegraph.serializer.direct;

import static com.baidu.hugegraph.serializer.direct.BinaryEntry.BackendColumn;
import static com.baidu.hugegraph.serializer.direct.BinaryEntry.BinaryId;
import static com.baidu.hugegraph.serializer.direct.struct.HugeType.EDGE;
import static com.baidu.hugegraph.serializer.direct.struct.HugeType.VERTEX;
import static com.baidu.hugegraph.structure.graph.Graph.HugeEdge;
import static com.baidu.hugegraph.structure.graph.Graph.HugeVertex;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.backend.id.IdGenerator;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.serializer.direct.struct.HugeElement;
import com.baidu.hugegraph.serializer.direct.struct.HugeType;
import com.baidu.hugegraph.serializer.direct.util.BytesBuffer;
import com.baidu.hugegraph.serializer.direct.util.EdgeId;
import com.baidu.hugegraph.serializer.direct.util.GraphSchema;
import com.baidu.hugegraph.serializer.direct.util.HugeException;
import com.baidu.hugegraph.serializer.direct.util.Id;
import com.baidu.hugegraph.serializer.direct.util.StringEncoding;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.HugeProperty;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.type.define.HugeKeys;
import com.baidu.hugegraph.util.Bytes;
import com.baidu.hugegraph.util.E;

/**
 * In this serializer, we only support normal type now:
 * - number
 * - string
 * And they will be transferred to bytes directly
 **/
public class RocksDBSerializer {

    /*
     * ID is stored in column name if keyWithIdPrefix=true like RocksDB, stored in rowkey for HBase
     */
    private final boolean keyWithIdPrefix;
    private final boolean indexWithIdPrefix;
    private final boolean enablePartition;
    GraphSchema schema;

    public RocksDBSerializer(HugeClient client) {
        this(true, true, false, client);
    }

    public RocksDBSerializer(boolean keyWithIdPrefix, boolean indexWithIdPrefix,
                             boolean enablePartition, HugeClient client) {
        // only consider rocksdb now
        this.keyWithIdPrefix = true;
        this.indexWithIdPrefix = true;
        this.enablePartition = false;
        this.schema = new GraphSchema(client);
    }

    protected BinaryEntry newBackendEntry(HugeType type, Id id) {
        if (type == VERTEX) {
            BytesBuffer buffer = BytesBuffer.allocate(2 + 1 + id.length());
            writePartitionedId(VERTEX, id, buffer);
            return new BinaryEntry(type, buffer.bytes());
        }

        if (type == EDGE) {
            return new BinaryEntry(type, (BinaryId) id);
        }

        BytesBuffer buffer = BytesBuffer.allocate(1 + id.length());
        byte[] idBytes = buffer.writeId(id).bytes();
        return new BinaryEntry(type, new BinaryId(idBytes, id));
    }

    protected final BinaryEntry newBackendEntry(Vertex vertex) {
        return newBackendEntry(VERTEX, vertex.id());
    }

    protected final BinaryEntry newBackendEntry(Edge edge) {
        BinaryId id = writeEdgeId(edge.idWithDirection());
        return newBackendEntry(EDGE, id);
    }

    protected final BinaryEntry newBackendEntry(SchemaElement elem) {
        return newBackendEntry(elem.type(), elem.id());
    }

    protected BackendColumn formatLabel(GraphElement elem) {
        BackendColumn col = new BackendColumn();
        col.name = this.formatSyspropName(elem.id(), HugeKeys.LABEL);
        Id label = elem.schemaLabel().id();
        BytesBuffer buffer = BytesBuffer.allocate(label.length() + 1);
        col.value = buffer.writeId(label).bytes();
        return col;
    }

    protected byte[] formatPropertyName(HugeProperty<?> prop) {
        Id id = prop.element().id();
        int idLen = this.keyWithIdPrefix ? 1 + id.length() : 0;
        Id pkeyId = prop.propertyKey().id();
        BytesBuffer buffer = BytesBuffer.allocate(idLen + 2 + pkeyId.length());
        if (this.keyWithIdPrefix) {
            buffer.writeId(id);
        }
        buffer.write(prop.type().code());
        buffer.writeId(pkeyId);
        return buffer.bytes();
    }

    protected BackendColumn formatProperty(HugeProperty<?> prop) {
        BytesBuffer buffer = BytesBuffer.allocate(BytesBuffer.BUF_PROPERTY);
        buffer.writeProperty(prop.propertyKey(), prop.value());
        return BackendColumn.of(this.formatPropertyName(prop), buffer.bytes());
    }

    protected void parseProperty(Id pkeyId, BytesBuffer buffer, GraphElement owner) {
        PropertyKey pkey = owner.graph().propertyKey(pkeyId);

        // Parse value
        Object value = buffer.readProperty(pkey);

        // Set properties of vertex/edge
        if (pkey.cardinality() == Cardinality.SINGLE) {
            owner.addProperty(pkey, value);
        } else {
            if (!(value instanceof Collection)) {
                throw new HugeException("Invalid value of non-single property: %s", value);
            }
            owner.addProperty(pkey, value);
        }
    }

    protected void formatProperties(Map<String, Object> props,
                                    BytesBuffer buffer) {
        // Write properties size
        buffer.writeVInt(props.size());

        // Write properties data
        for (Map.Entry<String, Object> kv : props.entrySet()) {
            String key = kv.getKey();
            Object value = kv.getValue();
            PropertyKey pkey =
        }

        for (HugeProperty<?> property : props) {
            PropertyKey pkey = property.propertyKey();
            buffer.writeVInt(SchemaElement.schemaId(pkey.id()));
            buffer.writeProperty(pkey, property.value());
        }
    }

    protected void parseProperties(BytesBuffer buffer, HugeElement owner) {
        int size = buffer.readVInt();
        assert size >= 0;
        for (int i = 0; i < size; i++) {
            Id pkeyId = IdGenerator.of(buffer.readVInt());
            this.parseProperty(pkeyId, buffer, owner);
        }
    }

    protected byte[] formatEdgeValue(Edge edge) {
        int propsCount = edge.sizeOfProperties();
        BytesBuffer buffer = BytesBuffer.allocate(4 + 16 * propsCount);

        // Write edge id
        //buffer.writeId(edge.id());

        // Write edge properties
        this.formatProperties(edge.properties(), buffer);
        return buffer.bytes();
    }

    protected void parseEdge(BackendColumn col, HugeVertex vertex) {
        // owner-vertex + dir + edge-label + sort-values + other-vertex

        BytesBuffer buffer = BytesBuffer.wrap(col.name);
        if (this.keyWithIdPrefix) {
            // Consume owner-vertex id
            buffer.readId();
        }
        byte type = buffer.read();
        Id labelId = buffer.readId();
        String sortValues = buffer.readStringWithEnding();
        Id otherVertexId = buffer.readId();

        boolean direction = EdgeId.isOutDirectionFromCode(type);
        EdgeLabel edgeLabel = schema.getEdgeLabel(labelId);

        // Construct edge
        HugeEdge edge = HugeEdge.constructEdge(vertex, direction, edgeLabel,
                                               sortValues, otherVertexId);

        // Parse edge-id + edge-properties
        buffer = BytesBuffer.wrap(col.value);

        //Id id = buffer.readId();

        // Parse edge properties
        this.parseProperties(buffer, edge);
    }

    protected void parseVertex(byte[] value, HugeVertex vertex) {
        BytesBuffer buffer = BytesBuffer.wrap(value);

        // Parse vertex label
        VertexLabel label = schema.getVertexLabel(buffer.readId());
        vertex.correctVertexLabel(label);

        // Parse properties
        this.parseProperties(buffer, vertex);
    }

    protected void parseColumn(BackendColumn col, Vertex vertex) {
        BytesBuffer buffer = BytesBuffer.wrap(col.name);
        Id id = this.keyWithIdPrefix ? buffer.readId() : vertex.id();
        E.checkState(buffer.remaining() > 0, "Missing column type");
        byte type = buffer.read();
        // Parse property
        if (type == HugeType.PROPERTY.code()) {
            Id pkeyId = buffer.readId();
            this.parseProperty(pkeyId, BytesBuffer.wrap(col.value), vertex);
        }
        // Parse edge
        else if (type == HugeType.EDGE_IN.code() ||
                 type == HugeType.EDGE_OUT.code()) {
            this.parseEdge(col, vertex);
        } else {
            E.checkState(false, "Invalid entry(%s) with unknown type(%s): 0x%s",
                         id, type & 0xff, Bytes.toHex(col.name));
        }
    }

    public BinaryEntry writeVertex(Vertex vertex) {
        BinaryEntry entry = newBackendEntry(vertex);

        int propsCount = vertex.sizeOfProperties();
        BytesBuffer buffer = BytesBuffer.allocate(8 + 16 * propsCount);

        // Write vertex label
        buffer.writeId(vertex.schemaLabel().id());

        // Write all properties of the vertex
        this.formatProperties(vertex.properties(), buffer);

        // Fill column
        byte[] name = this.keyWithIdPrefix ? entry.id().asBytes() : BytesBuffer.BYTES_EMPTY;
        entry.column(name, buffer.bytes());

        return entry;
    }

    public HugeVertex readVertex(BinaryEntry bytesEntry) {
        if (bytesEntry == null) {
            return null;
        }
        BinaryEntry entry = this.convertEntry(bytesEntry);

        // Parse id
        Id id = entry.id().origin();
        Id vid = id.edge() ? ((EdgeId) id).ownerVertexId() : id;
        HugeVertex vertex = new HugeVertex(vid, VertexLabel.NONE);

        // Parse all properties and edges of a Vertex
        Iterator<BackendColumn> iterator = entry.columns().iterator();
        for (int index = 0; iterator.hasNext(); index++) {
            BackendColumn col = iterator.next();
            if (entry.type().isEdge()) {
                // NOTE: the entry id type is vertex even if entry type is edge
                // Parse vertex edges
                this.parseColumn(col, vertex);
            } else {
                assert entry.type().isVertex();
                // Parse vertex properties
                assert entry.columnsSize() >= 1 : entry.columnsSize();
                if (index == 0) {
                    this.parseVertex(col.value, vertex);
                } else {
                    this.parseVertexOlap(col.value, vertex);
                }
            }
        }

        return vertex;
    }

    protected void parseVertexOlap(byte[] value, HugeVertex vertex) {
        BytesBuffer buffer = BytesBuffer.wrap(value);
        Id pkeyId = IdGenerator.of(buffer.readVInt());
        this.parseProperty(pkeyId, buffer, vertex);
    }

    public BinaryEntry writeEdge(HugeEdge edge) {
        BinaryEntry entry = newBackendEntry(edge);
        byte[] name = this.keyWithIdPrefix ?
                      entry.id().asBytes() : BytesBuffer.BYTES_EMPTY;
        byte[] value = this.formatEdgeValue(edge);
        entry.column(name, value);

        return entry;
    }

    public HugeEdge readEdge(BinaryEntry bytesEntry) {
        HugeVertex vertex = this.readVertex(bytesEntry);
        Collection<HugeEdge> edges = vertex.getEdges();
        if (edges.size() != 1) {
            E.checkState(false, "Expect 1 edge in vertex, but got %s", edges.size());
        }
        return edges.iterator().next();
    }

    public BinaryEntry writeId(HugeType type, Id id) {
        return newBackendEntry(type, id);
    }

    private BinaryId writeEdgeId(Id id) {
        EdgeId edgeId;
        if (id instanceof EdgeId) {
            edgeId = (EdgeId) id;
        } else {
            edgeId = EdgeId.parse(id.asString());
        }
        BytesBuffer buffer = BytesBuffer.allocate(BytesBuffer.BUF_EDGE_ID);
        if (this.enablePartition) {
            buffer.writeShort(getPartition(HugeType.EDGE, edgeId.ownerVertexId()));
            buffer.writeEdgeId(edgeId);
        } else {
            buffer.writeEdgeId(edgeId);
        }
        return new BinaryId(buffer.bytes(), id);
    }

    private void writePartitionedId(HugeType type, Id id, BytesBuffer buffer) {
        if (this.enablePartition) {
            buffer.writeShort(getPartition(type, id));
            buffer.writeId(id);
        } else {
            buffer.writeId(id);
        }
    }

    protected short getPartition(HugeType type, Id id) {
        return 0;
    }

    public BinaryEntry parse(BinaryEntry originEntry) {
        byte[] bytes = originEntry.id().asBytes();
        BinaryEntry parsedEntry = new BinaryEntry(originEntry.type(), bytes, this.enablePartition);

        if (this.enablePartition) {
            bytes = Arrays.copyOfRange(bytes, parsedEntry.id().length() + 2, bytes.length);
        } else {
            bytes = Arrays.copyOfRange(bytes, parsedEntry.id().length(), bytes.length);
        }
        BytesBuffer buffer = BytesBuffer.allocate(BytesBuffer.BUF_EDGE_ID);
        buffer.write(parsedEntry.id().asBytes());
        buffer.write(bytes);
        parsedEntry = new BinaryEntry(originEntry.type(),
                                      new BinaryId(buffer.bytes(), BytesBuffer.wrap(buffer.bytes()).readEdgeId()));

        for (BackendColumn col : originEntry.columns()) {
            parsedEntry.column(buffer.bytes(), col.value);
        }
        return parsedEntry;
    }


    protected static boolean indexIdLengthExceedLimit(Id id) {
        return id.asBytes().length > BytesBuffer.INDEX_HASH_ID_THRESHOLD;
    }

    protected static boolean indexFieldValuesUnmatched(byte[] value, Object fieldValues) {
        if (value != null && value.length > 0 && fieldValues != null) {
            return !StringEncoding.decode(value).equals(fieldValues);
        }
        return false;
    }
}
