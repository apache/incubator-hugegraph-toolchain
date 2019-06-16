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

package com.baidu.hugegraph.loader.util;

import java.time.Duration;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.executor.LoadSummary;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.JCommander;

public final class LoaderUtil {

    private static final Logger LOG = Log.logger(LoaderUtil.class);

    public static void exitWithUsage(JCommander commander, int status) {
        commander.usage();
        System.exit(status);
    }

    public static void printError(String message, Object... args) {
        String formattedMsg = String.format(message, args);
        LOG.error(formattedMsg);
        // Print an empty line.
        System.err.println();
        System.err.println(formattedMsg);
    }

    public static void printSummary(LoadSummary... summaries) {
        // Print count
        for (LoadSummary summary : summaries) {
            String type = summary.type();
            logAndPrint("---------------------------------------------");
            logAndPrint(String.format("%s results:", type));
            logAndPrint(String.format("parse failure %s", type),
                        summary.parseFailure());
            logAndPrint(String.format("parse success %s", type),
                        summary.parserSuccess());
            logAndPrint(String.format("insert failure %s", type),
                        summary.insertFailure());
            logAndPrint(String.format("insert success %s", type),
                        summary.insertSuccess());
            logAndPrint("insert speed per second", summary.averageSpeed());
        }
        // Print time
        logAndPrint("---------------------------------------------");
        logAndPrint("time results:");
        for (LoadSummary summary : summaries) {
            String type = summary.type();
            logAndPrint(String.format("%s loading time", type),
                        summary.loadTime().getSeconds());
        }
        // Print total time
        Duration totalTime = Duration.ZERO;
        for (LoadSummary summary : summaries) {
            totalTime = totalTime.plus(summary.loadTime());
        }
        logAndPrint("total loading time", totalTime.getSeconds());
    }

    private static void logAndPrint(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void logAndPrint(String desc, long value) {
        String msg = String.format("\t%-25s:\t%-20d", desc, value);
        logAndPrint(msg);
    }

    public static void printInBackward(long count) {
        System.out.print(String.format("%d%s", count,
                         LoaderUtil.backward(count)));
    }

    public static String backward(long word) {
        StringBuilder backward = new StringBuilder();
        for (int i = 0, len = String.valueOf(word).length(); i < len; i++) {
            backward.append("\b");
        }
        return backward.toString();
    }
}
