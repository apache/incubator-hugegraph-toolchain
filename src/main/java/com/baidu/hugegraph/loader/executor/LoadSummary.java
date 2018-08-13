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

package com.baidu.hugegraph.loader.executor;

import java.time.Duration;
import java.util.Formatter;

public class LoadSummary {

    private static Formatter formatter = new Formatter(System.out);

    private long parseFailureVertices;
    private long insertFailureVertices;
    private long insertSuccessVertices;

    private long parseFailureEdges;
    private long insertFailureEdges;
    private long insertSuccessEdges;

    private Duration vertexLoadTime;
    private Duration edgeLoadTime;

    public LoadSummary() {
        this.vertexLoadTime = Duration.ZERO;
        this.edgeLoadTime = Duration.ZERO;
    }

    public void print() {
        System.out.println("-------------------------------------------------");
        System.out.println("Vertex Results:");
        printInFormat("Parse failure vertices", this.parseFailureVertices());
        printInFormat("Insert failure vertices", this.insertFailureVertices());
        printInFormat("Insert success vertices", this.insertSuccessVertices());

        System.out.println("-------------------------------------------------");
        System.out.println("Edge Results:");
        printInFormat("Parse failure edges", this.parseFailureEdges());
        printInFormat("Insert failure edges", this.insertFailureEdges());
        printInFormat("Insert success edges", this.insertSuccessEdges());

        System.out.println("-------------------------------------------------");
        System.out.println("Time Results:");
        printInFormat("Vertex loading time", this.vertexLoadTime().getSeconds());
        printInFormat("Edge loading time", this.edgeLoadTime().getSeconds());
        printInFormat("Total loading time", this.totalTime().getSeconds());
    }

    private static void printInFormat(String desc, long value) {
        formatter.format("\t%-25s:\t%-20d%n", desc, value);
    }

    private Duration totalTime() {
        return edgeLoadTime().plus(vertexLoadTime());
    }

    public Duration vertexLoadTime() {
        return this.vertexLoadTime;
    }

    public void vertexLoadTime(Duration duration) {
        this.vertexLoadTime = duration;
    }

    public Duration edgeLoadTime() {
        return this.edgeLoadTime;
    }

    public void edgeLoadTime(Duration duration) {
        this.edgeLoadTime = duration;
    }

    public long parseFailureVertices() {
        return this.parseFailureVertices;
    }

    public void parseFailureVertices(long count) {
        this.parseFailureVertices = count;
    }

    public long insertFailureVertices() {
        return this.insertFailureVertices;
    }

    public void insertFailureVertices(long count) {
        this.insertFailureVertices = count;
    }

    public long insertSuccessVertices() {
        return this.insertSuccessVertices;
    }

    public void insertSuccessVertices(long count) {
        this.insertSuccessVertices = count;
    }

    public long parseFailureEdges() {
        return this.parseFailureEdges;
    }

    public void parseFailureEdges(long count) {
        this.parseFailureEdges = count;
    }

    public long insertFailureEdges() {
        return this.insertFailureEdges;
    }

    public void insertFailureEdges(long count) {
        this.insertFailureEdges = count;
    }

    public long insertSuccessEdges() {
        return this.insertSuccessEdges;
    }

    public void insertSuccessEdges(long count) {
        this.insertSuccessEdges = count;
    }
}
