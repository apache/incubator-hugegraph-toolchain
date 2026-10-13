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

package org.apache.hugegraph.structure;

import java.util.Map;
import java.util.UUID;

import org.apache.hugegraph.exception.InvalidResponseException;

import com.google.common.collect.ImmutableMap;

/** One import handle, bound to one live graph instance and capability epoch. */
public final class LoadingModeOwner {

    private final String instance;
    private final long epoch;
    private final String token;

    public LoadingModeOwner(Map<String, Object> capability) {
        if (capability == null || !Integer.valueOf(1).equals(capability.get("protocol")) ||
            !(capability.get("instance") instanceof String) ||
            !validUUID((String) capability.get("instance")) ||
            !(capability.get("epoch") instanceof Number)) {
            throw new InvalidResponseException("Server does not confirm loading ownership protocol 1");
        }
        Number epoch = (Number) capability.get("epoch");
        if (!(epoch instanceof Integer || epoch instanceof Long) || epoch.longValue() < 0) {
            throw new InvalidResponseException("Invalid loading owner epoch");
        }
        this.instance = (String) capability.get("instance");
        this.epoch = epoch.longValue();
        this.token = UUID.randomUUID().toString();
    }

    public Map<String, Object> request(String action) {
        return ImmutableMap.<String, Object>builder().put("action", action).put("instance", this.instance)
                           .put("epoch", this.epoch).put("token", this.token).build();
    }

    public void verify(Map<String, Object> receipt, boolean acquire) {
        Object epoch = receipt == null ? null : receipt.get("epoch");
        String state = acquire ? "active" : "released";
        Object mode = receipt == null ? null : receipt.get("mode");
        if (receipt == null || !Integer.valueOf(1).equals(receipt.get("protocol")) ||
            !this.instance.equals(receipt.get("instance")) || !this.token.equals(receipt.get("token")) ||
            !(epoch instanceof Integer || epoch instanceof Long) || ((Number) epoch).longValue() != this.epoch ||
            !state.equals(receipt.get("state")) ||
            (acquire ? !"LOADING".equals(mode) : !("LOADING".equals(mode) || "NONE".equals(mode) ||
                                                 "RESTORING".equals(mode) || "MERGING".equals(mode)))) {
            throw new InvalidResponseException("Server does not confirm loading owner %s for %s", state, this);
        }
    }

    @Override
    public String toString() {
        return this.instance + "/" + this.epoch;
    }

    private static boolean validUUID(String value) {
        try {
            return value.length() == 36 && UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
