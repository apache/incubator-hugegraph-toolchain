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

package org.apache.hugegraph.api;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.util.E;

public abstract class API {

    public static final String CHARSET = "UTF-8";
    public static final String BATCH_ENCODING = "gzip";
    public static final long NO_LIMIT = -1L;
    public static final String PATH_SPLITOR = "/";

    protected final RestClient client;

    private String path;

    public API(RestClient client) {
        E.checkNotNull(client, "client");
        this.client = client;
        this.path = null;
    }

    public String path() {
        E.checkState(this.path != null, "Path can't be null");
        return this.path;
    }

    protected void path(String path) {
        this.path = path;
    }

    protected void path(String pathTemplate, Object... args) {
        this.path = String.format(pathTemplate, args);
    }

    protected abstract String type();

    protected static void checkOffset(long value) {
        E.checkArgument(value >= 0, "Offset must be >= 0, but got: %s", value);
    }

    protected static void checkLimit(long value, String name) {
        E.checkArgument(value > 0 || value == NO_LIMIT,
                        "%s must be > 0 or == %s, but got: %s",
                        name, NO_LIMIT, value);
    }
}
