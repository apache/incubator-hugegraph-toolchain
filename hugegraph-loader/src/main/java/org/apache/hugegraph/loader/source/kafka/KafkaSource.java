/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.loader.source.kafka;

import java.util.List;

import org.apache.hugegraph.loader.source.AbstractSource;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.file.SkippedLine;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KafkaSource extends AbstractSource {

    @JsonProperty("bootstrap_server")
    private String bootstrapServer;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("group")
    private String group;

    @JsonProperty("from_beginning")
    private boolean fromBeginning = false;

    private FileFormat format;

    @JsonProperty("delimiter")
    private String delimiter;

    @JsonProperty("date_format")
    private String dateFormat;

    @JsonProperty("extra_date_formats")
    private List<String> extraDateFormats;

    @JsonProperty("time_zone")
    private String timeZone;

    @JsonProperty("skipped_line")
    private SkippedLine skippedLine;

    @JsonProperty("batch_size")
    private int batchSize = 500;

    @JsonProperty("early_stop")
    private boolean earlyStop = false;

    @Override
    public SourceType type() {
        return SourceType.KAFKA;
    }

    @Override
    public FileSource asFileSource() {
        FileSource source = new FileSource();
        source.header(this.header());
        source.charset(this.charset());
        source.listFormat(this.listFormat());
        return source;
    }
}
