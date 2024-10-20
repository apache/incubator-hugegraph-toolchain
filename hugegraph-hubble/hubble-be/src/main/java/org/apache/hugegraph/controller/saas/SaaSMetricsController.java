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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.common.AppType;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.ApplicationInfo;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.query.ApplicationInfoService;
import org.apache.hugegraph.service.saas.PrometheusService;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.apache.hugegraph.util.HubbleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(Constant.API_VERSION + "saas/")
public class SaaSMetricsController extends BaseController {

    Logger logger = LoggerFactory.getLogger(SaaSMetricsController.class);
    @Autowired
    private GraphSpaceService graphSpaceService;
    @Autowired
    private PrometheusService prometheusService;
    @Autowired
    private HugeConfig config;
    @Autowired
    private ApplicationInfoService appInfoService;

    private Cache<String, SaaSMetrics> saasMetricsCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(4 * 3600, TimeUnit.SECONDS)
                        .build();

    private static final String BASE_COUNT_QUERY = "sum(nginx_vts_filter_requests_total{__CONDITIONS__})";
    private static final String BASE_DIS_QUERY = "sum(rate(nginx_vts_filter_requests_total{__CONDITIONS__}[2m]))";

    @GetMapping("graphspaces/{graphspace}/graphs/{graph}/request-details")
    public Object appsRequestDetails(@PathVariable String graphspace,
                               @PathVariable String graph) {
        String idc = config.get(HubbleOptions.IDC);
        String graphName = join(idc, graphspace, graph);
        List<ApplicationInfo> appInfos = appInfoService.query(graphName);
        // 添加3个以下默认应用 GREMLIN, VERTEX_UPDATE, EDGE_UPDATE
        appInfos.add(defaultGremlinApp(idc, graphspace, graph));
        appInfos.add(defaultVertexUpdateApp(idc, graphspace, graph));
        appInfos.add(defaultEdgeUpdateApp(idc, graphspace, graph));

        long[] timestamps = HubbleUtil.getTimestampsBefore24Hours();
        long from = timestamps[0];
        long to = timestamps[1];
        List<AppRequestDetails> appsRequestDetails = new ArrayList<>();
        for (ApplicationInfo appInfo : appInfos) {
            appsRequestDetails.add(getAppRequestDetails(appInfo, from, to));
        }

        ApplicationInfo graphAppInfo = defaultGraphApp(idc, graphspace);
        AppRequestDetails totalGraph =
                getAppRequestDetails(graphAppInfo, from, to);

        return RequestDetails.builder()
                           .appNum(appInfos.size())
                           .appsRequestDetails(appsRequestDetails)
                           .countDuring24h(totalGraph.getCountDuring24h())
                           .distributionDuring24h(
                                   totalGraph.getDistributionDuring24h())
                           .distributionQuery(
                                   graphAppInfo.getDistributionQuery())
                           .countQuery(graphAppInfo.getCountQuery())
                           .build();
    }

    @GetMapping("graphspaces/{graphspace}/graphs/{graph}/request-distribution")
    public Object appRequestDetails(@PathVariable String graphspace,
                                  @PathVariable String graph,
                                  @RequestParam(name = "start", required = true)
                                  long start,
                                  @RequestParam(name = "end", required = true)
                                  long end,
                                  @RequestParam(name = "query", required = true)
                                  String query,
                                  @RequestParam(name = "step", required = true)
                                  long step) {
        return prometheusService.queryByRange(query, start, end, step);
    }


    private AppRequestDetails getAppRequestDetails(ApplicationInfo appInfo, long from, long to) {
        long countDuring24h =
                prometheusService.queryByDelta(appInfo.getCountQuery(), from,
                                                 to);
        List<List<Object>> distributionDuring24h =
                prometheusService.queryByRange(appInfo.getDistributionQuery(),
                                                 from, to, 900);
        return AppRequestDetails.builder()
                              .appInfo(appInfo)
                              .countDuring24h(countDuring24h)
                              .distributionDuring24h(distributionDuring24h)
                              .build();
    }

