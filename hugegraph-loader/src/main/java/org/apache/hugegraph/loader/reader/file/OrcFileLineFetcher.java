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

package org.apache.hugegraph.loader.reader.file;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Reader;
import org.apache.hadoop.hive.ql.io.orc.RecordReader;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.util.Log;

public class OrcFileLineFetcher extends FileLineFetcher {

    private static final Logger LOG = Log.logger(OrcFileLineFetcher.class);

    private final Configuration conf;

    private Reader reader;
    private RecordReader recordReader;
    private StructObjectInspector inspector;
    private Object row;

    public OrcFileLineFetcher(FileSource source) {
        this(source, new Configuration());
    }

    public OrcFileLineFetcher(FileSource source, Configuration conf) {
        super(source);
        this.conf = conf;
        this.reader = null;
        this.recordReader = null;
        this.inspector = null;
        this.row = null;
    }

    @Override
    public boolean ready() {
        return this.reader != null;
    }

    @Override
    public void resetReader() {
        this.reader = null;
        this.recordReader = null;
        this.inspector = null;
        this.row = null;
    }

    @Override
    public boolean needReadHeader() {
        return true;
    }

    @Override
    public String[] readHeader(List<Readable> readables) {
        Readable readable = readables.get(0);
        this.openReader(readable);
        StructObjectInspector inspector;
        try {
            inspector = (StructObjectInspector) this.reader.getObjectInspector();
            return this.parseHeader(inspector);
        } finally {
            try {
                this.closeReader();
            } catch (IOException e) {
                LOG.warn("Failed to close reader of '{}'", readable);
            }
        }
    }

    @Override
    public void openReader(Readable readable) {
        Path path = readable.path();
        try {
            OrcFile.ReaderOptions options = OrcFile.readerOptions(this.conf);
            this.reader = OrcFile.createReader(path, options);
            this.recordReader = this.reader.rows();
            this.inspector = (StructObjectInspector) this.reader.getObjectInspector();
            this.row = null;
        } catch (IOException e) {
            throw new LoadException("Failed to open orc reader for '%s'",
                                    e, readable);
        }
        this.resetOffset();
    }

    @Override
    public Line fetch() throws IOException {
        // Read next line from current file
        if (!this.recordReader.hasNext()) {
            return null;
        }

        this.row = this.recordReader.next(this.row);
        List<Object> data = this.inspector.getStructFieldsDataAsList(this.row);
        Object[] values = new Object[data.size()];
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == null) {
                values[i] = "";
            } else {
                values[i] = data.get(i).toString();
            }
        }
        String rawLine = StringUtils.join(values, Constants.COMMA_STR);

        this.increaseOffset();
        /*
         * NOTE: orc file actually corresponds to a table structure,
         * doesn't need to skip line or match header
         */
        return new Line(rawLine, this.source().header(), values);
    }

    @Override
    public void closeReader() throws IOException {
        if (this.recordReader != null) {
            this.recordReader.close();
        }
    }

    private String[] parseHeader(StructObjectInspector inspector) {
        List<? extends StructField> fields = inspector.getAllStructFieldRefs();
        return fields.stream().map(StructField::getFieldName)
                     .collect(Collectors.toList())
                     .toArray(new String[]{});
    }
}
