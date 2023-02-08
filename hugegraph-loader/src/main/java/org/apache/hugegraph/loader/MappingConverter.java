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

package org.apache.hugegraph.loader;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;

public final class MappingConverter {

    private static final Logger LOG = Log.logger(MappingConverter.class);

    public static void main(String[] args) {
        E.checkArgument(args.length == 1, "args: file");
        String input = args[0];
        LOG.info("Prepare to convert mapping file {}", input);

        File file = FileUtils.getFile(input);
        if (!file.exists() || !file.isFile()) {
            LOG.error("The file '{}' doesn't exists or not a file", input);
            throw new IllegalArgumentException(String.format("The file '%s' doesn't exists or " +
                                                             "not a file", input));
        }

        LoadMapping mapping = LoadMapping.of(input);
        String outputPath = getOutputPath(file);
        MappingUtil.write(mapping, outputPath);
        LOG.info("Convert mapping file successfully, stored at {}", outputPath);
    }

    public static String getOutputPath(File file) {
        String fileName = file.getName();
        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = prefix + "-v2" + suffix;
        if (file.getParent() != null) {
            return Paths.get(file.getParent(), newFileName).toString();
        } else {
            return newFileName;
        }
    }
}
