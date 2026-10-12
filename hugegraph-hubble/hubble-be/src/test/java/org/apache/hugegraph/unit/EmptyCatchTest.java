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

public class EmptyCatchTest {

    private static final Pattern EMPTY_CATCH = Pattern.compile(
            "catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}",
            Pattern.MULTILINE | Pattern.DOTALL);

    @Test
    public void testJavaSourcesDoNotContainEmptyCatchBlocks()
            throws Exception {
        Path repo = repoRoot();
        List<Path> roots = Arrays.asList(
                repo.resolve("hugegraph-client/src"),
                repo.resolve("hugegraph-loader/src"),
                repo.resolve("hugegraph-tools/src"),
                repo.resolve("hugegraph-spark-connector/src"),
                repo.resolve("hugegraph-hubble/hubble-be/src")
        );
        List<String> violations = new ArrayList<>();

        for (Path root : roots) {
            collectEmptyCatches(root, violations);
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

    private static void collectEmptyCatches(Path root,
                                           List<String> violations)
            throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/target/"))
                 .forEach(path -> collectEmptyCatchesInFile(path, violations));
        }
    }

    private static void collectEmptyCatchesInFile(Path path,
                                                  List<String> violations) {
        try {
            String content = new String(Files.readAllBytes(path),
                                        StandardCharsets.UTF_8);
            if (EMPTY_CATCH.matcher(content).find()) {
                violations.add(path.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
