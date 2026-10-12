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

package org.apache.hugegraph.loader.test.unit;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.hugegraph.loader.progress.InputItemProgress;
import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.loader.reader.file.FileLineFetcher;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class FileLineFetcherTest {

    @Test
    public void testHeaderlessFileKeepsFirstRowMatchingColumnName()
            throws Exception {
        Path file = Files.createTempFile("headerless-", ".csv");
        Files.write(file, List.of("name", "Carol"));
        FileLineFetcher fetcher = this.fetcher(file, false);
        try {
            Line first = fetcher.fetch();
            Line second = fetcher.fetch();

            Assert.assertEquals("name", first.rawLine());
            Assert.assertEquals("Carol", second.rawLine());
            Assert.assertNull(fetcher.fetch());
        } finally {
            fetcher.closeReader();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testPhysicalHeaderSkipsMatchingFirstRow()
            throws Exception {
        Path file = Files.createTempFile("physical-header-", ".csv");
        Files.write(file, List.of("name", "Carol"));
        FileLineFetcher fetcher = this.fetcher(file, true);
        try {
            Line first = fetcher.fetch();

            Assert.assertEquals("Carol", first.rawLine());
            Assert.assertNull(fetcher.fetch());
        } finally {
            fetcher.closeReader();
            Files.deleteIfExists(file);
        }
    }

    private FileLineFetcher fetcher(Path path, boolean hasHeader) {
        FileSource source = new FileSource();
        source.header(new String[]{"name"});
        source.hasHeader(hasHeader);
        FileLineFetcher fetcher = new FileLineFetcher(source);
        fetcher.openReader(new TestReadable(path));
        return fetcher;
    }

    private static final class TestReadable implements Readable {

        private final Path path;

        private TestReadable(Path path) {
            this.path = path;
        }

        @Override
        public String name() {
            return this.path.getFileName().toString();
        }

        @Override
        public org.apache.hadoop.fs.Path path() {
            return new org.apache.hadoop.fs.Path(this.path.toString());
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(this.path.toFile());
        }

        @Override
        public InputItemProgress inputItemProgress() {
            return null;
        }
    }
}
