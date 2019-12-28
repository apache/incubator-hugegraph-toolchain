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

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.metrics.LoadReport;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.TimeUtil;

public final class Printer {

    private static final Logger LOG = Log.logger(Printer.class);

    private static final String EMPTY_LINE = "";
    private static final String SLASH = "/";
    private static final String DIVIDE_LINE = StringUtils.repeat('-', 50);

    public static void printRealtimeProgress(LoadContext context) {
        long lastVertexLoaded = LoadUtil.lastLoaded(context, ElemType.VERTEX);
        long lastEdgeLoaded = LoadUtil.lastLoaded(context, ElemType.EDGE);
        if (lastVertexLoaded > 0 || lastEdgeLoaded > 0) {
            printAndLog("vertices/edges has been loaded last time : " +
                        lastVertexLoaded + SLASH + lastEdgeLoaded);
        }
        System.out.print("vertices/edges has been loaded this time : ");
    }

    public static void printFinalProgress(LoadContext context) {
        LoadSummary summary = context.summary();
        long vertexLoaded = summary.buildReport(ElemType.VERTEX).loadSuccess();
        long edgeLoaded = summary.buildReport(ElemType.EDGE).loadSuccess();
        System.out.println(vertexLoaded + SLASH + edgeLoaded);
    }

    /**
     *
     */
    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        // Just log vertices/edges metrics
        log(DIVIDE_LINE);
        log("vertices metrics");
        summary.vertexMetrics().forEach((label, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("label '%s'", label));
            logReport(metrics.buildReport());
        });
        log(DIVIDE_LINE);
        log("edges metrics");
        summary.edgeMetrics().forEach((label, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("label '%s'", label));
            logReport(metrics.buildReport());
        });

        // Print and log total vertices/edges metrics
        printAndLog(DIVIDE_LINE);
        printAndLog("integral vertex metrics");
        printReport(ElemType.VERTEX, summary.buildReport(ElemType.VERTEX));
        printAndLog(DIVIDE_LINE);
        printAndLog("integral edge metrics");
        printReport(ElemType.EDGE, summary.buildReport(ElemType.EDGE));
    }

    private static void logReport(LoadReport report) {
        log("parse success", report.parseSuccess());
        log("parse failure", report.parseFailure());
        log("load success", report.loadSuccess());
        log("load failure", report.loadFailure());
    }

    private static void printReport(ElemType type, LoadReport report) {
        printAndLog("total time", TimeUtil.readableTime(report.totalTime()));
        // Print parse success used to comfirm data integrity
        printAndLog("parse success", report.parseSuccess());
        printAndLog("parse failure", report.parseFailure());
        printAndLog("parse rate", String.format("%s(%s/s)", report.parseRate(),
                                                            type.string()));
        printAndLog("load success", report.loadSuccess());
        printAndLog("load failure", report.loadFailure());
        printAndLog("load rate", String.format("%s(%s/s)", report.loadRate(),
                                                           type.string()));
    }

    public static void printError(String message, Object... args) {
        String formatMsg = String.format(message, args);
        LOG.error(formatMsg);
        // Print an empty line
        System.err.println();
        System.err.println(formatMsg);
    }

    public static void printError(String message, Throwable e) {
        String formatMsg = String.format("%s, cause: %s",
                                         message, e.getMessage());
        LOG.error(formatMsg, e);
        // Print an empty line
        System.err.println();
        System.err.println(formatMsg);
    }

    public static void printProgress(LoadSummary summary, long frequency,
                                     int batchSize) {
        long vertexLoaded = summary.vertexLoaded();
        long edgeLoaded = summary.edgeLoaded();
        Printer.printInBackward(vertexLoaded, edgeLoaded);
        if (vertexLoaded % frequency < batchSize) {
            LOG.info("vertices has been loaded: {}", vertexLoaded);
        }
        if (edgeLoaded % frequency < batchSize) {
            LOG.info("edges has been loaded: {}", edgeLoaded);
        }
    }

    private static void printInBackward(long vertexLoaded, long edgeLoaded) {
        int vlength = String.valueOf(vertexLoaded).length();
        int elength = String.valueOf(edgeLoaded).length();
        System.out.print(vertexLoaded + SLASH + edgeLoaded +
                         backward(vlength + 1 + elength));
    }

    private static void log(String message) {
        LOG.info(message);
    }

    private static void log(String key, long value) {
        LOG.info(String.format("    %-24s: %-20d", key, value));
    }

    private static void log(String key, String value) {
        LOG.info(String.format("    %-24s: %-20s", key, value));
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

    private static String backward(int length) {
        return StringUtils.repeat('\b', length);
    }
}
