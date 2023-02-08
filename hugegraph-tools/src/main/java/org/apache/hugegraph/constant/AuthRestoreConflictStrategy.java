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

package org.apache.hugegraph.constant;

public enum AuthRestoreConflictStrategy {

    STOP(1, "stop"),
    IGNORE(2, "ignore");

    private int code;
    private String name = null;

    AuthRestoreConflictStrategy(int code, String name) {
        assert code < 256;
        this.code = code;
        this.name = name;
    }

    public int code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }

    public boolean isStopStrategy() {
        return this == AuthRestoreConflictStrategy.STOP;
    }

    public boolean isIgnoreStrategy() {
        return this == AuthRestoreConflictStrategy.IGNORE;
    }

    public static boolean matchStrategy(String strategy) {
        if (AuthRestoreConflictStrategy.STOP.string().equals(strategy) ||
            AuthRestoreConflictStrategy.IGNORE.string().equals(strategy)) {
            return true;
        }
        return false;
    }

    public static AuthRestoreConflictStrategy fromName(String name) {
        AuthRestoreConflictStrategy[] restoreStrategys = AuthRestoreConflictStrategy.values();
        for (AuthRestoreConflictStrategy strategy : restoreStrategys) {
            if (strategy.string().equals(name)) {
                return strategy;
            }
        }
        return null;
    }
}
