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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;

public class ConsolePrintTest {

    @Test
    public void testMainSourceDoesNotUseConsolePrints() throws Exception {
        Path sourceRoot = Paths.get("src/main/java");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                 .forEach(path -> collectConsolePrints(path, violations));
        }

        Assert.assertTrue(violations.toString(), violations.isEmpty());
    }

    private static void collectConsolePrints(Path path,
                                             List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path,
                                                    StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("System.out.println") ||
                    line.contains("System.err.println") ||
                    line.contains(".printStackTrace()")) {
                    violations.add(path + ":" + (i + 1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
