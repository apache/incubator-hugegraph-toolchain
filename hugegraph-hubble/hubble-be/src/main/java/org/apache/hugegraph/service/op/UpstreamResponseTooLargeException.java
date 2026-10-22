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

import java.util.Map;

public class UpstreamResponseTooLargeException
       extends UpstreamRequestException {

    private static final long serialVersionUID = 1L;

    public UpstreamResponseTooLargeException() {
        super("upstream_response_too_large");
    }

    public UpstreamResponseTooLargeException(String path, long limit,
                                            long observedBytes, boolean exact) {
        super("upstream_response_too_large", null,
              Map.of("request_path", path, "max_response_bytes", limit,
                     "response_bytes", observedBytes, "response_size_exact", exact));
    }
}
