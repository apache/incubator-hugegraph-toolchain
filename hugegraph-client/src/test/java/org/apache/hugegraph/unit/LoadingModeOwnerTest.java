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

package org.apache.hugegraph.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.hugegraph.exception.InvalidResponseException;
import org.apache.hugegraph.structure.LoadingModeOwner;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class LoadingModeOwnerTest {

    @Test
    public void testRequiresActualCapability() {
        Assert.assertThrows(InvalidResponseException.class, () -> new LoadingModeOwner(null));
        Assert.assertThrows(InvalidResponseException.class, () -> new LoadingModeOwner(new HashMap<>()));
        Map<String, Object> capability = capability();
        capability.put("protocol", 2);
        Assert.assertThrows(InvalidResponseException.class, () -> new LoadingModeOwner(capability));
        capability.put("protocol", 1);
        capability.put("epoch", 0.5);
        Assert.assertThrows(InvalidResponseException.class, () -> new LoadingModeOwner(capability));
    }

    @Test
    public void testEveryReceiptIsBoundToStableTuple() {
        LoadingModeOwner owner = new LoadingModeOwner(capability());
        Map<String, Object> acquire = owner.request("acquire");
        Map<String, Object> release = owner.request("release");
        Assert.assertEquals(acquire.get("instance"), release.get("instance"));
        Assert.assertEquals(acquire.get("epoch"), release.get("epoch"));
        Assert.assertEquals(acquire.get("token"), release.get("token"));
        Map<String, Object> receipt = receipt(owner, "active", "LOADING");
        owner.verify(receipt, true);
        for (String key : new String[]{"protocol", "instance", "epoch", "token", "state", "mode"}) {
            Map<String, Object> corrupt = new HashMap<>(receipt);
            corrupt.put(key, "wrong");
            Assert.assertThrows(InvalidResponseException.class, () -> owner.verify(corrupt, true));
        }
        Assert.assertThrows(InvalidResponseException.class, () -> owner.verify(null, true));
    }

    @Test
    public void testInvalidReceiptDoesNotExposeOwnerToken() {
        LoadingModeOwner owner = new LoadingModeOwner(capability());
        String token = (String) owner.request("acquire").get("token");
        Assert.assertFalse(owner.toString().contains(token));
        for (Object invalid : new Object[]{null, UUID.randomUUID().toString()}) {
            Map<String, Object> receipt = receipt(owner, "active", "LOADING");
            receipt.put("token", invalid);
            InvalidResponseException error = (InvalidResponseException) Assert.assertThrows(
                    InvalidResponseException.class, () -> owner.verify(receipt, true));
            Assert.assertFalse(error.getMessage().contains(token));
        }
    }

    @Test
    public void testReleasedCannotConfirmAcquireAndReleaseAllowsAnotherActiveOwner() {
        LoadingModeOwner owner = new LoadingModeOwner(capability());
        Map<String, Object> released = receipt(owner, "released", "LOADING");
        owner.verify(released, false);
        Assert.assertThrows(InvalidResponseException.class, () -> owner.verify(released, true));
        owner.verify(receipt(owner, "released", "NONE"), false);
        Assert.assertThrows(InvalidResponseException.class,
                            () -> owner.verify(receipt(owner, "active", "LOADING"), false));
    }

    private static Map<String, Object> capability() {
        Map<String, Object> capability = new HashMap<>();
        capability.put("protocol", 1);
        capability.put("instance", UUID.randomUUID().toString());
        capability.put("epoch", 0);
        return capability;
    }

    private static Map<String, Object> receipt(LoadingModeOwner owner, String state, String mode) {
        Map<String, Object> receipt = new HashMap<>(owner.request("acquire"));
        receipt.remove("action");
        receipt.put("protocol", 1);
        receipt.put("state", state);
        receipt.put("mode", mode);
        return receipt;
    }
}
