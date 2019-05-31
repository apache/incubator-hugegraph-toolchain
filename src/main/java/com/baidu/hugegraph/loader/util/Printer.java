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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.LoadContext;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.util.Log;

public final class Printer {

    private static final Logger LOG = Log.logger(Printer.class);

    private static final String DIVIDE_LINE = StringUtils.repeat('-', 42);

    public static void print(Object message) {
        System.out.println(message);
    }

    public static void printElemType(ElemType type) {
        if (type.isVertex()) {
            System.out.print("vertices has been loaded    : 0\b");
        } else {
            assert type.isEdge();
            System.out.print("edges has been loaded       : 0\b");
        }
    }

    /**
     * The print format like
     *
     * vertices has been loaded    : 9923
     * edges has been loaded       : 1000209
     * ------------------------------------------
     * vertices summary
     * counter:
     *     parse failure           : 0
     * 	   insert failure          : 0
     * 	   insert success          : 9923
     * timer:
     * 	   loading time(second)    : 0
     * ------------------------------------------
     * edges summary
     * counter:
     * 	   parse failure           : 0
     * 	   insert failure          : 0
     * 	   insert success          : 1000209
     * timer:
     * 	   loading time(second)    : 18
     * 	   total loading time      : 18
     */
    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        List<LoadMetrics> metricsList = Arrays.asList(summary.vertexMetrics(),
                                                      summary.edgeMetrics());
        // Print vertex and edge metrics
        for (LoadMetrics metrics : metricsList) {
            printAndLog(DIVIDE_LINE);
            printAndLog(String.format("%s summary", metrics.type().string()));
            printAndLog("counter:");
            printAndLog("parse failure", metrics.parseFailure());
            printAndLog("insert failure", metrics.insertFailure());
            printAndLog("insert success", metrics.insertSuccess());
            printAndLog("timer:");
            printAndLog("loading time(second)", metrics.duration().getSeconds());
        }

        // Print total time
        Duration total = metricsList.stream().map(LoadMetrics::duration)
                                    .reduce(Duration::plus).get();
        printAndLog("total loading time", total.getSeconds());
    }

    public static void printError(String message, Object... args) {
        String formattedMsg = String.format("Error: " + message, args);
        LOG.error(formattedMsg);
        // Print an empty line
        System.err.println();
        System.err.println(formattedMsg);
    }

    public static void printProgress(LoadMetrics metrics, long frequency,
                                     int batchSize) {
        long loaded = metrics.insertSuccess();
        Printer.printInBackward(loaded);
        if (loaded % frequency < batchSize) {
            LOG.info("{} has been loaded: {}", metrics.type().string(), loaded);
        }
    }

    public static void printInBackward(long count) {
        System.out.print(String.format("%d%s", count, backward(count)));
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void printAndLog(String desc, long value) {
        String msg = String.format("    %-24s: %-20d", desc, value);
        printAndLog(msg);
    }

    private static String backward(long count) {
        StringBuilder backward = new StringBuilder();
        for (int i = 0, len = String.valueOf(count).length(); i < len; i++) {
            backward.append("\b");
        }
        return backward.toString();
    }
}
