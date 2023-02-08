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

package org.apache.hugegraph.structure.gremlin;

import java.util.Map;

import org.apache.hugegraph.driver.GraphManager;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    public static class Status {
        @JsonProperty
        private String message;
        @JsonProperty
        private int code;
        @JsonProperty
        private Map<String, ?> attributes;

        public String message() {
            return message;
        }

        public int code() {
            return code;
        }

        public Map<String, ?> attributes() {
            return attributes;
        }
    }

    @JsonProperty
    private String requestId;
    @JsonProperty
    private Status status;
    @JsonProperty
    private ResultSet result;

    public void graphManager(GraphManager graphManager) {
        this.result.graphManager(graphManager);
    }

    public String requestId() {
        return this.requestId;
    }

    public Status status() {
        return this.status;
    }

    public ResultSet result() {
        return this.result;
    }
}
