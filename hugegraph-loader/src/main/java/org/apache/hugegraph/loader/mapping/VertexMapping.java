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

package org.apache.hugegraph.loader.mapping;

import org.apache.hugegraph.loader.constant.ElemType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VertexMapping extends ElementMapping {

    // Be null when id strategy is primary key
    @JsonProperty("id")
    private final String idField;
    @JsonProperty("unfold")
    private final boolean unfold;

    @JsonCreator
    public VertexMapping(@JsonProperty("id") String idField,
                         @JsonProperty("unfold") boolean unfold) {
        this.idField = idField;
        this.unfold = unfold;
    }

    @Override
    public ElemType type() {
        return ElemType.VERTEX;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
    }

    public String idField() {
        return this.idField;
    }

    public boolean unfold() {
        return this.unfold;
    }

    @Override
    public String toString() {
        return String.format("vertex-mapping(label=%s)", this.label());
    }
}
