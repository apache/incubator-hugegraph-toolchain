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

package org.apache.hugegraph.exception;

import org.apache.hugegraph.constant.Constants;

public class ExitException extends RuntimeException {

    private final String details;
    private final Integer exitCode;

    public ExitException(String details, String reason) {
        super(reason);
        this.details = details;
        this.exitCode = Constants.EXIT_CODE_NORMAL;
    }

    public ExitException(String details, String reason,
                         Object... args) {
        super(String.format(reason, args));
        this.details = details;
        this.exitCode = Constants.EXIT_CODE_NORMAL;
    }

    public ExitException(Integer exitCode, String details,
                         String reason) {
        super(reason);
        this.details = details;
        this.exitCode = exitCode;
    }

    public ExitException(Integer exitCode, String details,
                         String reason, Throwable cause) {
        super(reason, cause);
        this.details = details;
        this.exitCode = exitCode;
    }

    public ExitException(Integer exitCode, String details,
                         String reason, Object... args) {
        super(String.format(reason, args));
        this.details = details;
        this.exitCode = exitCode;
    }

    public ExitException(Integer exitCode, String details,
                         String reason, Throwable cause,
                         Object... args) {
        super(String.format(reason, args), cause);
        this.details = details;
        this.exitCode = exitCode;
    }

    public String details() {
        return this.details;
    }

    public Integer exitCode() {
        return this.exitCode;
    }

    public static ExitException exception(String details, String reason,
                                          Object... args) {
        return new ExitException(Constants.EXIT_CODE_ERROR,
                                 details, reason, args);
    }

    public static ExitException normal(String details, String reason,
                                       Object... args) {
        return new ExitException(Constants.EXIT_CODE_NORMAL,
                                 details, reason, args);
    }
}
