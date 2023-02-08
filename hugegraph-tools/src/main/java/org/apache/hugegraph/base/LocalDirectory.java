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

package org.apache.hugegraph.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.exception.ToolsException;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableList;

public class LocalDirectory extends Directory {

    public LocalDirectory(String directory) {
        super(directory);
    }

    @Override
    public List<String> files() {
        List<String> fileList = new ArrayList<>(8);
        File dir = new File(this.directory());
        String[] files = dir.list();
        if (files == null) {
            return ImmutableList.of();
        }
        for (String f : files) {
            File file = Paths.get(dir.getAbsolutePath(), f).toFile();
            if (file.isFile()) {
                fileList.add(file.getName());
            }
        }
        return fileList;
    }

    @Override
    public void removeDirectory() {
        removeDirectory(this.directory());
    }

    @Override
    public void ensureDirectoryExist(boolean create) {
        ensureDirectoryExist(this.directory(), create);
    }

    @Override
    public String suffix(boolean compress) {
        return compress ? ".zip" : "";
    }

    @Override
    public InputStream inputStream(String file) {
        if (file.endsWith(this.suffix(true))) {
            return this.zipInputStream(file);
        }
        // Keep compatible with version before 1.3.0, which backup data no zip
        return this.textInputStream(file);
    }

    private ZipInputStream zipInputStream(String file) {
        String path = Paths.get(this.directory(), file).toString();
        InputStream is = null;
        ZipInputStream zis;
        try {
            is = new FileInputStream(path);
            zis = new ZipInputStream(is);
            E.checkState(zis.getNextEntry() != null,
                         "Invalid zip file '%s'", file);
        } catch (IOException | IllegalStateException e) {
            closeAndIgnoreException(is);
            throw new ClientException("Failed to read from local file: %s",
                                      e, path);
        }
        return zis;
    }

    private InputStream textInputStream(String file) {
        String path = Paths.get(this.directory(), file).toString();
        try {
            return new FileInputStream(path);
        } catch (IOException e) {
            throw new ClientException("Failed to read from local file: %s",
                                      e, path);
        }
    }

    @Override
    public OutputStream outputStream(String file, boolean compress,
                                     boolean override) {
        String path = Paths.get(this.directory(), file + this.suffix(compress))
                           .toString();
        FileOutputStream os = null;
        ZipOutputStream zos = null;
        try {
            os = new FileOutputStream(path, !override);
            if (!compress) {
                return os;
            }
            zos = new ZipOutputStream(os);
            ZipEntry entry = new ZipEntry(file);
            zos.putNextEntry(entry);
        } catch (IOException e) {
            closeAndIgnoreException(zos);
            closeAndIgnoreException(os);
            throw new ClientException("Failed to write to local file: %s",
                                      e, path);
        }
        return zos;
    }

    public static LocalDirectory constructDir(String directory, String graph) {
        if (directory == null || directory.isEmpty()) {
            directory = "./" + graph;
        }
        return new LocalDirectory(directory);
    }

    public static void ensureDirectoryExist(String directory) {
        ensureDirectoryExist(directory, true);
    }

    private static void ensureDirectoryExist(String directory, boolean create) {
        File file = new File(directory);
        if (file.exists()) {
            E.checkState(file.isDirectory(),
                         "Can't use directory '%s' because a file with " +
                         "same name exists.", file.getAbsolutePath());
        } else {
            if (create) {
                E.checkState(file.mkdirs(),
                             "The directory does not exist and created " +
                             "failed: '%s'", file.getAbsolutePath());
            } else {
                E.checkState(false, "The directory does not exist: '%s'",
                             file.getAbsolutePath());
            }
        }
    }

    private static void removeDirectory(String directory) {
        File dir = new File(directory);
        E.checkState(dir.exists() && dir.isDirectory(),
                     "The directory does not exist: '%s'",
                     dir.getAbsolutePath());
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            throw new ToolsException("Failed to delete directory '%s'",
                                     dir.getAbsolutePath());
        }
    }
}
