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

package org.apache.hugegraph.loader.source.afs;

import java.util.List;

import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.loader.source.file.Compression;
import org.apache.hugegraph.loader.source.file.DirFilter;
import org.apache.hugegraph.loader.source.file.FileFilter;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.file.SkippedLine;
import org.apache.hugegraph.loader.source.hdfs.HDFSSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AFSSource extends HDFSSource {

    @JsonCreator
    public AFSSource(@JsonProperty("path") String path,
                     @JsonProperty("dir_filter") DirFilter dirFilter,
                     @JsonProperty("filter") FileFilter filter,
                     @JsonProperty("format") FileFormat format,
                     @JsonProperty("delimiter") String delimiter,
                     @JsonProperty("date_format") String dateFormat,
                     @JsonProperty("extra_date_formats")
                             List<String> extraDateFormats,
                     @JsonProperty("time_zone") String timeZone,
                     @JsonProperty("skipped_line") SkippedLine skippedLine,
                     @JsonProperty("compression") Compression compression,
                     @JsonProperty("batch_size") Integer batchSize) {
        super(path, dirFilter, filter, format, delimiter, dateFormat,
              extraDateFormats, timeZone, skippedLine, compression, batchSize);
    }

    @Override
    public SourceType type() {
        return SourceType.AFS;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
    }

}
