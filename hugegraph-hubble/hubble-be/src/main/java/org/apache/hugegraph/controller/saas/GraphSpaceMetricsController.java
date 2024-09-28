/*
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

package org.apache.hugegraph.controller.saas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.client.api.graph.GraphMetricsAPI;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.graphs.GraphsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(Constant.API_VERSION + "/graphspaces/{graphspace}/metrics")
public class GraphSpaceMetricsController extends BaseController {
    @Autowired
    private GraphsService g;

    @GetMapping("element")
    public Object elementMetrics(
            @PathVariable("graphspace") String graphSpace,
            @DateTimeFormat(pattern = "yyyyMMdd") String from,
            @RequestParam(name = "to", required = true)
            @DateTimeFormat(pattern = "yyyyMMdd") String to) {
        HugeClient client = this.authClient(graphSpace, null);
        Set<String> graphs = g.listGraphNames(client, graphSpace, "");

        List<GraphMetricsController.EvCountEntity> result = new ArrayList<>();

        Map<String, GraphMetricsAPI.TypeCount> gsTypeCounts = new HashMap<>();

        for (String graph : graphs) {
            client.assignGraph(graphSpace, graph);
            GraphMetricsAPI.TypeCounts typeCounts =
                    client.graph().getTypeCounts(from, to);
            mergeEvCount(gsTypeCounts, typeCounts);
        }

        List<GraphMetricsController.EvCountEntity> list = new ArrayList<>();
        for (Map.Entry<String, GraphMetricsAPI.TypeCount> entry :
                gsTypeCounts.entrySet()) {
            list.add(new GraphMetricsController.EvCountEntity(entry.getKey(),
                                                              entry.getValue()));
        }
        return list;
    }

    @GetMapping("schema")
    public Object schemaMetrics(
            @PathVariable("graphspace") String graphSpace) {
        HugeClient client = this.authClient(graphSpace, null);
        Set<String> graphs = g.listGraphNames(client, graphSpace, "");

        String today = GraphMetricsController.SAFE_DATE_FORMAT.format(DateUtil.now());

        // 获取当前日期前14天的记录
        List<String> dateRange = GraphMetricsController.getRangeDate(today, -13);

        Map<String, GraphMetricsAPI.TypeCount> gsTypeCounts = new HashMap<>();

        GraphMetricsController.SchemaCount<Integer> schemaCount =
                new GraphMetricsController.SchemaCount<>(0, 0, 0, 0);
        for (String graph : graphs) {
            client.assignGraph(graphSpace, graph);
            GraphMetricsAPI.TypeCounts typeCounts =
                    client.graph().getTypeCounts(dateRange.get(0), dateRange.get(13));
            mergeEvCount(gsTypeCounts, typeCounts);
            schemaCount.pk += client.schema().getPropertyKeys().size();
            schemaCount.vl += client.schema().getVertexLabels().size();
            schemaCount.el += client.schema().getEdgeLabels().size();
            schemaCount.il += client.schema().getIndexLabels().size();
        }

        GraphMetricsController.SchemaCount<Double> schemaWeekRate =
                GraphMetricsController.weeklyGrowthRate(gsTypeCounts, dateRange, schemaCount);

        return new GraphMetricsController.SchemaMetrics(schemaWeekRate,
                                                        schemaCount);
    }


    private void mergeEvCount(
            Map<String, GraphMetricsAPI.TypeCount> merged,
            GraphMetricsAPI.TypeCounts evCount) {
        for (Map.Entry<String, GraphMetricsAPI.TypeCount> entry :
                evCount.getTypeCounts().entrySet()) {
            String k = entry.getKey();
            GraphMetricsAPI.TypeCount v = entry.getValue();
            GraphMetricsAPI.TypeCount gsTypeCount = merged.get(k);
            if (gsTypeCount == null) {
                merged.put(k, v);
            }else {
                // 部分图没有数据,continue
                if (v == null) {
                    continue;
                }

                String updateTime =
                        gsTypeCount.getDatetime().compareTo(v.getDatetime()) >
                        0 ? gsTypeCount.getDatetime() : v.getDatetime();
                gsTypeCount.setDatetime(updateTime);
                mergeEvCount(gsTypeCount.getVertices(), v.getVertices());
                mergeEvCount(gsTypeCount.getEdges(), v.getEdges());
            }
        }
    }

    private void mergeEvCount(Map<String, Long> merged, Map<String, Long> map) {
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }

}
