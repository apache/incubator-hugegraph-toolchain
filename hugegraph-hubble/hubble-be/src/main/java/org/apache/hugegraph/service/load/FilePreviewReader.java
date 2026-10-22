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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.loader.parser.CsvLineParser;
import org.apache.hugegraph.loader.parser.LineParser;
import org.apache.hugegraph.loader.parser.TextLineParser;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.util.Ex;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a header and one physical data line using the loader's delimiter semantics.
 */
public final class FilePreviewReader {

    private static final ObjectMapper JSON_MAPPER =
            new ObjectMapper().disable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);

    private FilePreviewReader() {
    }

    public static Columns read(Path file, FileSetting setting) {
        Charset charset = Charset.forName(setting.getCharset());
        Pattern skippedLine = Pattern.compile(setting.getSkippedLine());
        FileFormat format = FileFormat.valueOf(setting.getFormat());
        // Upload previews run before users can select the encoding. Preserve undecodable
        // bytes as replacement characters so the file settings can still be corrected.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), charset))) {
            String firstLine = nextDataLine(reader, skippedLine);
            Ex.check(firstLine != null, "The file has no data line can treat as header");
            return switch (format) {
                case CSV -> readDelimited(reader, firstLine, skippedLine,
                                          new CsvLineParser(), setting.isHasHeader());
                case TEXT -> readDelimited(reader, firstLine, skippedLine,
                                           new TextLineParser(setting.getDelimiter()), setting.isHasHeader());
                case JSON -> readJson(firstLine, file);
            };
        } catch (IOException e) {
            throw new InternalException("Failed to read columns from file %s", e, file);
        }
    }

    private static Columns readDelimited(BufferedReader reader, String firstLine,
                                         Pattern skippedLine, LineParser parser,
                                         boolean hasHeader) throws IOException {
        List<String> firstValues = Arrays.asList(parser.split(firstLine));
        if (hasHeader) {
            String sample = nextDataLine(reader, skippedLine);
            List<String> values = sample == null ? List.of() : Arrays.asList(parser.split(sample));
            return new Columns(firstValues, values);
        }
        List<String> names = new ArrayList<>(firstValues.size());
        for (int i = 1; i <= firstValues.size(); i++) {
            names.add("col-" + i);
        }
        return new Columns(names, firstValues);
    }

    private static String nextDataLine(BufferedReader reader, Pattern skippedLine) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && !skippedLine.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    private static Columns readJson(String line, Path file) {
        try {
            Map<String, Object> fields = JSON_MAPPER.readValue(
                    line, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            List<String> names = new ArrayList<>(fields.keySet());
            List<String> values = names.stream().map(fields::get).map(String::valueOf).toList();
            return new Columns(names, values);
        } catch (IOException ignored) {
            // Parser errors can contain imported values; keep them out of responses and logs.
            throw new InternalException("Failed to read JSON fields from file %s", file);
        }
    }

    public record Columns(List<String> names, List<String> values) {

        public Columns {
            names = List.copyOf(names);
            values = List.copyOf(values);
        }
    }
}
