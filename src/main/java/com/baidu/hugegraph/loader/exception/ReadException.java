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

package com.baidu.hugegraph.loader.exception;

public class ReadException extends ParseException {

    private static final long serialVersionUID = 5626071847324807449L;

    public ReadException(String line, Throwable cause) {
        super(line, cause);
    }

    public ReadException(String line, String message, Object... args) {
        super(line, message, args);
    }

    public ReadException(String line, String message, Throwable cause,
                         Object... args) {
        super(line, message, cause, args);
    }
}
