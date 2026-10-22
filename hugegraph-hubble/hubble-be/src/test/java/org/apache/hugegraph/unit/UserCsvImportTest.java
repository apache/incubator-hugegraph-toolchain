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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.auth.UserService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

public class UserCsvImportTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void testQuotedUnicodeMultilineAndEmptyValues() throws Exception {
        File file = this.csv("user_name,user_password,user_email,user_description\r\n" +
                             "张三,\"  p,\"\"secret\"\"\n终  \",,\"first\nsecond\"\r\n");

        List<Map<String, String>> users = new UserService().readUserCsv(file);

        Assert.assertEquals(1, users.size());
        Assert.assertEquals(Map.of("user_name", "张三",
                                   "user_password", "  p,\"secret\"\n终  ",
                                   "user_email", "",
                                   "user_description", "first\nsecond"), users.get(0));
    }

    @Test
    public void testBomWhitespaceAndBlankLines() throws Exception {
        File file = this.csv("\uFEFF\"user_name\", user_password \n\n alice , secret \n\n");

        Assert.assertEquals(List.of(Map.of("user_name", "alice", "user_password", "secret")),
                            new UserService().readUserCsv(file));
    }

    @Test
    public void testRejectsMissingDuplicateAndBlankHeaders() throws Exception {
        for (String content : List.of("", "\n\n", "user_name,user_name\nalice,bob\n",
                                      "user_name, \nalice,secret\n", ",user_password\nalice,secret\n")) {
            this.assertInvalid(this.csv(content));
        }
    }

    @Test
    public void testRejectsEveryRecordWithMismatchedWidth() throws Exception {
        for (String records : List.of("alice\n", "alice,secret,extra\n",
                                      "alice,secret\nbob\n", "alice,secret\nbob,secret,extra\n",
                                      "\"\"\nalice,secret\n", "alice,secret\n\"\"\n")) {
            this.assertInvalid(this.csv("user_name,user_password\n" + records));
        }
    }

    @Test
    public void testValidatesWholeFileBeforeCreatingAnyAccounts() {
        HugeClient client = Mockito.mock(HugeClient.class);
        MockMultipartFile upload = new MockMultipartFile(
                "file", "users.csv", "text/csv",
                "user_name,user_password\nalice,secret\nbob,secret,unexpected\n"
                .getBytes(StandardCharsets.UTF_8));

        try {
            new UserService().addbatch(client, upload);
            Assert.fail("Expected invalid CSV to be rejected");
        } catch (InternalException error) {
            Assert.assertEquals("auth.user.import-csv.failed", error.getMessage());
            Assert.assertNull(error.getCause());
        }
        Mockito.verifyNoInteractions(client);
    }

    @Test
    public void testInvalidEncodingDoesNotExposeCsvContents() throws Exception {
        File file = this.temporary.newFile();
        Files.write(file.toPath(), new byte[]{(byte) 0xC3, (byte) 0x28});

        this.assertInvalid(file);
    }

    @Test
    public void testHeaderOnlyFileHasNoAccounts() throws Exception {
        Assert.assertTrue(new UserService().readUserCsv(
                this.csv("user_name,user_password\n")).isEmpty());
    }

    private File csv(String content) throws Exception {
        File file = this.temporary.newFile();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private void assertInvalid(File file) {
        try {
            new UserService().readUserCsv(file);
            Assert.fail("Expected invalid CSV to be rejected");
        } catch (InternalException error) {
            Assert.assertEquals("auth.user.import-csv.failed", error.getMessage());
            Assert.assertNull(error.getCause());
        }
    }
}
