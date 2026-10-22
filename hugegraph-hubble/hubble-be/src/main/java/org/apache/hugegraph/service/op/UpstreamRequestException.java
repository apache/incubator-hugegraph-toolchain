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

package org.apache.hugegraph.service.op;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpstreamRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> diagnostics;

    public UpstreamRequestException(String reason) {
        this(reason, null);
    }

    public UpstreamRequestException(String reason, Throwable cause) {
        this(reason, cause, Collections.emptyMap());
    }

    public UpstreamRequestException(String reason, Throwable cause,
                                    Map<String, Object> diagnostics) {
        super(reason, cause);
        this.diagnostics = Collections.unmodifiableMap(new LinkedHashMap<>(diagnostics));
    }

    public Map<String, Object> getDiagnostics() {
        return this.diagnostics;
    }
}
