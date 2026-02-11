/*
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

package org.apache.hugegraph.entity.schema;

import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.structure.constant.HugeType;

public enum SchemaType {

    PROPERTY_KEY("property key"),

    VERTEX_LABEL("vertex label"),

    EDGE_LABEL("edge label"),

    PROPERTY_INDEX("property index");

    private String name;

    SchemaType(String name) {
        this.name = name;
    }

    public String string() {
        return this.name;
    }

    public boolean isPropertyKey() {
        return this == PROPERTY_KEY;
    }

    public boolean isVertexLabel() {
        return this == VERTEX_LABEL;
    }

    public boolean isEdgeLabel() {
        return this == EDGE_LABEL;
    }

    public boolean isPropertyIndex() {
        return this == PROPERTY_INDEX;
    }

    public static SchemaType convert(HugeType type) {
        switch (type) {
            case PROPERTY_KEY:
                return PROPERTY_KEY;
            case VERTEX_LABEL:
                return VERTEX_LABEL;
            case EDGE_LABEL:
                return EDGE_LABEL;
            case INDEX_LABEL:
                return PROPERTY_INDEX;
            default:
                throw new InternalException(
                          "Can't convert HugeType '%s' to SchemaType", type);
        }
    }
}
