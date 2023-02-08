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

package org.apache.hugegraph.loader.exception;

public class InsertException extends IllegalArgumentException {

    private static final long serialVersionUID = 2218730111752576867L;

    private final String line;

    public InsertException(String line, Throwable cause) {
        super(cause.getMessage(), cause);
        this.line = line;
    }

    public InsertException(String line, String message, Object... args) {
        super(String.format(message, args));
        this.line = line;
    }

    public InsertException(String line, String message, Throwable cause,
                           Object... args) {
        super(String.format(message, args), cause);
        this.line = line;
    }

    public String line() {
        return this.line;
    }
}
