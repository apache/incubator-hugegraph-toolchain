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

package org.apache.hugegraph.exception;

public class ExternalException extends ParameterizedException {

    private int status;

    public ExternalException(String message, Object... args) {
        this(400, message, args);
    }

    public ExternalException(int status, String message, Object... args) {
        this(status, message, null, args);
    }

    public ExternalException(String message, Throwable cause, Object... args) {
        this(400, message, cause, args);
    }

    public ExternalException(int status, String message, Throwable cause,
                             Object... args) {
        super(message, cause, args);
        this.status = status;
    }

    public int status() {
        return this.status;
    }
}
