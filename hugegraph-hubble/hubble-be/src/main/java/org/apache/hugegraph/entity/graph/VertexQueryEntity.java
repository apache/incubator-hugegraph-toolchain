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

package org.apache.hugegraph.entity.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.hugegraph.structure.graph.Vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class VertexQueryEntity extends Vertex {
    @JsonProperty("statistics")
    private Map<String, Object> statistics;

    public VertexQueryEntity(String label) {
        super(label);
        this.statistics = new HashMap<>();
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public static VertexQueryEntity fromVertex(Vertex vertex) {
        VertexQueryEntity vertexQE =
                new VertexQueryEntity(vertex.label());
        vertexQE.id(vertex.id());
        vertexQE.setProperties(vertex.properties());
        return vertexQE;
    }

    public static Collection<VertexQueryEntity> fromVertices(Collection<Vertex> vertices) {
        Collection<VertexQueryEntity> vertexQueryEntities = new ArrayList<>();
        for (Vertex vertex: vertices) {
            vertexQueryEntities.add(fromVertex(vertex));
        }
        return vertexQueryEntities;
    }

    public Vertex convertVertex() {
        // get Vertex instance from VertexQueryEntity
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, label=%s, properties=%s, statistics=%s}",
                             this.id(),
                this.label(), this.properties(), this.statistics);
    }
}
