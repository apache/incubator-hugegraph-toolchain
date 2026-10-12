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

package org.apache.hugegraph.unit;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.auth.UserService;

public class UserImportSafetyTest {

    @Test
    public void testTemporaryFileFailureIsNotReturnedAsNull() throws Exception {
        MultipartFile upload = Mockito.mock(MultipartFile.class);
        Mockito.when(upload.getOriginalFilename()).thenReturn("users.csv");
        AtomicReference<File> temporaryFile = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            File file = invocation.getArgument(0);
            temporaryFile.set(file);
            throw new IOException("disk unavailable");
        }).when(upload).transferTo(Mockito.any(File.class));

        assertInternalException(() ->
                new UserService().multipartFileToFile(upload));

        Assert.assertNotNull(temporaryFile.get());
        Assert.assertFalse(temporaryFile.get().exists());
    }

    @Test
    public void testNullOriginalFilenameUsesSafeTemporaryName()
           throws Exception {
        MultipartFile upload = Mockito.mock(MultipartFile.class);
        Mockito.when(upload.getOriginalFilename()).thenReturn(null);

        File file = new UserService().multipartFileToFile(upload);

        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.delete());
    }

    @Test
    public void testExtensionlessFilenameUsesSafeTemporaryName()
           throws Exception {
        MultipartFile upload = Mockito.mock(MultipartFile.class);
        Mockito.when(upload.getOriginalFilename()).thenReturn("users");

        File file = new UserService().multipartFileToFile(upload);

        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.delete());
    }

    @Test(expected = InternalException.class)
    public void testNullCsvFileHasActionableFailure() {
        new UserService().readCsvByCsvReader(null);
    }

    private static void assertInternalException(ThrowingRunnable runnable)
                                                throws Exception {
        try {
            runnable.run();
        } catch (InternalException ignored) {
            return;
        }
        Assert.fail("Expected InternalException");
    }

    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}
