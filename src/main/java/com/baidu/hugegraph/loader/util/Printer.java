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
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.TimeUtil;

public final class Printer {

    private static final Logger LOG = Log.logger(Printer.class);

    private static final String EMPTY_LINE = "";
    private static final String SLASH = "/";
    private static final String DIVIDE_LINE = StringUtils.repeat('-', 50);

    public static void printRealtimeProgress(LoadContext context) {
        LoadOptions options = context.options();
        if (!options.printProgress) {
            return;
        }
        System.out.printf(">> HugeGraphLoader worked in %s%n",
                options.workModeString());
        if (options.incrementalMode) {
            LoadProgress progress = context.oldProgress();
            System.out.printf("vertices/edges loaded last time: %s/%s%n",
                    progress.vertexLoaded(), progress.edgeLoaded());
        }
        System.out.print("vertices/edges loaded this time : ");
    }

    public static void printFinalProgress(LoadContext context) {
        LoadOptions options = context.options();
        if (!options.printProgress) {
            return;
        }
        LoadSummary summary = context.summary();
        long vertexLoaded = summary.vertex().getCount();
        long edgeLoaded = summary.edge().getCount();
        System.out.println(vertexLoaded + SLASH + edgeLoaded);
    }

    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        // Just log vertices/edges metrics
        log(DIVIDE_LINE + "\ndetail metrics");
        summary.inputMetricsMap().forEach((id, metrics) -> {
            log(EMPTY_LINE + format("\ninput-struct '%s'", id) +
                format("\nread success", metrics.readSuccess()) +
                format("\nread failure", metrics.readFailure()));
            metrics.vertexMetrics().forEach((label, labelMetrics) -> {
                log(String.format("vertex '%s'", label) +
                    format("\nparse success", labelMetrics.parseSuccess()) +
                    format("\nparse failure", labelMetrics.parseFailure()) +
                    format("\ninsert success", labelMetrics.insertSuccess()) +
                    format("\ninsert failure", labelMetrics.insertFailure()));
            });
            metrics.edgeMetrics().forEach((label, labelMetrics) -> {
                log(String.format("edge '%s'", label) +
                    format("\nparse success", labelMetrics.parseSuccess()) +
                    format("\nparse failure", labelMetrics.parseFailure()) +
                    format("\ninsert success", labelMetrics.insertSuccess()) +
                    format("\ninsert failure", labelMetrics.insertFailure()));
            });
        });

        // Print and log total vertices/edges metrics
        printAndLog(DIVIDE_LINE + "\n" +
                 getCountReport(LoadReport.collect(summary)) +
                "\n" + DIVIDE_LINE);
        printMeterReport(summary);
    }
    private static String getCountReport(LoadReport r) {
        // for multi-thread output
        String sb = "count metrics" +
                format("\ninput read success", r.readSuccess()) +
                format("\ninput read failure", r.readFailure()) +
                format("\nvertex parse success", r.vertexParseSuccess()) +
                format("\nvertex parse failure", r.vertexParseFailure()) +
                format("\nvertex insert success", r.vertexInsertSuccess()) +
                format("\nvertex insert failure", r.vertexInsertFailure()) +
                format("\nedge parse success", r.edgeParseSuccess()) +
                format("\nedge parse failure", r.edgeParseFailure()) +
                format("\nedge insert success", r.edgeInsertSuccess()) +
                format("\nedge insert failure", r.edgeInsertFailure());
        return sb;
    }
    private static void printCountReport(LoadReport r) {
        printAndLog(getCountReport(r));
    }

    private static void printMeterReport(LoadSummary summary) {
        long totalTime = summary.totalTime();
        long loadTime = summary.loadTime();
        long readTime = totalTime - loadTime;

        // for multi-thread output
        String sb = "meter metrics" +
                format("\ntotal time", TimeUtil.readableTime(totalTime)) +
                format("\nread time", TimeUtil.readableTime(readTime)) +
                format("\nload time", TimeUtil.readableTime(loadTime)) +
                format("\nvertex load time", TimeUtil.readableTime(
                        summary.vertex().getTime())) +
                format("\nvertex load rate(vertices/s)",
                        summary.vertex().avgRate()) +
                format("\nedge load time",
                        TimeUtil.readableTime(summary.edge().getTime())) +
                format("\nedge load rate(edges/s)", summary.edge().avgRate());

        printAndLog(sb);
    }

    public static void printError(String message, Object... args) {
        String formatMsg = String.format(message, args);
        LOG.error(formatMsg);
        // Print an empty line
        System.err.println();
        System.err.println(formatMsg);
    }

    public static void printError(String message, Throwable e) {
        String formatMsg;
        if (!StringUtils.isEmpty(e.getMessage())) {
            formatMsg = String.format("%s, cause: %s", message, e.getMessage());
        } else {
            formatMsg = String.format("%s, please see log for detail", message);
        }
        LOG.error(formatMsg, e);
        // Print an empty line
        System.err.println();
        System.err.println(formatMsg);
    }

    public static void printProgress(LoadContext context, ElemType type,
                                     long frequency, int batchSize) {
        LoadSummary summary = context.summary();
        if (context.options().printProgress) {
            Printer.printInBackward(summary.vertex().getCount(),
                    summary.edge().getCount());
        }

        LoadSummary.LoadRater rater =
                type.isVertex() ? summary.vertex() : summary.edge();
        if (rater.getCount() % frequency < batchSize &&
                (rater.curRate() != 0 || rater.getCount() == 0)) {
            LOG.info("{} loaded: {}, " +
                            "average rate: {}/s, cur rate: {}/s, " +
                            "average queue: {}, cur queue: {}",
                    type.string(), rater.getCount(),
                    rater.avgRate(), rater.curRate(),
                    summary.getAvgTaskQueueLen(), summary.getTaskQueueLen());
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
        LOG.info(format(key, value));
    }

    private static void log(String key, String value) {
        LOG.info(format(key, value));
    }

    private static void printAndLog(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private static String format(String key, long value) {
        return String.format("    %-30s: %-20d", key, value);
    }

    private static String format(String key, String value) {
        return String.format("    %-30s: %-20s", key, value);
    }

    private static String backward(int length) {
        return StringUtils.repeat('\b', length);
    }
}
