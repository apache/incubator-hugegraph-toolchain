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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.structure.constant;

import com.fasterxml.jackson.annotation.JsonProperty;


public class EdgeLink {

    @JsonProperty
    private String source;
    @JsonProperty
    private String target;

    public static EdgeLink of(String source, String target) {
        return new EdgeLink(source, target);
    }

    public EdgeLink() {
        super();
    }

    public EdgeLink(String source, String target) {
        super();
        this.source = source;
        this.target = target;
    }

    public String source() {
        return source;
    }

    public String target() {
        return target;
    }

    @Override
    public String toString() {
        return String.format("source=%s, target=%s", this.source, this.target);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EdgeLink)) {
            return false;
        }

        EdgeLink other = (EdgeLink) obj;
        if (this.source().equals(other.source())
                && this.target().equals(other.target())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.source().hashCode() ^ this.target().hashCode();
    }

}
