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

public final class Printer {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);

    private static final String EMPTY_LINE = "";
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
     * ------------------------------------------
     * vertices metrics
     *
     * struct 'user-e6d5731c'
     * parse:
     *     parse success           : 6040
     *     parse failure           : 0
     *     parse time(second)      : 0
     * load:
     *     load success            : 6040
     *     load failure            : 0
     *     load time(second)       : 0
     *
     * struct 'movie-a917bbd1'
     * parse:
     *     parse success           : 3883
     *     parse failure           : 0
     *     parse time(second)      : 0
     * load:
     *     load success            : 3883
     *     load failure            : 0
     *     load time(second)       : 0
     * ------------------------------------------
     * edges metrics
     *
     * struct 'rating-6ec4d883'
     * parse:
     *     parse success           : 1000209
     *     parse failure           : 0
     *     parse time(second)      : 3
     *     parse rate              : 333403
     * load:
     *     load success            : 1000209
     *     load failure            : 0
     *     load time(second)       : 17
     *     load rate               : 58835
     * ------------------------------------------
     * total metrics
     * parse:
     *     parse success           : 1010132
     *     parse failure           : 0
     *     parse time(second)      : 3
     *     parse rate              : 336710
     * load:
     *     load success            : 1010132
     *     load failure            : 0
     *     load time(second)       : 17
     *     load rate               : 59419
     */
    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        // Print vertices and edges metrics
        printAndLog(DIVIDE_LINE);
        printAndLog(String.format("%s metrics", ElemType.VERTEX.string()));
        summary.vertexMetrics().forEach((uniqueKey, metrics) -> {
            printAndLog(EMPTY_LINE);
            printAndLog(String.format("struct '%s'", uniqueKey));
            printMetrics(metrics);
        });
        printAndLog(DIVIDE_LINE);
        printAndLog(String.format("%s metrics", ElemType.EDGE.string()));
        summary.edgeMetrics().forEach((uniqueKey, metrics) -> {
            printAndLog(EMPTY_LINE);
            printAndLog(String.format("struct '%s'", uniqueKey));
            printMetrics(metrics);
        });

        // Print total metrics
        LoadMetrics totalMetrics = summary.accumulateAllMetrics();
        printAndLog(DIVIDE_LINE);
        printAndLog("total metrics");
        printMetrics(totalMetrics);
    }

    private static void printMetrics(LoadMetrics metrics) {
        printAndLog("parse:");
        printAndLog("parse success", metrics.parseSuccess());
        printAndLog("parse failure", metrics.parseFailure());
        printAndLog("parse time(second)", metrics.parseTime());
        if (metrics.parseTime() != 0) {
            printAndLog("parse rate", metrics.parseRate());
        }

        printAndLog("load:");
        printAndLog("load success", metrics.loadSuccess());
        printAndLog("load failure", metrics.loadFailure());
        printAndLog("load time(second)", metrics.loadTime());
        if (metrics.loadTime() != 0) {
            printAndLog("load rate", metrics.loadRate());
        }
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
        System.out.print(String.format("%d%s", count, backward(count)));
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void printAndLog(String key, long value) {
        String msg = String.format("    %-24s: %-20d", key, value);
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
