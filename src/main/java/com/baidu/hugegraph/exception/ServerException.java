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

import java.util.Map;

import javax.ws.rs.core.Response;

import com.baidu.hugegraph.rest.RestResult;

public class ServerException extends RuntimeException {

    private static final long serialVersionUID = 6335623004322652358L;

    private static final String[] EXCEPTION_KEYS = {"exception",
                                                    "Exception-Class"};
    private static final String[] MESSAGE_KEYS = {"message"};
    private static final String[] CAUSE_KEYS = {"cause", "stackTrace"};

    private int status = 0;
    private String exception;
    private String message;
    private String cause;

    public static ServerException fromResponse(Response response) {
        RestResult rs = new RestResult(response);
        ServerException exception = new ServerException(rs.content());
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> json = rs.readObject(Map.class);
            exception.exception = getByKeys(json, EXCEPTION_KEYS);
            exception.message = getByKeys(json, MESSAGE_KEYS);
            exception.cause = getByKeys(json, CAUSE_KEYS);
        } catch (Exception ignored) {}
        exception.status(response.getStatus());
        return exception;
    }

    public ServerException(String message) {
        this.message = message;
    }

    public ServerException(String message, Object... args) {
        this(String.format(message, args));
    }

    public String exception() {
        return this.exception;
    }

    public String message() {
        return this.message;
    }

    public String cause() {
        return this.cause;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public Throwable getCause() {
        if (this.cause() == null || this.cause().isEmpty()) {
            return null;
        }
        return new ServerCause(this.cause());
    }

    public void status(int status) {
        this.status = status;
    }

    public int status() {
        return this.status;
    }

    @Override
    public String toString() {
        String s = this.exception;
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    private static String getByKeys(Map<String, String> json, String[] keys) {
        for (String key : keys) {
            if (json.containsKey(key)) {
                return json.get(key);
            }
        }
        return null;
    }

    /**
     * The stack trace of server exception
     */
    private static class ServerCause extends RuntimeException {

        private static final long serialVersionUID = 8755660573085501031L;

        public ServerCause(String cause) {
            super(cause, null, true, false);
        }

        @Override
        public String toString() {
            return super.getMessage();
        }
    }
}
