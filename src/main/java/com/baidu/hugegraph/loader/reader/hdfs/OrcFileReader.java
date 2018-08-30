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

package com.baidu.hugegraph.loader.reader.hdfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Reader;
import org.apache.hadoop.hive.ql.io.orc.RecordReader;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

/**
 * TODO: Optimize this class
 */
public class OrcFileReader extends HDFSReader {

    private static final Logger LOG = Log.logger(OrcFileReader.class);

    private Iterator<Path> paths;
    private boolean finished;

    private Reader reader;
    private StructObjectInspector inspector;
    private List<String> names;
    private RecordReader records;

    public OrcFileReader(HDFSSource source) {
        super(source);
        this.paths = null;
        this.finished = false;
        this.reader = null;
        this.inspector = null;
        this.names = null;
        this.records = null;
    }

    @Override
    public void init() {
        Path path = new Path(this.source().path());
        FileSystem hdfs = this.fileSystem();
        try {
            if (hdfs.isFile(path)) {
                this.paths = Arrays.asList(path).iterator();
            } else {
                assert hdfs.isDirectory(path);
                FileStatus[] statuses = hdfs.listStatus(path);
                Path[] paths = FileUtil.stat2Paths(statuses);
                this.paths = Arrays.asList(paths).iterator();
            }
        } catch (IOException e) {
            throw new LoadException("");
        }

        this.initNextReader();
    }

    private void initNextReader() {
        if (!this.paths.hasNext()) {
            this.finished = true;
            return;
        }

        Path path = this.paths.next();
        LOG.info("Ready to open HDFS file '{}'", source().path());
        try {
            this.reader = OrcFile.createReader(this.fileSystem(), path);
            this.inspector = (StructObjectInspector)
                             this.reader.getObjectInspector();
            List<? extends StructField> fields = this.inspector
                                                     .getAllStructFieldRefs();
            this.names = fields.stream().map(StructField::getFieldName)
                               .collect(Collectors.toList());
            this.records = this.reader.rows();
        } catch (IOException e) {
            throw new LoadException("Failed to init orc file reader", e);
        }
    }

    @Override
    public Line fetch() {
        if (this.finished) {
            return null;
        }

        try {
            if (!this.records.hasNext()) {
                return null;
            }

            Object row = this.records.next(null);
            if (row == null) {
                this.initNextReader();
                return this.fetch();
            }

            E.checkArgument(row instanceof String,
                            "The orc raw record must be readed as string");
            List<Object> values = this.inspector.getStructFieldsDataAsList(row);
            return new Line((String) row, this.names, values);
        } catch (IOException e) {
            throw new LoadException("Read next line error", e);
        }
    }
}
