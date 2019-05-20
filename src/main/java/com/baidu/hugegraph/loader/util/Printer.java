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

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.util.Log;

public final class Printer {

    private static final Logger LOG = Log.logger(Printer.class);

    private static final String DIVIDE_LINE =
            "----------------------------------------------------";

    public static void print(Object message) {
        System.out.println(message);
    }

    public static void printElemType(ElemType type) {
        if (type.isVertex()) {
            System.out.print("vertices has been loaded     :  0\b");
        } else {
            assert type.isEdge();
            System.out.print("edges has been loaded        :  0\b");
        }
    }

    public static void printSummary(LoadSummary... summaries) {
        // Print count
        for (LoadSummary summary : summaries) {
            String type = summary.type();
            printAndLog(DIVIDE_LINE);
            printAndLog(String.format("%s summary:", type));
            printAndLog(String.format("parse failure %s", type),
                        summary.parseFailure());
            printAndLog(String.format("insert failure %s", type),
                        summary.insertFailure());
            printAndLog(String.format("insert success %s", type),
                        summary.insertSuccess());
            printAndLog("insert speed per second", summary.averageSpeed());
        }
        // Print time
        printAndLog(DIVIDE_LINE);
        printAndLog("time summary:");
        for (LoadSummary summary : summaries) {
            String type = summary.type();
            printAndLog(String.format("%s loading time", type),
                        summary.loadTime().getSeconds());
        }
        // Print total time
        Duration totalTime = Duration.ZERO;
        for (LoadSummary summary : summaries) {
            totalTime = totalTime.plus(summary.loadTime());
        }
        printAndLog("total loading time", totalTime.getSeconds());
    }

    public static void printError(String message, Object... args) {
        String formattedMsg = String.format(message, args);
        LOG.error(formattedMsg);
        // Print an empty line
        System.err.println();
        System.err.println(formattedMsg);
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void printAndLog(String desc, long value) {
        String msg = String.format("\t%-25s:\t%-20d", desc, value);
        printAndLog(msg);
    }

    public static void printInBackward(long count) {
        System.out.print(String.format("%d%s", count, backward(count)));
    }

    private static String backward(long count) {
        StringBuilder backward = new StringBuilder();
        for (int i = 0, len = String.valueOf(count).length(); i < len; i++) {
            backward.append("\b");
        }
        return backward.toString();
    }
}
