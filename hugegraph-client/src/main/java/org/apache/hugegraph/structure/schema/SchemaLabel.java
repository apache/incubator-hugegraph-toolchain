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

package org.apache.hugegraph.structure.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.hugegraph.structure.SchemaElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public abstract class SchemaLabel extends SchemaElement {

    @JsonProperty("nullable_keys")
    protected Set<String> nullableKeys;
    @JsonProperty("enable_label_index")
    protected Boolean enableLabelIndex;
    @JsonProperty("index_labels")
    protected List<String> indexLabels;

    public SchemaLabel(String name) {
        super(name);
        this.nullableKeys = new ConcurrentSkipListSet<>();
        this.indexLabels = null;
        this.enableLabelIndex = null;
    }

    public Set<String> nullableKeys() {
        return this.nullableKeys;
    }

    public List<String> indexLabels() {
        if (this.indexLabels == null) {
            return ImmutableList.of();
        }
        return Collections.unmodifiableList(this.indexLabels);
    }

    public boolean enableLabelIndex() {
        return this.enableLabelIndex;
    }
}
