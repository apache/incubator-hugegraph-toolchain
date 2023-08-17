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

package org.apache.hugegraph.rest;

import okhttp3.Headers;

import java.util.Map;

public interface OkhttpRestClient {
    /**
     * Post method
     */
    OkhttpRestResult post(String path, Object object);

    OkhttpRestResult post(String path, Object object, Headers headers);
    OkhttpRestResult post(String path, Object object, Map<String, Object> params);

    OkhttpRestResult post(String path, Object object, Headers headers,
                          Map<String, Object> params);

    /**
     * Put method
     */
    OkhttpRestResult put(String path, String id, Object object);
    OkhttpRestResult put(String path, String id, Object object, Headers headers);

    OkhttpRestResult put(String path, String id, Object object, Map<String, Object> params);

    OkhttpRestResult put(String path, String id, Object object, Headers headers,
                         Map<String, Object> params);

    /**
     * Get method
     */
    OkhttpRestResult get(String path);

    OkhttpRestResult get(String path, Map<String, Object> params);

    OkhttpRestResult get(String path, String id);

    /**
     * Delete method
     */
    OkhttpRestResult delete(String path, Map<String, Object> params);

    OkhttpRestResult delete(String path, String id);

    void close();
}
