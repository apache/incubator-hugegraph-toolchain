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

package org.apache.hugegraph.service.load;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class FilePreviewReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCsvPreservesQuotedAndEmptyCells() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        Path file = this.write("\"family,name\",nickname,age\n\"Lee, Mei\",,18\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("family,name", "nickname", "age"), columns.names());
        Assert.assertEquals(List.of("Lee, Mei", "", "18"), columns.values());
    }

    @Test
    public void testCsvWithoutHeaderKeepsLeadingAndTrailingEmptyCells() throws Exception {
        FileSetting setting = new FileSetting();
        Path file = this.write(",\"value,with comma\",\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("col-1", "col-2", "col-3"), columns.names());
        Assert.assertEquals(List.of("", "value,with comma", ""), columns.values());
    }

    @Test
    public void testCsvSkipsEmptyPhysicalLinesBeforeHeaderAndSample() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        for (String data : List.of("a,b\n\n1,2\n", "\n\na,b\n\n1,2\n")) {
            FilePreviewReader.Columns columns = FilePreviewReader.read(this.write(data, setting), setting);

            Assert.assertEquals(List.of("a", "b"), columns.names());
            Assert.assertEquals(List.of("1", "2"), columns.values());
        }
    }

    @Test
    public void testCsvQuotedEmptyFieldRemainsADataLine() throws Exception {
        FileSetting setting = new FileSetting();
        Path file = this.write("\n\"\"\nvalue\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("col-1"), columns.names());
        Assert.assertEquals(List.of(""), columns.values());
    }

    @Test
    public void testTextTreatsMultiCharacterDelimiterAsOneSeparator() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setFormat("TEXT");
        setting.setDelimiter("::");
        Path file = this.write("a:b::::c\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("col-1", "col-2", "col-3"), columns.names());
        Assert.assertEquals(List.of("a:b", "", "c"), columns.values());
    }

    @Test
    public void testConfiguredCharsetAppliesToHeaderAndSample() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        setting.setCharset("GB18030");
        Path file = this.write("姓名,城市\n李明,北京\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("姓名", "城市"), columns.names());
        Assert.assertEquals(List.of("李明", "北京"), columns.values());
    }

    @Test
    public void testPreviewAllowsSelectingCharsetAfterInitialUpload() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        Path file = this.folder.newFile().toPath();
        Files.writeString(file, "姓名,城市\n李明,北京\n", Charset.forName("GB18030"));

        FilePreviewReader.Columns initial = FilePreviewReader.read(file, setting);
        Assert.assertTrue(String.join(",", initial.names()).contains("\uFFFD"));

        setting.setCharset("GB18030");
        FilePreviewReader.Columns corrected = FilePreviewReader.read(file, setting);
        Assert.assertEquals(List.of("姓名", "城市"), corrected.names());
        Assert.assertEquals(List.of("李明", "北京"), corrected.values());
    }

    @Test
    public void testSkippedLinesAreIgnoredBeforeHeaderAndSample() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        Path file = this.write("# file description\nname,age\n// sample comment\n# more\nMei,18\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("name", "age"), columns.names());
        Assert.assertEquals(List.of("Mei", "18"), columns.values());
    }

    @Test
    public void testHeaderOnlyHasEmptySample() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        Path file = this.write("name,age\n\n# no data yet\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("name", "age"), columns.names());
        Assert.assertEquals(List.of(), columns.values());
    }

    @Test
    public void testEmptyAndCommentOnlyFilesAreRejected() throws Exception {
        FileSetting setting = new FileSetting();
        for (String data : List.of("", "\n\n", "# comment\n// comment\n")) {
            Path file = this.write(data, setting);
            Assert.assertThrows(ExternalException.class, () -> FilePreviewReader.read(file, setting));
        }
    }

    @Test
    public void testInvalidSkipPatternIsRejectedBeforeOpeningFile() {
        FileSetting setting = new FileSetting();
        setting.setSkippedLine("[");
        Path missingFile = this.folder.getRoot().toPath().resolve("missing.csv");

        Assert.assertThrows(PatternSyntaxException.class, () -> FilePreviewReader.read(missingFile, setting));
    }

    @Test
    public void testJsonRetainsFieldOrderAndValues() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setFormat("JSON");
        Path file = this.write("{\"name\":\"Mei\",\"age\":18,\"nickname\":null}\n", setting);

        FilePreviewReader.Columns columns = FilePreviewReader.read(file, setting);

        Assert.assertEquals(List.of("name", "age", "nickname"), columns.names());
        Assert.assertEquals(List.of("Mei", "18", "null"), columns.values());
    }

    @Test
    public void testMalformedJsonDoesNotExposeImportedValues() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setFormat("JSON");
        Path file = this.write("{\"password\":\"private-preview-canary\"", setting);

        Throwable error = Assert.assertThrows(
                InternalException.class, () -> FilePreviewReader.read(file, setting));

        Assert.assertFalse(error.toString().contains("private-preview-canary"));
        Assert.assertNull(error.getCause());
    }

    @Test
    public void testFileMappingUsesSharedPreview() throws Exception {
        FileSetting setting = new FileSetting();
        setting.setHasHeader(true);
        setting.setCharset("GB18030");
        Path file = this.write("\"姓名,别名\",城市\n# comment\n\"李明,小李\",\n", setting);
        FileMapping mapping = new FileMapping("DEFAULT", "hugegraph", "data.csv", file.toString());
        mapping.setFileSetting(setting);
        FileMappingService service = Mockito.spy(new FileMappingService());
        Mockito.doReturn(file.toFile()).when(service).requirePathUnderUploadRoot(file.toString());

        service.extractColumns(mapping);

        Assert.assertEquals(List.of("姓名,别名", "城市"), setting.getColumnNames());
        Assert.assertEquals(List.of("李明,小李", ""), setting.getColumnValues());
        Mockito.verify(service).requirePathUnderUploadRoot(file.toString());
    }

    private Path write(String data, FileSetting setting) throws Exception {
        Path file = this.folder.newFile().toPath();
        return Files.writeString(file, data, Charset.forName(setting.getCharset()));
    }
}
