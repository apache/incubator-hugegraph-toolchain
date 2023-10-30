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

package org.apache.hugegraph.spark.connector.mapping;

import java.util.List;

import org.apache.hugegraph.spark.connector.constant.DataTypeEnum;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EdgeMapping extends ElementMapping {

    @JsonProperty("source")
    private final List<String> sourceFields;

    @JsonProperty("target")
    private final List<String> targetFields;

    public EdgeMapping(List<String> sourceFields, List<String> targetFields) {
        this.sourceFields = sourceFields;
        this.targetFields = targetFields;
    }

    @Override
    public DataTypeEnum type() {
        return DataTypeEnum.EDGE;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
        E.checkArgument(this.sourceFields != null && !this.sourceFields.isEmpty(),
                        "The source field of edge label '%s' can't be null or empty", this.label());
        E.checkArgument(this.targetFields != null && !this.targetFields.isEmpty(),
                        "The target field of edge label '%s' can't be null or empty", this.label());
    }

    public List<String> sourceFields() {
        return this.sourceFields;
    }

    public List<String> targetFields() {
        return this.targetFields;
    }

    @Override
    public String toString() {
        return String.format("edge-mapping(label=%s)", this.label());
    }
}
