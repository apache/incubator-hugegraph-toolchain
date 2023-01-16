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

package org.apache.hugegraph.test.functional;

import org.apache.hugegraph.cmd.HugeGraphCommand;
import org.apache.hugegraph.exception.ExitException;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class CommandTest extends AuthTest {

    @Test
    public void testHelpCommand() {
        String[] args = new String[]{
                "--throw-mode", "true",
                "--user", USER_NAME,
                "--password", USER_PASSWORD,
                "help"
        };

        Assert.assertThrows(ExitException.class, () -> {
            HugeGraphCommand.main(args);
        }, e -> {
            ExitException exception = (ExitException) e;
            Assert.assertContains("Command : hugegragh help",
                                  exception.getMessage());
            Assert.assertContains("Usage: hugegraph [options] [command]",
                                  exception.details());
        });
    }

    @Test
    public void testHelpSubCommand() {
        String[] args = new String[]{
                "--throw-mode", "true",
                "--user", USER_NAME,
                "--password", USER_PASSWORD,
                "help", "auth-backup"
        };

        Assert.assertThrows(ExitException.class, () -> {
            HugeGraphCommand.main(args);
        }, e -> {
            ExitException exception = (ExitException) e;
            Assert.assertContains("Command : hugegragh help auth-backup",
                                  exception.getMessage());
            Assert.assertContains("Usage: auth-backup [options]",
                                  exception.details());
        });
    }

    @Test
    public void testBadHelpSubCommandException() {
        String badCommand = "asd";
        String[] args = new String[]{
                "--throw-mode", "true",
                "--user", USER_NAME,
                "--password", USER_PASSWORD,
                "help", badCommand
        };

        Assert.assertThrows(ExitException.class, () -> {
            HugeGraphCommand.main(args);
        }, e -> {
            ExitException exception = (ExitException) e;
            Assert.assertContains(String.format(
                                  "Unexpected help sub-command %s",
                                  badCommand), exception.getMessage());
            Assert.assertContains("Here are some sub-command ",
                                  exception.details());
        });
    }

    @Test
    public void testEmptyCommandException() {
        String[] args = new String[]{
                "--throw-mode", "true",
                "--user", USER_NAME,
                "--password", USER_PASSWORD
        };

        Assert.assertThrows(ExitException.class, () -> {
            HugeGraphCommand.main(args);
        }, e -> {
            ExitException exception = (ExitException) e;
            Assert.assertContains("No sub-command found",
                                  exception.getMessage());
            Assert.assertContains("Warning : must provide one sub-command",
                                  exception.details());
        });
    }
}