    @GetMapping("metrics")
    public Object metrics() {
        HugeClient client = this.authClient(null, null);
        try {
            return this.saasMetricsCache.get("saas-metrics", () -> {
                Map<String, Long> metrics =
                        this.graphSpaceService.metrics(client);
                String lastDay = HubbleUtil.dateFormatLastDay();
                Long preDayCountTotal =
                        prometheusService.queryCountOffSet1Day(lastDay + " 23:59:59");
                SaaSMetrics saasMetrics =
                        SaaSMetrics.builder()
                                   .edgeCount(metrics.get("eCount"))
                                   .edgeLabelCount(metrics.get("elCount"))
                                   .graphCount(metrics.get("gCount"))
                                   .graphSpaceCount(metrics.get("gsCount"))
                                   .vertexCount(metrics.get("vCount"))
                                   .vertexLabelCount(metrics.get("vlCount"))
                                   .preDayTaskCount(metrics.get("preDayTaskCount"))
                                   .preDayQueryCount(preDayCountTotal)
                                   .build();
                logger.info("saas metrics in [{}]: {}", HubbleUtil.dateFormatLastDay(), saasMetrics);
                this.saasMetricsCache.put("saas-metrics", saasMetrics);
                return saasMetrics;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static ApplicationInfo defaultGremlinApp(String idc,
                                                     String graphSpace,
                                                     String graph) {
        // 默认的Gremlin应用
        String department = departmentName(graphSpace);
        String baseCondition = "idc=~\"__IDC__\", filter=~\".*__DEPARTMENT__.*\", filter_name=\"POST::/gremlin\"";

        String condition = baseCondition.replace("__DEPARTMENT__", department)
                                        .replace("__IDC__", idc);

        ApplicationInfo.ApplicationInfoBuilder app =
                ApplicationInfo.builder();
        app.appName("GREMLIN")
           .appType(AppType.GENERAL)
           .graphName(join(idc, graphSpace, graph))
           .countQuery(BASE_COUNT_QUERY.replace("__CONDITIONS__", condition))
           .distributionQuery(
                   BASE_DIS_QUERY.replace("__CONDITIONS__", condition))
           .description("Gremlin查询应用(通用应用)");
        return app.build();
    }


    public static ApplicationInfo defaultVertexUpdateApp(String idc,
                                                          String graphSpace,
                                                          String graph) {
        // 默认的VertexUpdate应用
        String department = departmentName(graphSpace);
        String baseCondition =
                "idc=~\"__IDC__\", filter=~\".*__DEPARTMENT__.*\", " +
                "filter_name=~\".*/graphspaces/__GRAPH_SPACE__/graphs/__GRAPH__/graph/vertices/batch\"";

        String condition = baseCondition.replace("__DEPARTMENT__", department)
                                        .replace("__IDC__", idc)
                                        .replace("__GRAPH_SPACE__", graphSpace)
                                        .replace("__GRAPH__", graph);

        ApplicationInfo.ApplicationInfoBuilder app =
                ApplicationInfo.builder();
        app.appName("VERTEX-UPDATE")
           .graphName(join(idc, graphSpace, graph))
           .appType(AppType.GENERAL)
           .countQuery(BASE_COUNT_QUERY.replace("__CONDITIONS__", condition))
           .distributionQuery(BASE_DIS_QUERY.replace("__CONDITIONS__", condition))
           .description("批量顶点更新应用(通用应用)");
        return app.build();
    }

    public static ApplicationInfo defaultEdgeUpdateApp(String idc,
                                                        String graphSpace,
                                                        String graph) {
        // 默认的EdgeUpdate应用
        String department = departmentName(graphSpace);
        String baseCondition =
                "idc=~\"__IDC__\", filter=~\".*__DEPARTMENT__.*\", " +
                "filter_name=~\".*/graphspaces/__GRAPH_SPACE__/graphs" +
                "/__GRAPH__/graph/edges/batch\"";

        String condition = baseCondition.replace("__DEPARTMENT__", department)
                                        .replace("__IDC__", idc)
                                        .replace("__GRAPH_SPACE__", graphSpace)
                                        .replace("__GRAPH__", graph);

        ApplicationInfo.ApplicationInfoBuilder app =
                ApplicationInfo.builder();
        app.appName("EDGE-UPDATE")
           .graphName(join(idc, graphSpace, graph))
           .appType(AppType.GENERAL)
           .countQuery(BASE_COUNT_QUERY.replace("__CONDITIONS__", condition))
           .distributionQuery(BASE_DIS_QUERY.replace("__CONDITIONS__", condition))
           .description("批量边更新应用(通用应用)");
        return app.build();
    }

    private static ApplicationInfo defaultGraphApp(String idc, String graphSpace) {
        // 默认的graph应用
        String department = departmentName(graphSpace);
        String baseCondition =
                "idc=~\"__IDC__\", filter=~\".*__DEPARTMENT__.*\"";

        String condition = baseCondition.replace("__DEPARTMENT__", department)
                                        .replace("__IDC__", idc);

        ApplicationInfo.ApplicationInfoBuilder app =
                ApplicationInfo.builder();
        app.appName("TOTAL-GRAPH")
           .appType(AppType.GENERAL)
           .countQuery(BASE_COUNT_QUERY.replace("__CONDITIONS__", condition))
           .distributionQuery(BASE_DIS_QUERY.replace("__CONDITIONS__", condition));
        return app.build();
    }


    private static String departmentName(String graphSpace) {
        if (graphSpace.endsWith("gs")) {
            return graphSpace.substring(0, graphSpace.length() - 2);
        } else {
            return graphSpace;
        }
    }


    private static String join(String idc, String graphSpace, String graph) {
        return String.join("-", idc, graphSpace, graph);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class SaaSMetrics {
        @JsonProperty("vertex-label-count")
        private long vertexLabelCount;

        @JsonProperty("vertex-count")
        private long vertexCount;

        @JsonProperty("edge-label-count")
        private long edgeLabelCount;

        @JsonProperty("edge-count")
        private long edgeCount;

        @JsonProperty("graph-space-count")
        private long graphSpaceCount;

        @JsonProperty("graph-count")
        private long graphCount;

        @JsonProperty("pre-day-task-count")
        private long preDayTaskCount;

        @JsonProperty("pre-day-query-count")
        private long preDayQueryCount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class RequestDetails {
        @JsonProperty("app_num")
        private int appNum;

        @JsonProperty("count_during24h")
        private long countDuring24h;

        @JsonProperty("distribution_during24h")
        private List<List<Object>> distributionDuring24h;

        @JsonProperty("count_query")
        private String countQuery;

        @JsonProperty("distribution_query")
        private String distributionQuery;

        @JsonProperty("apps_request_details")
        private List<AppRequestDetails> appsRequestDetails;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class AppRequestDetails {
        @JsonProperty("app_info")
        private ApplicationInfo appInfo;

        @JsonProperty("count_during24h")
        private long countDuring24h;

        @JsonProperty("distribution_during24h")
        private List<List<Object>> distributionDuring24h;
    }

    public static void main(String[] args) {
        System.out.println("gremlin应用" + defaultGremlinApp("bddwd", "astrolabegs", "indexpro_online"));
        System.out.println("边更新应用" + defaultEdgeUpdateApp("bddwd", "astrolabegs", "indexpro_online"));
        System.out.println("顶点更新应用" + defaultVertexUpdateApp("bddwd", "astrolabegs", "indexpro_online"));
    }
}
