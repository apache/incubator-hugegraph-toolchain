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

package org.apache.hugegraph.loader.failure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.InsertException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.exception.ParseException;
import org.apache.hugegraph.loader.exception.ReadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.util.LoadUtil;
import org.apache.hugegraph.util.JsonUtil;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public final class FailLogger {

    private static final Logger LOG = Log.logger(FailLogger.class);

    private final LoadOptions options;
    private final InputStruct struct;
    private final File file;
    private final FailWriter writer;

    public FailLogger(LoadContext context, InputStruct struct) {
        this.options = context.options();
        this.struct = struct;
        String prefix = LoadUtil.getStructDirPrefix(context.options());
        String parentDir = Paths.get(prefix, Constants.FAILURE_DATA).toString();
        // The files under failure path are like:
        // mapping/failure-data/input-1.error
        String fileName = this.struct.id() + Constants.FAILURE_SUFFIX;
        boolean append;
        if (this.options.incrementalMode) {
            // Append to existed file
            append = true;
        } else if (this.options.failureMode) {
            // Write to a temp file first, then rename
            fileName += Constants.TEMP_SUFFIX;
            append = false;
        } else {
            // Overwrite the origin file
            append = false;
        }
        String savePath = Paths.get(parentDir, fileName).toString();
        LOG.info("The failure data path of input struct {} is at {}: ",
                 struct.id(), savePath);
        this.file = new File(savePath);
        String charset = this.struct.input().charset();
        this.writer = new FailWriter(this.file, charset, append);
    }

    public void write(ReadException e) {
        this.writer.write(e);
    }

    public void write(ParseException e) {
        this.writer.write(e);
    }

    public void write(InsertException e) {
        this.writer.write(e);
    }

    public void close() {
        this.writeHeaderIfNeeded();
        this.writer.close();

        if (this.file.length() == 0) {
            LOG.debug("The file {} is empty, delete it", this.file);
            this.file.delete();
        } else {
            this.removeDupLines();
            this.renameTempFile();
        }
    }

    /**
     * Write head to a specialized file, every input struct has one
     */
    private void writeHeaderIfNeeded() {
        // header() == null means no need header
        if (this.struct.input().header() == null) {
            return;
        }
        String header = JsonUtil.toJson(this.struct.input().header());
        /*
         * The files under failure path are like:
         * mapping/failure-data/input-1.header
         */
        String fileName = this.struct.id() + Constants.HEADER_SUFFIX;
        String filePath = Paths.get(this.file.getParent(), fileName).toString();
        File headerFile = new File(filePath);
        String charset = this.struct.input().charset();
        try {
            FileUtils.writeStringToFile(headerFile, header, charset);
        } catch (IOException e) {
            throw new LoadException("Failed to write header '%s'", e);
        }
    }

    private void removeDupLines() {
        Charset charset = Charset.forName(this.struct.input().charset());
        File dedupFile = new File(this.file.getAbsolutePath() + Constants.DEDUP_SUFFIX);
        try (InputStream is = Files.newInputStream(this.file.toPath());
             Reader ir = new InputStreamReader(is, charset);
             BufferedReader reader = new BufferedReader(ir);
             // upper is input, below is output
             OutputStream os = Files.newOutputStream(dedupFile.toPath());
             Writer ow = new OutputStreamWriter(os, charset);
             BufferedWriter writer = new BufferedWriter(ow)) {
            Set<Integer> wroteLines = new HashSet<>();
            HashFunction hashFunc = Hashing.murmur3_32();
            for (String tipsLine, dataLine; (tipsLine = reader.readLine()) != null &&
                                            (dataLine = reader.readLine()) != null; ) {
                /*
                 * Hash data line to remove duplicate lines
                 * Misjudgment may occur, but the probability is extremely low
                 */
                int hash = hashFunc.hashString(dataLine, charset).asInt();
                if (!wroteLines.contains(hash)) {
                    writer.write(tipsLine);
                    writer.newLine();
                    writer.write(dataLine);
                    writer.newLine();
                    // Save the hash value of wrote line
                    wroteLines.add(hash);
                }
            }
        } catch (IOException e) {
            throw new LoadException("Failed to remove duplicate lines");
        }
        if (!dedupFile.renameTo(this.file)) {
            throw new LoadException("Failed to rename dedup file to origin");
        }
    }

    private void renameTempFile() {
        // Renamed file if needed
        boolean needRename = this.options.failureMode;
        if (needRename) {
            String fileName = this.file.getAbsolutePath();
            int idx = fileName.lastIndexOf(Constants.TEMP_SUFFIX);
            String destName = fileName.substring(0, idx);
            if (!this.file.renameTo(new File(destName))) {
                LOG.warn("Failed to rename failure data file {} to {}",
                         fileName, destName);
            }
        }
    }
}
