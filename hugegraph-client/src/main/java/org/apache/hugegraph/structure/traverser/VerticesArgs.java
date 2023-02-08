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

package org.apache.hugegraph.structure.traverser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.util.E;

public class VerticesArgs {

    public Set<Object> ids;
    public String label;
    public Map<String, Object> properties;

    private VerticesArgs() {
        this.ids = new HashSet<>();
        this.label = null;
        this.properties = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("VerticesArgs{ids=%s,label=%s,properties=%s}",
                             this.ids, this.label, this.properties);
    }

    public static class Builder {

        private final VerticesArgs vertices;

        private Builder() {
            this.vertices = new VerticesArgs();
        }

        public Builder ids(Set<Object> ids) {
            this.vertices.ids.addAll(ids);
            return this;
        }

        public Builder ids(Object... ids) {
            this.vertices.ids.addAll(Arrays.asList(ids));
            return this;
        }

        public Builder label(String label) {
            this.vertices.label = label;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.vertices.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            this.vertices.properties.put(key, value);
            return this;
        }

        protected VerticesArgs build() {
            E.checkArgument(!((this.vertices.ids == null ||
                               this.vertices.ids.isEmpty()) &&
                              (this.vertices.properties == null ||
                               this.vertices.properties.isEmpty()) &&
                              (this.vertices.label == null ||
                               this.vertices.label.isEmpty())),
                            "No vertices provided");
            return this.vertices;
        }
    }
}
