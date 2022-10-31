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

package com.baidu.hugegraph.loader.metrics;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import org.apache.spark.SparkContext;
import org.apache.spark.util.LongAccumulator;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class LoadDistributeMetrics implements Serializable {

    private final InputStruct struct;
    // 分布式写入点边成功导入数量统计

    private   Map<String, Metrics> vertexDisMetrics;
    private   Map<String, Metrics> edgeDisMetrics;

//    private   Map<String, LongAccumulator> vertexParseDisMetrics;
//    private   Map<String, LongAccumulator> edgeParseDisMetrics;
//    private   Map<String, LongAccumulator> vertexSerDisMetrics;
//    private   Map<String, LongAccumulator> edgeSerDisMetrics;
//    private   Map<String, LongAccumulator> vertexInsertDisMetrics;
//    private   Map<String, LongAccumulator> edgeInsertDisMetrics;




    public LoadDistributeMetrics(InputStruct struct) {
        this.struct = struct;
        this.vertexDisMetrics = new HashMap<>();
        this.edgeDisMetrics = new HashMap<>();
        for (VertexMapping mapping : struct.vertices()) {
            this.vertexDisMetrics.put(mapping.label(), new Metrics());
        }
        for (EdgeMapping mapping : struct.edges()) {
            this.edgeDisMetrics.put(mapping.label(), new Metrics());
        }
    }





    //初始化分布式metric
    public void init(SparkContext sc) {
        for (VertexMapping mapping : this.struct.vertices()) {
            Metrics metrics = this.vertexDisMetrics.get(mapping.label());
            metrics.insertSuccess=sc.longAccumulator(mapping.label()+ Constants.UNDERLINE_STR+ Constants.LOAD_DATA_INSERT_SUFFIX);
            metrics.parseSuccess=sc.longAccumulator(mapping.label()+ Constants.UNDERLINE_STR+ Constants.LOAD_DATA_PARSE_SUFFIX);

        }
        for (EdgeMapping mapping : this.struct.edges()) {
            Metrics metrics = this.edgeDisMetrics.get(mapping.label());
            metrics.insertSuccess=sc.longAccumulator(mapping.label()+ Constants.UNDERLINE_STR+ Constants.LOAD_DATA_INSERT_SUFFIX);
            metrics.parseSuccess=sc.longAccumulator(mapping.label()+ Constants.UNDERLINE_STR+ Constants.LOAD_DATA_PARSE_SUFFIX);

        }
    }








    public void increaseDisVertexParseSuccess(ElementMapping mapping) {
        this.disMetrics(mapping).parseSuccess.add(1);
    }
    public void pluseDisVertexParseSuccess(ElementMapping mapping, Long count) {
        this.disMetrics(mapping).parseSuccess.add(count);
    }
    public void increaseDisVertexInsertSuccess(ElementMapping mapping) {
        this.disMetrics(mapping).insertSuccess.add(1);
    }

    public void plusDisVertexInsertSuccess(ElementMapping mapping, Long count) {
        this.disMetrics(mapping).insertSuccess.add(count);
    }



    public void increaseDisEdgeParseSuccess(ElementMapping mapping) {
        this.disMetrics(mapping).parseSuccess.add(1);
    }
    public void pluseDisEdgeParseSuccess(ElementMapping mapping, Long count) {
        this.disMetrics(mapping).parseSuccess.add(count);
    }
    public void increaseDisEdgeInsertSuccess(ElementMapping mapping) {
        this.disMetrics(mapping).insertSuccess.add(1);
    }

    public void plusDisEdgeInsertSuccess(ElementMapping mapping, Long count) {
        this.disMetrics(mapping).insertSuccess.add(count);
    }

    public Long readVertexInsertSuccess(){
        Long totalCnt=0L;
        Collection<Metrics> values = vertexDisMetrics.values();
        for (Metrics metrics : values) {
            totalCnt += metrics.insertSuccess();
        }
        return totalCnt;
    }



    public Long readEdgeInsertSuccess(){
        Long totalCnt=0L;
        Collection<Metrics> values = edgeDisMetrics.values();
        for (Metrics metrics : values) {
            totalCnt += metrics.insertSuccess();
        }
        return totalCnt;
    }

    private Metrics disMetrics(ElementMapping mapping) {
        if (mapping.type().isVertex()) {
            return this.vertexDisMetrics.get(mapping.label());
        } else {
            return this.edgeDisMetrics.get(mapping.label());
        }
    }
    public static class Metrics implements Serializable {

//        private final LongAccumulator parseSuccess;
//        private final LongAccumulator parseFailure;
//        private final LongAccumulator insertSuccess;
//        private final LongAccumulator insertFailure;

        private LongAccumulator parseSuccess;
        private LongAccumulator parseFailure;
        private LongAccumulator insertSuccess;
        private LongAccumulator insertFailure;

        public Metrics() {
//            this.parseSuccess = new LongAdder();
//            this.parseFailure = new LongAdder();
//            this.insertSuccess = new LongAdder();
//            this.insertFailure = new LongAdder();
        }

        public long parseSuccess() {
            return this.parseSuccess.value();
        }

        public long parseFailure() {
            return this.parseFailure.value();
        }

        public long insertSuccess() {
            return this.insertSuccess.value();
        }

        public long insertFailure() {
            return this.insertFailure.value();
        }
    }
}
