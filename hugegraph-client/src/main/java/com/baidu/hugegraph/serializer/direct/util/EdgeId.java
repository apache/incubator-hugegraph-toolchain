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

package com.baidu.hugegraph.serializer.direct.util;

import com.baidu.hugegraph.exception.NotFoundException;
import com.baidu.hugegraph.serializer.direct.struct.HugeType;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.IdUtil;

/**
 * Class used to format and parse id of edge, the edge id consists of:
 * { source-vertex-id + edge-label + edge-name + target-vertex-id }
 * NOTE: if we use `entry.type()` which is IN or OUT as a part of id,
 * an edge's id will be different due to different directions (belongs
 * to 2 owner vertex)
 */
public class EdgeId implements Id {

    private final Id ownerVertexId;
    private final Direction direction;
    private final Id edgeLabelId;
    private final String sortValues;
    private final Id otherVertexId;

    private final boolean directed;
    private String cache;

    public EdgeId(Id ownerVertexId, Direction direction, Id edgeLabelId,
                  String sortValues, Id otherVertexId) {
        this(ownerVertexId, direction, edgeLabelId,
             sortValues, otherVertexId, false);
    }

    public EdgeId(Id ownerVertexId, Direction direction, Id edgeLabelId,
                  String sortValues, Id otherVertexId, boolean directed) {
        this.ownerVertexId = ownerVertexId;
        this.direction = direction;
        this.edgeLabelId = edgeLabelId;
        this.sortValues = sortValues;
        this.otherVertexId = otherVertexId;
        this.directed = directed;
        this.cache = null;
    }

    private Id sourceVertexId() {
        return this.direction == Direction.OUT ?
               this.ownerVertexId :
               this.otherVertexId;
    }

    private Id targetVertexId() {
        return this.direction == Direction.OUT ?
               this.otherVertexId :
               this.ownerVertexId;
    }

    public Id ownerVertexId() {
        return this.ownerVertexId;
    }

    public Id edgeLabelId() {
        return this.edgeLabelId;
    }

    public Direction direction() {
        return this.direction;
    }

    public byte directionCode() {
        return directionToCode(this.direction);
    }

    public String sortValues() {
        return this.sortValues;
    }

    public Id otherVertexId() {
        return this.otherVertexId;
    }

    public String asString() {
        if (this.cache != null) {
            return this.cache;
        }
        if (this.directed) {
            this.cache = SplicingIdGenerator.concat(
                    IdUtil.writeString(this.ownerVertexId),
                    this.direction.type().string(),
                    IdUtil.writeLong(this.edgeLabelId),
                    this.sortValues,
                    IdUtil.writeString(this.otherVertexId));
        } else {
            this.cache = SplicingIdGenerator.concat(
                         IdUtil.writeString(this.sourceVertexId()),
                         IdUtil.writeLong(this.edgeLabelId),
                         this.sortValues,
                         IdUtil.writeString(this.targetVertexId()));
        }
        return this.cache;
    }

    @Override
    public byte[] asBytes() {
        return StringEncoding.encode(this.asString());
    }

    @Override
    public int length() {
        return this.asString().length();
    }

    @Override
    public IdType type() {
        return IdType.EDGE;
    }

    @Override
    public int hashCode() {
        if (this.directed) {
            return this.ownerVertexId.hashCode() ^
                   this.direction.hashCode() ^
                   this.edgeLabelId.hashCode() ^
                   this.sortValues.hashCode() ^
                   this.otherVertexId.hashCode();
        } else {
            return this.sourceVertexId().hashCode() ^
                   this.edgeLabelId.hashCode() ^
                   this.sortValues.hashCode() ^
                   this.targetVertexId().hashCode();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EdgeId)) {
            return false;
        }
        EdgeId other = (EdgeId) object;
        if (this.directed) {
            return this.ownerVertexId.equals(other.ownerVertexId) &&
                   this.direction == other.direction &&
                   this.edgeLabelId.equals(other.edgeLabelId) &&
                   this.sortValues.equals(other.sortValues) &&
                   this.otherVertexId.equals(other.otherVertexId);
        } else {
            return this.sourceVertexId().equals(other.sourceVertexId()) &&
                   this.edgeLabelId.equals(other.edgeLabelId) &&
                   this.sortValues.equals(other.sortValues) &&
                   this.targetVertexId().equals(other.targetVertexId());
        }
    }

    @Override
    public String toString() {
        return this.asString();
    }

    public static byte directionToCode(Direction direction) {
        return direction.type().code();
    }

    public static EdgeId parse(String id) {
        return parse(id, false);
    }

    public static EdgeId parse(String id, boolean returnNullIfError) {
        String[] idParts = SplicingIdGenerator.split(id);
        if (!(idParts.length == 4 || idParts.length == 5)) {
            if (returnNullIfError) {
                return null;
            }
            throw new HugeException("EdgeId must format as 4~5 parts, but got %s parts: '%s'",
                                    idParts.length, id);
        }
        try {
            if (idParts.length == 4) {
                Id ownerVertexId = IdUtil.readString(idParts[0]);
                Id edgeLabelId = IdUtil.readLong(idParts[1]);
                String sortValues = idParts[2];
                Id otherVertexId = IdUtil.readString(idParts[3]);
                return new EdgeId(ownerVertexId, Direction.OUT, edgeLabelId,
                                  sortValues, otherVertexId);
            } else {
                assert idParts.length == 5;
                Id ownerVertexId = IdUtil.readString(idParts[0]);
                HugeType direction = HugeType.fromString(idParts[1]);
                Id edgeLabelId = IdUtil.readLong(idParts[2]);
                String sortValues = idParts[3];
                Id otherVertexId = IdUtil.readString(idParts[4]);
                return new EdgeId(ownerVertexId, Direction.convert(direction),
                                  edgeLabelId, sortValues, otherVertexId);
            }
        } catch (Throwable e) {
            if (returnNullIfError) {
                return null;
            }
            throw new HugeException("Invalid format of edge id '%s'", e, id);
        }
    }

    public static Id parseStoredString(String id) {
        String[] idParts = split(id);
        E.checkArgument(idParts.length == 4, "Invalid id format: %s", id);
        Id ownerVertexId = IdUtil.readStoredString(idParts[0]);
        Id edgeLabelId = IdGenerator.ofStoredString(idParts[1], IdType.LONG);
        String sortValues = idParts[2];
        Id otherVertexId = IdUtil.readStoredString(idParts[3]);
        return new EdgeId(ownerVertexId, Direction.OUT, edgeLabelId,
                          sortValues, otherVertexId);
    }

    public static String asStoredString(Id id) {
        EdgeId eid = (EdgeId) id;
        return SplicingIdGenerator.concat(
               IdUtil.writeStoredString(eid.sourceVertexId()),
               IdGenerator.asStoredString(eid.edgeLabelId()),
               eid.sortValues(),
               IdUtil.writeStoredString(eid.targetVertexId()));
    }

    public static String[] split(String id) {
        return SplicingIdGenerator.split(id);
    }
}
