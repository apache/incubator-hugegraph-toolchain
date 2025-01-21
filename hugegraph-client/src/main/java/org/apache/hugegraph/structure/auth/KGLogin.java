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

package org.apache.hugegraph.structure.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KGLogin {

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("token_expire")
    private long expire = 31536000;

    @JsonProperty("sign")
    private String sign;

    public void name(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public void sign(String sign) {
        this.sign = sign;
    }

    public String sign() {
        return this.sign;
    }

    public void expire(long expire) {
        this.expire = expire;
    }

    public long expire() {
        return this.expire;
    }
}
