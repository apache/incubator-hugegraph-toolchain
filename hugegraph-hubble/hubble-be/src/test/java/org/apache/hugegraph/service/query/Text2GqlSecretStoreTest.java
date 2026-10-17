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

package org.apache.hugegraph.service.query;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

import org.apache.hugegraph.exception.ExternalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Text2GqlSecretStoreTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void testKeyIsLazyOwnerOnlyPersistentAndEncryptionUsesFreshNonces() throws Exception {
        Path file = this.temporary.getRoot().toPath().resolve("data/text2gql-master.key");
        Text2GqlSecretStore store = new Text2GqlSecretStore(file, ignored -> null);
        assertFalse(Files.exists(file));
        String encrypted = store.encrypt("private-model-key");
        assertFalse(encrypted.contains("private-model-key"));
        assertNotEquals(encrypted, store.encrypt("private-model-key"));
        assertEquals("private-model-key", store.decrypt(encrypted));
        assertEquals(32, Base64.getDecoder().decode(Files.readString(file)).length);
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        }
        byte[] master = Files.readAllBytes(file);
        Text2GqlSecretStore restarted = new Text2GqlSecretStore(file, ignored -> null);
        assertEquals("private-model-key", restarted.decrypt(encrypted));
        org.junit.Assert.assertArrayEquals(master, Files.readAllBytes(file));
    }

    @Test
    public void testMissingMasterNeverCreatesReplacementDuringDecryption() throws Exception {
        Path file = this.temporary.getRoot().toPath().resolve("master.key");
        Text2GqlSecretStore store = new Text2GqlSecretStore(file, ignored -> null);
        String encrypted = store.encrypt("private-model-key");
        Files.delete(file);
        assertUnavailable(() -> store.decrypt(encrypted));
        assertFalse(Files.exists(file));
    }

    @Test
    public void testEnvironmentKeyNeedsNoFileAndRejectsWrongKeyOrModifiedCiphertext() {
        Path file = this.temporary.getRoot().toPath().resolve("unused.key");
        String firstKey = Base64.getEncoder().encodeToString(new byte[32]);
        byte[] second = new byte[32];
        second[0] = 1;
        String secondKey = Base64.getEncoder().encodeToString(second);
        Text2GqlSecretStore first = new Text2GqlSecretStore(file, ignored -> firstKey);
        String encrypted = first.encrypt("private-model-key");
        assertFalse(Files.exists(file));
        Text2GqlSecretStore wrong = new Text2GqlSecretStore(file, ignored -> secondKey);
        assertUnavailable(() -> wrong.decrypt(encrypted));
        byte[] packed = Base64.getDecoder().decode(encrypted.substring(3));
        packed[packed.length - 1] ^= 1;
        assertUnavailable(() -> first.decrypt("v1:" + Base64.getEncoder().encodeToString(packed)));
        assertUnavailable(() -> new Text2GqlSecretStore(file, ignored -> "invalid-base64-key")
                .encrypt("private-model-key"));
        assertTrue(encrypted.startsWith("v1:"));
    }

    private static void assertUnavailable(Runnable action) {
        try {
            action.run();
            fail("Expected a safe secret-store failure");
        } catch (ExternalException exception) {
            assertEquals(503, exception.status());
            assertEquals("text2gql.shared.secret-unavailable", exception.getMessage());
            assertNull(exception.getCause());
        }
    }
}
