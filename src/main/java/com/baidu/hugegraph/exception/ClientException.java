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

package com.baidu.hugegraph.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientException extends IllegalArgumentException {

    private int status = 0;
    @JsonProperty
    private String exception;
    @JsonProperty
    private String message;
    @JsonProperty
    private String cause;

    public String exception() {
        return exception;
    }

    public String message() {
        return message;
    }

    public String cause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ClientException(String message) {
        super(message);
        this.message = message;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public void status(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
