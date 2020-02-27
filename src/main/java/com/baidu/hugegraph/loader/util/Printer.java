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
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.metrics.LoadReport;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.TimeUtil;

public final class Printer {

    private static final Logger LOG = Log.logger(Printer.class);

    private static final String EMPTY_LINE = "";
    private static final String SLASH = "/";
    private static final String DIVIDE_LINE = StringUtils.repeat('-', 50);

    private static final LoadContext context = LoadContext.get();

    public static void printRealtimeProgress() {
        LoadOptions options = context.options();
        if (!options.printProgress) {
            return;
        }
        System.out.print("vertices/edges has been loaded this time : ");
    }

    public static void printFinalProgress() {
        LoadOptions options = context.options();
        if (!options.printProgress) {
            return;
        }
        LoadSummary summary = context.summary();
        long vertexLoaded = summary.vertexLoaded();
        long edgeLoaded = summary.edgeLoaded();
        System.out.println(vertexLoaded + SLASH + edgeLoaded);
    }

    public static void printSummary() {
        LoadSummary summary = context.summary();
        // Just log vertices/edges metrics
        log(DIVIDE_LINE);
        log("detail metrics");
        summary.inputMetricsMap().forEach((id, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("input-struct '%s'", id));
            log("read success", metrics.readSuccess());
            log("read failure", metrics.readFailure());
            metrics.vertexCounters().forEach((label, counter) -> {
                log(String.format("vertex '%s'", label));
                log("parse success", counter.parseSuccess());
                log("parse failure", counter.parseFailure());
                log("insert success", counter.insertSuccess());
                log("insert failure", counter.insertFailure());
            });
            metrics.edgeCounters().forEach((label, counter) -> {
                log(String.format("edge '%s'", label));
                log("parse success", counter.parseSuccess());
                log("parse failure", counter.parseFailure());
                log("insert success", counter.insertSuccess());
                log("insert failure", counter.insertFailure());
            });
        });

        // Print and log total vertices/edges metrics
        printAndLog(DIVIDE_LINE);
        printCountReport(LoadReport.collect(summary));
        printAndLog(DIVIDE_LINE);
        printMeterReport(summary);
    }

    private static void printCountReport(LoadReport report) {
        printAndLog("count metrics");
        printAndLog("input read success", report.readSuccess());
        printAndLog("input read failure", report.readFailure());
        printAndLog("vertex parse success", report.vertexParseSuccess());
        printAndLog("vertex parse failure", report.vertexParseFailure());
        printAndLog("vertex insert success", report.vertexInsertSuccess());
        printAndLog("vertex insert failure", report.vertexInsertFailure());
        printAndLog("edge parse success", report.edgeParseSuccess());
        printAndLog("edge parse failure", report.edgeParseFailure());
        printAndLog("edge insert success", report.edgeInsertSuccess());
        printAndLog("edge insert failure", report.edgeParseFailure());
    }

    private static void printMeterReport(LoadSummary summary) {
        printAndLog("meter metrics");
        printAndLog("total time", TimeUtil.readableTime(summary.totalTime()));
        printAndLog("vertex load rate(vertices/s)", summary.loadRate(ElemType.VERTEX));
        printAndLog("edge load rate(edges/s)", summary.loadRate(ElemType.EDGE));
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

    public static void printProgress(ElemType type, long frequency,
                                     int batchSize) {
        LoadSummary summary = context.summary();
        long vertexLoaded = summary.vertexLoaded();
        long edgeLoaded = summary.edgeLoaded();
        if (context.options().printProgress) {
            Printer.printInBackward(vertexLoaded, edgeLoaded);
        }

        long loadSuccess = type.isVertex() ? vertexLoaded : edgeLoaded;
        if (loadSuccess % frequency < batchSize) {
            LOG.info("{} has been loaded: {}, average load rate: {}/s",
                     type.string(), loadSuccess, summary.loadRate(type));
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
        LOG.info(String.format("    %-30s: %-20d", key, value));
    }

    private static void log(String key, String value) {
        LOG.info(String.format("    %-30s: %-20s", key, value));
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static void printAndLog(String key, long value) {
        String msg = String.format("    %-30s: %-20d", key, value);
        printAndLog(msg);
    }

    private static void printAndLog(String key, String value) {
        String msg = String.format("    %-30s: %-20s", key, value);
        printAndLog(msg);
    }

    private static String backward(int length) {
        return StringUtils.repeat('\b', length);
    }
}
