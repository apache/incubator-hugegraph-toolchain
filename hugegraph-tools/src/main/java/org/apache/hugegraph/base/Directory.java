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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.hugegraph.util.E;

public abstract class Directory {

    private final String directory;

    public Directory(String directory) {
        E.checkArgument(directory != null && !directory.isEmpty(),
                        "Directory can't be null or empty");
        this.directory = directory;
    }

    public String directory() {
        return this.directory;
    }

    public abstract List<String> files();

    public abstract String suffix(boolean compress);

    public abstract void ensureDirectoryExist(boolean create);

    public abstract void removeDirectory();

    public abstract InputStream inputStream(String path);

    public abstract OutputStream outputStream(String path, boolean compress,
                                              boolean override);

    public static void closeAndIgnoreException(Closeable stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception ignored) {
            // Ignore
        }
    }
}
