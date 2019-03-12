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

package com.baidu.hugegraph.api.traverser.structure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.baidu.hugegraph.util.E;

public class SourceVertices {

    public Set<Object> ids;
    public String label;
    public Map<String, Object> properties;

    private SourceVertices() {
        this.ids = new HashSet<>();
        this.label = null;
        this.properties = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format("SourceVertices{ids=%s,label=%s,properties=%s}",
                             this.ids, this.label, this.properties);
    }

    public static class Builder {

        private SourceVertices sources;

        protected Builder() {
            this.sources = new SourceVertices();
        }

        public Builder ids(Set<Object> ids) {
            this.sources.ids.addAll(ids);
            return this;
        }

        public Builder ids(Object... ids) {
            this.sources.ids.addAll(Arrays.asList(ids));
            return this;
        }

        public Builder label(String label) {
            this.sources.label = label;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.sources.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            this.sources.properties.put(key, value);
            return this;
        }

        protected SourceVertices build() {
            E.checkArgument(!((this.sources.ids == null ||
                               this.sources.ids.isEmpty()) &&
                              (this.sources.properties == null ||
                               this.sources.properties.isEmpty()) &&
                              this.sources.label == null),
                            "No source vertices provided");
            return this.sources;
        }
    }
}
