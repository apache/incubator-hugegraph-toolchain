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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;

public class BusinessAssertTest {

    private static final Pattern BUSINESS_ASSERT = Pattern.compile(
            "^\\s*assert\\b", Pattern.MULTILINE);

    @Test
    public void testHubbleBusinessSourcesDoNotUseJavaAssert()
            throws Exception {
        Path repo = repoRoot();
        List<Path> roots = Arrays.asList(
                repo.resolve("hugegraph-hubble/hubble-be/src/main/java/" +
                             "org/apache/hugegraph/controller"),
                repo.resolve("hugegraph-hubble/hubble-be/src/main/java/" +
                             "org/apache/hugegraph/service")
        );
        List<String> violations = new ArrayList<>();

        for (Path root : roots) {
            collectAsserts(root, violations);
        }

        Assert.assertTrue(violations.toString(), violations.isEmpty());
    }

    private static Path repoRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("hugegraph-client")) &&
                Files.isDirectory(current.resolve("hugegraph-hubble"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Failed to locate repository root");
    }

    private static void collectAsserts(Path root, List<String> violations)
            throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/target/"))
                 .forEach(path -> collectAssertsInFile(path, violations));
        }
    }

    private static void collectAssertsInFile(Path path,
                                             List<String> violations) {
        try {
            String content = new String(Files.readAllBytes(path),
                                        StandardCharsets.UTF_8);
            if (BUSINESS_ASSERT.matcher(content).find()) {
                violations.add(path.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
