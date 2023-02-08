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

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.util.ParquetUtil;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.util.Log;

public class ParquetFileLineFetcher extends FileLineFetcher {

    private static final Logger LOG = Log.logger(ParquetFileLineFetcher.class);

    private final Configuration conf;

    private ParquetFileReader reader;
    private MessageType schema;
    private MessageColumnIO columnIO;
    private RecordReader<?> recordReader;
    private PageReadStore pages;
    private long pagesRowCount;
    private long currRowOffset;

    public ParquetFileLineFetcher(FileSource source) {
        this(source, new Configuration());
    }

    public ParquetFileLineFetcher(FileSource source, Configuration conf) {
        super(source);
        this.conf = conf;
        this.resetReader();
    }

    @Override
    public boolean ready() {
        return this.reader != null;
    }

    @Override
    public void resetReader() {
        this.reader = null;
        this.schema = null;
        this.columnIO = null;
        this.pages = null;
        this.pagesRowCount = -1L;
        this.currRowOffset = -1L;
    }

    @Override
    public boolean needReadHeader() {
        return true;
    }

    @Override
    public String[] readHeader(List<Readable> readables) {
        Readable readable = readables.get(0);
        this.openReader(readables.get(0));
        try {
            return this.parseHeader(this.schema);
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
            HadoopInputFile file = HadoopInputFile.fromPath(path, this.conf);
            this.reader = ParquetFileReader.open(file);
            this.schema = this.reader.getFooter().getFileMetaData().getSchema();
            this.columnIO = new ColumnIOFactory().getColumnIO(this.schema);
        } catch (IOException e) {
            throw new LoadException("Failed to open parquet reader for '%s'",
                                    e, readable);
        }
        this.resetOffset();
    }

    @Override
    public Line fetch() {
        boolean needFetchNext = this.pages == null ||
                                this.currRowOffset >= this.pagesRowCount;
        // Read next row group
        if (needFetchNext && !this.fetchNextPage()) {
            return null;
        }

        int fieldSize = this.schema.getFields().size();
        Object[] values = new Object[fieldSize];
        SimpleGroup group = (SimpleGroup) this.recordReader.read();
        for (int fieldIndex = 0; fieldIndex < fieldSize; fieldIndex++) {
            values[fieldIndex] = ParquetUtil.convertObject(group, fieldIndex);
        }
        String rawLine = StringUtils.join(values, Constants.COMMA_STR);

        this.currRowOffset++;
        this.increaseOffset();
        /*
         * NOTE: parquet file actually corresponds to a table structure,
         * doesn't need to skip line or match header
         */
        return new Line(rawLine, this.source().header(), values);
    }

    @Override
    public void closeReader() throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
    }

    private boolean fetchNextPage() {
        try {
            this.pages = this.reader.readNextRowGroup();
            if (this.pages == null) {
                return false;
            }
        } catch (IOException e) {
            throw new LoadException("Failed to read next page for '%s'", e);
        }
        GroupRecordConverter converter = new GroupRecordConverter(this.schema);
        this.recordReader = this.columnIO.getRecordReader(this.pages,
                                                          converter);
        this.pagesRowCount = this.pages.getRowCount();
        this.currRowOffset = 0L;
        return this.currRowOffset < this.pagesRowCount;
    }

    private String[] parseHeader(MessageType schema) {
        List<Type> fields = schema.getFields();
        String[] header = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            header[i] = fields.get(i).getName();
        }
        return header;
    }
}
