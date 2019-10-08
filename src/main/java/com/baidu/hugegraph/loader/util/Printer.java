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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.TimeUtil;

public final class Printer {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);

    private static final String EMPTY_LINE = "";
    private static final String DIVIDE_LINE = StringUtils.repeat('-', 50);

    public static void print(Object message) {
        System.out.println(message);
    }

    public static void printRealTimeProgress(ElemType type, long count) {
        String lastLoadMsg = String.format("%s has been loaded last time : %s",
                                           type.string(), count);
        String thisLoadMsg = String.format("%s has been loaded : 0\b",
                                           type.string());
        if (count > 0) {
            printAndLog(lastLoadMsg);
        }
        System.out.print(thisLoadMsg);
    }

    /**
     * The print format like
     *
     * --------------------------------------------------
     * total vertex metrics
     * parse:
     *     parse success           : 9923
     *     parse failure           : 0
     *     parse time              : 0.239s
     *     parse rate              : 41518(vertices/s)
     * load:
     *     load success            : 9923
     *     load failure            : 0
     *     load time               : 0.375s
     *     load rate               : 26461(vertices/s)
     * --------------------------------------------------
     * total edge metrics
     * parse:
     *     parse success           : 1000209
     *     parse failure           : 0
     *     parse time              : 2.994s
     *     parse rate              : 334071(edges/s)
     * load:
     *     load success            : 1000209
     *     load failure            : 0
     *     load time               : 19.688s
     *     load rate               : 50802(edges/s)
     */
    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        // Just log vertices/edges metrics
        log(DIVIDE_LINE);
        log("vertices metrics");
        summary.vertexMetrics().forEach((uniqueKey, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("struct '%s'", uniqueKey));
            logMetrics(ElemType.VERTEX, metrics);
        });
        log(DIVIDE_LINE);
        log("edges metrics");
        summary.edgeMetrics().forEach((uniqueKey, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("struct '%s'", uniqueKey));
            logMetrics(ElemType.EDGE, metrics);
        });

        // Print and log total vertices/edges metrics
        printAndLog(DIVIDE_LINE);
        printAndLog("total vertex metrics");
        printMetrics(ElemType.VERTEX, summary.accumulateMetrics(ElemType.VERTEX));
        printAndLog(DIVIDE_LINE);
        printAndLog("total edge metrics");
        printMetrics(ElemType.EDGE, summary.accumulateMetrics(ElemType.EDGE));
    }

    private static void logMetrics(ElemType type, LoadMetrics metrics) {
        log("parse success", metrics.parseSuccess());
        log("parse failure", metrics.parseFailure());
        log("parse time", TimeUtil.readableTime(metrics.parseTime()));
        log("parse rate", String.format("%s(%s/s)", metrics.parseRate(),
                                                    type.string()));
        log("load success", metrics.loadSuccess());
        log("load failure", metrics.loadFailure());
        log("load time", TimeUtil.readableTime(metrics.loadTime()));
        log("load rate", String.format("%s(%s/s)", metrics.averageLoadRate(),
                                                   type.string()));
    }

    private static void printMetrics(ElemType type, LoadMetrics metrics) {
        printAndLog("parse:");
        // Print parse success used to comfirm data integrity
        printAndLog("parse success", metrics.parseSuccess());
        printAndLog("parse failure", metrics.parseFailure());
        printAndLog("parse time", TimeUtil.readableTime(metrics.parseTime()));
        printAndLog("parse rate", String.format("%s(%s/s)", metrics.parseRate(),
                                                            type.string()));

        printAndLog("load:");
        printAndLog("load success", metrics.loadSuccess());
        printAndLog("load failure", metrics.loadFailure());
        printAndLog("load time", TimeUtil.readableTime(metrics.loadTime()));
        printAndLog("load rate", String.format("%s(%s/s)", metrics.loadRate(),
                                                           type.string()));
    }

    public static void printError(String message, Object... args) {
        printError(message, null, args);
    }

    public static void printError(String message, Throwable e, Object... args) {
        String formattedMsg = String.format("ERROR: " + message, args);
        if (e != null) {
            LOG.error(formattedMsg, e);
        } else {
            LOG.error(formattedMsg);
        }
        // Print an empty line
        System.err.println();
        System.err.println(formattedMsg);
    }

    public static void printProgress(ElemType type, long loaded,
                                     long frequency, int batchSize) {
        Printer.printInBackward(loaded);
        if (loaded % frequency < batchSize) {
            LOG.info("{} has been loaded: {}", type.string(), loaded);
        }
    }

    public static void printInBackward(long count) {
        // format "%d%s"
        System.out.print(count + backward(count));
    }

    private static void log(String message) {
        LOG.info(message);
    }

    private static void log(String key, long value) {
        String msg = String.format("    %-24s: %-20d", key, value);
        LOG.info(msg);
    }

    private static void log(String key, String value) {
        String msg = String.format("    %-24s: %-20s", key, value);
        LOG.info(msg);
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void printAndLog(String key, long value) {
        String msg = String.format("    %-24s: %-20d", key, value);
        printAndLog(msg);
    }

    private static void printAndLog(String key, String value) {
        String msg = String.format("    %-24s: %-20s", key, value);
        printAndLog(msg);
    }

    private static String backward(long count) {
        return StringUtils.repeat('\b', String.valueOf(count).length());
    }
}
