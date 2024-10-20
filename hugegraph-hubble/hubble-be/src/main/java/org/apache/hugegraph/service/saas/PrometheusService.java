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

package org.apache.hugegraph.service.saas;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.rest.AbstractRestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.util.HubbleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chengxin05
 * @date 2024/3/20
 * @desc PrometheusService.class 作用为请求prometheus上存储的指标数据
 */

@Slf4j
@Service
public class PrometheusService {

    private static final String Q_PATH = "/api/v1/query";
    private static final String Q_RANGE_PATH = "api/v1/query_range";

    @Autowired
    private HugeConfig config;

    private MetricsClient client;

    public PrometheusService() {
    }

    public PrometheusService(String prometheusUrl) {
        client = new MetricsClient(prometheusUrl, 30000);
    }

    private MetricsClient getClient() {
        if (client == null) {
            client = new MetricsClient(config.get(HubbleOptions.PROMETHEUS_URL),
                                       30000);
        }
        return client;
    }


    public long queryByDelta(String query, long start, long end) {
        log.info("queryByDelta: query=[{}] start=[{}], end=[{}]", query, start, end);
        return queryByTimestamp(query, end) - queryByTimestamp(query, start);
    }


    /**
     * 根据时间戳查询当前的统计数据
     * @param query     promSQL查询语句
     * @param timestamp 时间戳
     * @return 返回查询结果的Map对象
     */
    private long queryByTimestamp(String query, long timestamp) {
        Map<String, Object> map = new HashMap<>();
        map.put("query", encodeUrl(query));
        map.put("time", timestamp);
        RestResult result = getClient().get(Q_PATH, map);
        return extractCount(result.readObject(Map.class));
    }

    /**
     * 根据指定范围查询数据
     * @param query 查询条件
     * @param from  起始时间戳
     * @param to    结束时间戳
     * @param step  时间步长/s (比如(to-from)=10s, step=2s, 则返回5组数据)
     * @return 返回查询结果
     */
    public List<List<Object>> queryByRange(String query, long from, long to, long step) {
        log.info("queryByDelta: query=[{}] start=[{}], end=[{}], step=[{}]", query, from, to, step);
        Map<String, Object> map = new HashMap<>();
        map.put("query", encodeUrl(query));
        map.put("start", from);
        map.put("end", to);
        map.put("step", step);
        RestResult result = getClient().get(Q_RANGE_PATH, map);
        return extractDistribution(result.readObject(Map.class));

    }

    private List<List<Object>> extractDistribution(Map map) {
        List<List<Object>> values = null;
        try {
            Map<String, Object> map1 = (Map) map.get("data");
            List list = (List) map1.get("result");
            Map<String, Object> map2 = (Map) list.get(0);
            values = (List<List<Object>>) map2.get("values");
        } catch (Exception e) {
            log.error("extract distribution from prometheus response error");
        }
        return values;
    }

    /**
     * 统计当前日期往前一天的请求量
     * @param lastDay eg: 2023-12-06 23:59:59
     * @return
     */
    public Long queryCountOffSet1Day(String lastDay) {
        long[] timestamps = HubbleUtil.getTimestampsBefore24Hours(lastDay);
        String query = "sum(nginx_vts_filter_requests_total{})";
        return queryByDelta(query, timestamps[0], timestamps[1]);
    }

    private long extractCount(Map dataMap) {
        long count = 0L;
        try {
            Map map1 = (Map) dataMap.get("data");
            Map map2 = (Map) ((List) map1.get("result")).get(0);
            List list = (List) map2.get("value");

            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                if (obj instanceof String) {
                    String countS = (String) obj;
                    count = Long.valueOf(countS.split("\\.")[0]);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("extract count from prometheus response error");
        }

        return count;
    }

    private static String encodeUrl(String url) {
        // promQL 语法中括号不需要转义
        String encodeUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
        return encodeUrl.replaceAll("%28", "(")
                        .replaceAll("%29", ")")
                        .replaceAll("\\+", "%20");
    }
// TODO re-evaluate this change
//    private static class MetricsClient extends AbstractRestClient {
//        public MetricsClient(String url, int timeout) {
//            super(url, timeout);
//        }
//
//        @Override
//        protected void checkStatus(Response response, Response.Status... statuses) {
//
//        }
//
//    }

    private static class MetricsClient extends AbstractRestClient {
        public MetricsClient(String url, int timeout) {
            super(url, timeout);
        }
        @Override
        protected void checkStatus(Response response, int... ints) {

        }
    }
}
