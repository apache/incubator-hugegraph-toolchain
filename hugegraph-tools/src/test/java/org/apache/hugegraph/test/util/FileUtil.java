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

package org.apache.hugegraph.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.exception.ToolsException;

import com.google.common.collect.Lists;

public class FileUtil {

    protected static final int LBUF_SIZE = 1024;
    protected static final String CONFIG_PATH = "src/test/resources";

    public static String configPath(String fileName) {
        return Paths.get(CONFIG_PATH, fileName).toString();
    }

    public static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public static List<String> subdirectories(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return ListUtils.EMPTY_LIST;
        }
        String[] files = file.list();
        List<String> list = Lists.newArrayList();
        for (int i = 0; i < files.length; i++) {
             File fileDir = new File(file, files[i]);
             list.add(fileDir.getName());
        }

        return list;
    }

    public static void clearDirectories(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                File fileDir = new File(file, files[i]);
                fileDir.delete();
            }
        }
    }

    public static long writeTestRestoreData(String filePath, List<?> list) {
        long count = 0L;
        try (FileOutputStream os = new FileOutputStream(filePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE)) {
             StringBuilder builder = new StringBuilder(LBUF_SIZE);
             for (Object e : list) {
                  count++;
                  builder.append(e).append("\n");
             }
             baos.write(builder.toString().getBytes(API.CHARSET));
             os.write(baos.toByteArray());
        } catch (IOException e) {
             throw new ToolsException("Failed write file path is %s",
                                      e, filePath);
        }

        return count;
    }

    public static List<String> readTestRestoreData(String filePath) {
        List<String> results = Lists.newArrayList();
        try (InputStream is = new FileInputStream(filePath);
             InputStreamReader isr = new InputStreamReader(is, API.CHARSET)) {
             BufferedReader reader = new BufferedReader(isr);
             String line;
             while ((line = reader.readLine()) != null) {
                 results.add(line);
             }
        } catch (IOException e) {
             throw new ToolsException("Failed read file path is %s",
                                      e, filePath);
        }

        return results;
    }
}
