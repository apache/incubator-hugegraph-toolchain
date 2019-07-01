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

package com.baidu.hugegraph.loader.summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class LoadSummary {

    private final Map<String, LoadMetrics> vertexMetricsMap;
    private final Map<String, LoadMetrics> edgeMetricsMap;

    public LoadSummary() {
        this.vertexMetricsMap = InsertionOrderUtil.newMap();
        this.edgeMetricsMap = InsertionOrderUtil.newMap();
    }

    public LoadMetrics metrics(ElementStruct struct) {
        ElemType type = struct.type();
        Map<String, LoadMetrics> metricsMap;
        if (type.isVertex()) {
            metricsMap = this.vertexMetricsMap;
        } else {
            assert type.isEdge();
            metricsMap = this.edgeMetricsMap;
        }
        return metricsMap.computeIfAbsent(struct.uniqueKey(),
                                          k -> new LoadMetrics());
    }

    public Map<String, LoadMetrics> vertexMetrics() {
        return this.vertexMetricsMap;
    }

    public Map<String, LoadMetrics> edgeMetrics() {
        return this.edgeMetricsMap;
    }

    public LoadMetrics accumulateMetrics(ElemType type) {
        if (type.isVertex()) {
            return this.accumulateMetrics(this.vertexMetricsMap.values());
        } else {
            assert type.isEdge();
            return this.accumulateMetrics(this.edgeMetricsMap.values());
        }
    }

    public LoadMetrics accumulateAllMetrics() {
        List<LoadMetrics> metricsList = new ArrayList<>();
        metricsList.addAll(this.vertexMetricsMap.values());
        metricsList.addAll(this.edgeMetricsMap.values());
        return this.accumulateMetrics(metricsList);
    }

    private LoadMetrics accumulateMetrics(Collection<LoadMetrics> metricsList) {
        LoadMetrics totalMetrics = new LoadMetrics();
        for (LoadMetrics metrics : metricsList) {
            totalMetrics.plusParseSuccess(metrics.parseSuccess());
            totalMetrics.plusParseFailure(metrics.parseFailure());
            totalMetrics.plusParseTime(metrics.parseTime());

            totalMetrics.plusLoadSuccess(metrics.loadSuccess());
            totalMetrics.plusLoadFailure(metrics.loadFailure());
            totalMetrics.plusLoadTime(metrics.loadTime());
        }
        return totalMetrics;
    }
}
