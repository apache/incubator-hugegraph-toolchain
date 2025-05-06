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

package org.apache.hugegraph.loader.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.constant.ElemType;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.metrics.LoadReport;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.loader.progress.LoadProgress;
import org.apache.hugegraph.util.Log;
import org.apache.hugegraph.util.TimeUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
        long vertexLoaded = summary.vertexLoaded();
        long edgeLoaded = summary.edgeLoaded();
        System.out.println(vertexLoaded + SLASH + edgeLoaded);
    }

    public static void printSummary(LoadContext context) {
        LoadSummary summary = context.summary();
        // Create a JSON structure to hold our metadata
        JsonObjectBuilder metadataJson = Json.createObjectBuilder();
        JsonObjectBuilder vertexCountsByLabel = Json.createObjectBuilder();
        JsonObjectBuilder edgeCountsByLabel = Json.createObjectBuilder();
        
        // Just log vertices/edges metrics
        log(DIVIDE_LINE);
        log("detail metrics");
        summary.inputMetricsMap().forEach((id, metrics) -> {
            log(EMPTY_LINE);
            log(String.format("input-struct '%s'", id));
            log("read success", metrics.readSuccess());
            log("read failure", metrics.readFailure());
            metrics.vertexMetrics().forEach((label, labelMetrics) -> {
                log(String.format("vertex '%s'", label));
                log("parse success", labelMetrics.parseSuccess());
                log("parse failure", labelMetrics.parseFailure());
                log("insert success", labelMetrics.insertSuccess());
                log("insert failure", labelMetrics.insertFailure());
                
                // Add vertex counts by label to our JSON
                vertexCountsByLabel.add(label, labelMetrics.insertSuccess());
            });
            metrics.edgeMetrics().forEach((label, labelMetrics) -> {
                log(String.format("edge '%s'", label));
                log("parse success", labelMetrics.parseSuccess());
                log("parse failure", labelMetrics.parseFailure());
                log("insert success", labelMetrics.insertSuccess());
                log("insert failure", labelMetrics.insertFailure());
                
                // Add edge counts by label to our JSON
                edgeCountsByLabel.add(label, labelMetrics.insertSuccess());
            });
        });

        // Get total counts
        LoadReport report = LoadReport.collect(summary);
        long totalVertices = report.vertexInsertSuccess();
        long totalEdges = report.edgeInsertSuccess();
        
        // Add totals to our metadata JSON
        metadataJson.add("totalVertices", totalVertices);
        metadataJson.add("totalEdges", totalEdges);
        metadataJson.add("verticesByLabel", vertexCountsByLabel);
        metadataJson.add("edgesByLabel", edgeCountsByLabel);
        
        // Print and log total vertices/edges metrics
        printAndLog(DIVIDE_LINE);
        printCountReport(report);
        printAndLog(DIVIDE_LINE);
        printMeterReport(summary);
        
        // Save metadata to output file
        saveMetadataToFile(metadataJson, context);
    }

    private static void saveMetadataToFile(JsonObjectBuilder metadata, LoadContext context) {
        // Determine output directory - use the option if available or default to current directory
        String outputDir = context.options().output;
        
        // Create the output directory if it doesn't exist
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // Create filename
        String filename = "graph_metadata.json";
        File outputFile = new File(outputDir, filename);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Build the complete JSON object
            JsonObject metadataJson = metadata.build();
            
            // Extract just the valueMap if it exists
            JsonObject valueMap = metadataJson.getJsonObject("valueMap");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            if (valueMap != null) {
                // Write only the valueMap content
                gson.toJson(valueMap, writer);
            } else {
                // Fallback to writing entire metadata if valueMap doesn't exist
                gson.toJson(metadataJson, writer);
            }
            
            log("Metadata saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            log("Failed to save metadata to file: " + e.getMessage());
            e.printStackTrace();
        }
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
        printAndLog("edge insert failure", report.edgeInsertFailure());
    }

    private static void printMeterReport(LoadSummary summary) {
        long totalTime = summary.totalTime();
        long vertexTime = summary.vertexTime();
        long edgeTime = summary.edgeTime();
        long loadTime = summary.loadTime();
        long readTime = totalTime - loadTime;

        printAndLog("meter metrics");
        printAndLog("total time", TimeUtil.readableTime(totalTime));
        printAndLog("read time", TimeUtil.readableTime(readTime));
        printAndLog("load time", TimeUtil.readableTime(loadTime));
        printAndLog("vertex load time", TimeUtil.readableTime(vertexTime));
        printAndLog("vertex load rate(vertices/s)",
                    summary.loadRate(ElemType.VERTEX));
        printAndLog("edge load time", TimeUtil.readableTime(edgeTime));
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
        int vLength = String.valueOf(vertexLoaded).length();
        int eLength = String.valueOf(edgeLoaded).length();
        System.out.print(vertexLoaded + SLASH + edgeLoaded +
                         backward(vLength + 1 + eLength));
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
        printAndLog(String.format("    %-30s: %-20d", key, value));
    }

    private static void printAndLog(String key, String value) {
        printAndLog(String.format("    %-30s: %-20s", key, value));
    }

    private static String backward(int length) {
        return StringUtils.repeat('\b', length);
    }
}
