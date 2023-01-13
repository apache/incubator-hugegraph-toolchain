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

package org.apache.hugegraph.util;

import java.util.concurrent.Callable;

import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.InternalException;

public final class Ex {

    public static void check(boolean expression, String message,
                             Object... args) {
        if (!expression) {
            throw new ExternalException(message, args);
        }
    }

    public static void check(boolean expression, int status,
                             String message, Object... args) {
        if (!expression) {
            throw new ExternalException(status, message, args);
        }
    }

    public static void check(boolean condition, Callable<Boolean> predicate,
                             String message, Object... args) {
        try {
            /*
             * The condition is a prerequisite, only make predication when
             * condition is true
             */
            if (!condition || predicate.call()) {
                return;
            }
        } catch (Exception e) {
            throw new InternalException("execute.predication.error", e);
        }
        throw new ExternalException(message, args);
    }

    public static void check(boolean expression, String message,
                             Throwable cause, Object... args) {
        if (!expression) {
            throw new ExternalException(message, cause, args);
        }
    }

    public static void check(boolean condition, Callable<Boolean> predicate,
                             String message, Throwable cause, Object... args) {
        try {
            /*
             * The condition is a prerequisite, only make predication when
             * condition is true
             */
            if (!condition || predicate.call()) {
                return;
            }
        } catch (Exception e) {
            throw new InternalException("execute.predication.error", e);
        }
        throw new ExternalException(message, cause, args);
    }

    public static Throwable rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
