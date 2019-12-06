/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.loader.reader.file;

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
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public class OrcReaders extends Readers {

    private static final Logger LOG = Log.logger(OrcReaders.class);

    private final Configuration conf;

    private RecordReader recordReader;
    private StructObjectInspector inspector;
    private Object row;

    public OrcReaders(FileSource source, List<Readable> readables) {
        this(source, readables, new Configuration());
    }

    public OrcReaders(FileSource source, List<Readable> readables,
                      Configuration conf) {
        super(source, readables);
        this.conf = conf;
        this.recordReader = null;
        this.inspector = null;
        this.row = null;
    }

    @Override
    public String[] readHeader() {
        E.checkArgument(this.readables.size() > 0,
                        "Must contain at least one readable file");
        Reader reader = this.openReader(this.readables.get(0));
        StructObjectInspector inspector = (StructObjectInspector)
                                          reader.getObjectInspector();
        this.reset();
        return this.parseHeader(inspector);
    }

    @Override
    public Line readNextLine() throws IOException {
        // Open the first file need to read
        if (this.recordReader == null) {
            this.openNext();
            if (this.recordReader == null) {
                return null;
            }
        } else if (!this.recordReader.hasNext()) {
            return null;
        }

        this.row = this.recordReader.next(this.row);
        String[] names = this.source.header();
        Object[] values = this.inspector.getStructFieldsDataAsList(this.row)
                                        .stream().map(Object::toString)
                                        .toArray();
        String rawLine = StringUtils.join(values, Constants.COMMA_STR);
        return new Line(rawLine, names, values);
    }

    @Override
    public void close() throws IOException {
        if (this.index < this.readables.size()) {
            Readable readable = this.readables.get(this.index);
            LOG.debug("Ready to close '{}'", readable);
        }
        if (this.recordReader != null) {
            this.recordReader.close();
        }
    }

    private void openNext() {
        if (++this.index >= this.readables.size()) {
            return;
        }

        Readable readable = this.readables.get(this.index);
        if (this.checkLoaded(readable)) {
            this.openNext();
        }

        try {
            Reader reader = this.openReader(readable);
            this.recordReader = reader.rows();
            this.inspector = (StructObjectInspector) reader.getObjectInspector();
            this.row = null;
        } catch (IOException e) {
            throw new LoadException("Failed to create orc reader for '%s'",
                                    e, readable);
        }
    }

    private Reader openReader(Readable readable) {
        Path path = new Path(this.source.path());
        try {
            return OrcFile.createReader(path, OrcFile.readerOptions(this.conf));
        } catch (IOException e) {
            throw new LoadException("Failed to open orcReader for '%s'",
                                    e, readable);
        }
    }

    private String[] parseHeader(StructObjectInspector inspector) {
        List<? extends StructField> fields = inspector.getAllStructFieldRefs();
        return fields.stream().map(StructField::getFieldName)
                     .collect(Collectors.toList())
                     .toArray(new String[]{});
    }
}
