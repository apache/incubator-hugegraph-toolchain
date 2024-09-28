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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hugegraph.client.api.graph.GraphMetricsAPI;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.date.SafeDateFormat;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.HugeException;
import org.apache.hugegraph.util.DateUtil;
import org.apache.hugegraph.util.E;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(Constant.API_VERSION + "/graphspaces/{graphspace}/graphs/{graph}/metrics")
public class GraphMetricsController extends BaseController {
    private static final Logger LOG = LoggerFactory.getLogger(
            GraphMetricsController.class);

    public static final SafeDateFormat SAFE_DATE_FORMAT = new SafeDateFormat("yyyyMMdd");


    @GetMapping("schema")
    public Object schemaMetrics(@PathVariable("graphspace") String graphSpace,
                                @PathVariable("graph") String graph) {

        HugeClient client = this.authClient(graphSpace, graph);
        String today = SAFE_DATE_FORMAT.format(DateUtil.now());

        // 获取当前日期前14天的记录
        List<String> dateRange = getRangeDate(today, -13);

        LOG.info("schema metrics info: query type count from = [{}], to = [{}]",
                 dateRange.get(0), dateRange.get(13));

        SchemaCount<Integer> schemaCount =
                new SchemaCount<>(client.schema().getPropertyKeys().size(),
                                client.schema().getVertexLabels().size(),
                                client.schema().getEdgeLabels().size(),
                                client.schema().getIndexLabels().size());

        GraphMetricsAPI.TypeCounts rangeTypeCount =
                client.graph().getTypeCounts(dateRange.get(0), dateRange.get(dateRange.size() - 1));

        SchemaCount<Double> schemaWeekRate =
                weeklyGrowthRate(rangeTypeCount.getTypeCounts(), dateRange, schemaCount);

        return new SchemaMetrics(schemaWeekRate, schemaCount);
    }


    /**
     * @return 返回当前日期前14天的日期列表
     */
    @GetMapping("element")
    public Object elementMetrics(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestParam(name = "from", required = true)
            @DateTimeFormat(pattern = "yyyyMMdd") String from,
            @RequestParam(name = "to", required = true)
            @DateTimeFormat(pattern = "yyyyMMdd") String to) {
        HugeClient client = this.authClient(graphSpace, graph);
        return getEvCount(client, graphSpace, graph, from, to);
    }


    /**
     * 统计单种类型顶点或者边的日增长率及其存量数据增长率
     * 及其他统计信息
     * @param from 起始日期
     * @param to 终止日期
     * @param type 顶点或边
     * @param schema schema名称
     * @return
     */
    @GetMapping("element/schema")
    public Object elementSchemaMetrics(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestParam(name = "from", required = true)
            @DateTimeFormat(pattern = "yyyyMMdd") String from,
            @RequestParam(name = "to", required = true)
            @DateTimeFormat(pattern = "yyyyMMdd") String to,
            @RequestParam(name = "type", required = true) String type,
            @RequestParam(name = "schema", required = true) String schema) {
        // 校验type参数
        E.checkArgument("vertex".equals(type) || "edge".equals(type),
                        String.format("type must in [vertex, edge], but got '%s'", type));

        // 校验schema存不存在
        HugeClient client = this.authClient(graphSpace, graph);
        boolean schemaExisted = false;
        if("vertex".equals(type)) {
            schemaExisted = client.schema().getVertexLabel(schema) != null;
        } else {
            schemaExisted = client.schema().getEdgeLabel(schema) != null;
        }
        E.checkArgument(schemaExisted,
                        String.format("schema '%s' not existed", schema));

        // 计算单种类型顶点或者边的日增长率及其存量数据增长率
        PerEvEntity result = calGrowthRates(client, type, schema);

        // 组织当前schema顶点或者边按日期分布的数据
        ArrayList<ItemCount> itemCounts = new ArrayList<>();
        GraphMetricsAPI.TypeCounts typeCounts =
                client.graph().getTypeCounts(from, to);
        for (Map.Entry<String, GraphMetricsAPI.TypeCount> entry :
                typeCounts.getTypeCounts().entrySet()) {
            String k = entry.getKey();
            GraphMetricsAPI.TypeCount v = entry.getValue();
            if (v == null) {
                itemCounts.add(new ItemCount(k, null, 0L));
            }else {
                itemCounts.add(new ItemCount(k, v.getDatetime(),
                                             getEleCount(type, schema, v)));
            }
        }
        result.setCountBySchema(itemCounts);

        return result;
    }

    private PerEvEntity calGrowthRates(HugeClient client, String type,
                                       String schema) {
        String today = SAFE_DATE_FORMAT.format(DateUtil.now());
        List<String> last4Days = getRangeDate(today, -3);
        GraphMetricsAPI.TypeCounts last3TypeCount =
                client.graph().getTypeCounts(last4Days.get(0), last4Days.get(2));

        Long[] last3Days = extractTotalCountLast3Days(last3TypeCount, type, schema,
                                         last4Days.subList(0, 3));
        Long totalCount1 = last3Days[2]; // 前1天当前schema总数
        Long totalCount2 = last3Days[1]; // 前2天当前schema总数
        Long totalCount3 = last3Days[0]; // 前3天当前schema总数

        // 设置总数值及增长量值
        return PerEvEntity.builder()
                          .addedGrowthRate(growthRate(
                                  totalCount2 - totalCount3,
                                  totalCount1 - totalCount2))
                          .totalGrowthRate(growthRate(totalCount2, totalCount1))
                          .preDayAdded(totalCount1 - totalCount2)
                          .preDayTotal(totalCount1)
                          .type(schema).build();
    }

    public static List<EvCountEntity> getEvCount(HugeClient client, String gs,
                                                 String graph, String from,
                                                 String to) {
        client.assignGraph(gs, graph);
        GraphMetricsAPI.TypeCounts typeCounts =
                client.graph().getTypeCounts(from, to);

        List<EvCountEntity> list = new ArrayList<>();
        for (Map.Entry<String, GraphMetricsAPI.TypeCount> entry :
                typeCounts.getTypeCounts().entrySet()) {
            list.add(new EvCountEntity(entry.getKey(), entry.getValue()));
        }
        return list;

    }

    private Long[] extractTotalCountLast3Days(
            GraphMetricsAPI.TypeCounts typeCounts, String type,
            String schema, List<String> dateRange) {

        if (typeCounts.getTypeCounts() == null) {
            return new Long[]{0L, 0L, 0L};
        }

        ArrayList<Long> list = new ArrayList<>();
        for (int i = 0; i < dateRange.size(); i++) {
            GraphMetricsAPI.TypeCount typeCount =
                    typeCounts.getTypeCounts().get(dateRange.get(i));
            Long totalCount = getEleCount(type, schema, typeCount);
            list.add(totalCount);
        }

        assert list.size() == 3;
        return list.toArray(new Long[3]);
    }

    private Long getEleCount(String type, String schema, GraphMetricsAPI.TypeCount typeCount) {
        if (typeCount == null) {
            return 0L;
        }

        Map<String, Long> countMap;
        if ("vertex".equals(type)) {
            countMap = typeCount.getVertices();
        } else if ("edge".equals(type)) {
            countMap = typeCount.getEdges();
        } else {
            throw new HugeException("undefine type: type must in [vertex, edge]");
        }

        if (countMap == null || countMap.get(schema) == null) {
            return 0L;
        }

        return countMap.get(schema);
    }


    /**
     * 计算周增长率
     *
     * @param map  日期范围下类型统计数据
     * @param dateRange   日期范围
     * @param todaySchema 当前日期下的schema统计
     * @return schema的周增长率
     */
    public static SchemaCount<Double> weeklyGrowthRate(
            Map<String, GraphMetricsAPI.TypeCount> map,
            List<String> dateRange,
            SchemaCount<Integer> todaySchema) {

        assert dateRange.size() == 14 && map.size() == 14;

        SchemaCount<Long> s1 = new SchemaCount<>(0L, 0L, 0L, 0L);
        SchemaCount<Long> s2 = new SchemaCount<>(0L, 0L, 0L, 0L);
        // 统计近一周的前一周
        for (int i = 0; i < 7; i++) {
            GraphMetricsAPI.TypeCount typeCount = map.get(dateRange.get(i));
            if (typeCount == null) {
                continue;
            }
            s1.pk = s1.pk + typeCount.getSchemas().get("pk_count");
            s1.vl = s1.vl + typeCount.getSchemas().get("vl_count");
            s1.el = s1.el + typeCount.getSchemas().get("el_count");
            s1.il = s1.il + typeCount.getSchemas().get("il_count");
        }

        // 近一周：取最近6天的数据 + 当天数据用实时数据
        s2.pk = s2.pk + todaySchema.pk;
        s2.vl = s2.vl + todaySchema.vl;
        s2.el = s2.el + todaySchema.el;
        s2.il = s2.il + todaySchema.il;
        for (int i = 7; i < 13; i++) {
            GraphMetricsAPI.TypeCount typeCount = map.get(dateRange.get(i));
            if (typeCount == null) {
                continue;
            }
            s2.pk = s2.pk + typeCount.getSchemas().get("pk_count");
            s2.vl = s2.vl + typeCount.getSchemas().get("vl_count");
            s2.el = s2.el + typeCount.getSchemas().get("el_count");
            s2.il = s2.il + typeCount.getSchemas().get("il_count");
        }
        return new SchemaCount<>(growthRate(s1.pk, s2.pk),
                                 growthRate(s1.vl, s2.vl),
                                 growthRate(s1.el, s2.el),
                                 growthRate(s1.il, s2.il));
    }

    /**
     * @return num2 - num1 相比于 num1 的增长率
     * 只保留两位小数
     */
    private static Double growthRate(Long num1, Long num2) {
        assert num1 != null;
        assert num2 != null;
        assert num1 >= 0;
        assert num2 >= 0;
        if(num1 == 0 && num2 == 0){
            return 0.0;
        }
        if (num1 == 0) {
            return 1.0;
        }
        if (num2 == 0) {
            return -1.0;
        }
        // 保留两位小数
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String formattedResult = decimalFormat.format((num2 - num1) / ((double) num1));
        return Double.parseDouble(formattedResult);
    }

    public static List<String> getRangeDate(String start, int days) {
        List<String> dateList = new ArrayList<>();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(SAFE_DATE_FORMAT.parse(start));

            for (int i = 0; i <= Math.abs(days); i++) {
                dateList.add(SAFE_DATE_FORMAT.format(calendar.getTime()));
                if (days > 0) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateList.stream().sorted().collect(Collectors.toList());
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerEvEntity {
        @JsonProperty("total_growth_rate")
        public Double totalGrowthRate = 0.0;
        @JsonProperty("added_growth_rate")
        public Double addedGrowthRate = 0.0;

        @JsonProperty("pre_day_total")
        public Long preDayTotal = 0L;
        @JsonProperty("pre_day_added")
        public Long preDayAdded = 0L;
        @JsonProperty("type")
        public String type;
        @JsonProperty("count_by_schema")
        public List<ItemCount> countBySchema = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvCountEntity {
        @JsonProperty("vertices_count")
        public Long verticesCount = 0L;

        @JsonProperty("edges_count")
        public Long edgesCount = 0L;

        @JsonProperty("date")
        public String date;

        @JsonProperty("update_time")
        public String updateTime;

        @JsonProperty("vertices")
        public List<Item> vertices = new ArrayList<>();
        @JsonProperty("edges")
        public List<Item> edges = new ArrayList<>();

        public EvCountEntity(String date, GraphMetricsAPI.TypeCount typeCount) {
            this.date = date;
            if (typeCount == null) {
                return;
            }
            this.vertices = mergeItem(typeCount.getVertices());
            this.edges = mergeItem(typeCount.getEdges());
            this.updateTime = typeCount.getDatetime();
            this.verticesCount = mergeCount(typeCount.getVertices());
            this.edgesCount = mergeCount( typeCount.getEdges());
        }

        private List<Item> mergeItem(Map<String, Long> evTypeCount) {
            ArrayList<Item> list = new ArrayList<>();
            if (evTypeCount == null) {
                return list;
            }
            for (Map.Entry<String, Long> entry : evTypeCount.entrySet()) {
                Item item = new Item(entry.getKey(), entry.getValue());
                list.add(item);
            }
            return list;
        }

        private Long mergeCount(Map<String, Long> map) {
            Long count = 0L;
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                count += entry.getValue();
            }
            return count;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @JsonProperty("type")
        private String type;

        @JsonProperty("count")
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemCount {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("update_time")
        private String updateTime;

        @JsonProperty("count")
        private Long count;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchemaMetrics {
        @JsonProperty("weekly_growth_rate")
        private SchemaCount<Double> weeklyGrowthRate;

        @JsonProperty("item_count")
        private SchemaCount<Integer> itemCount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchemaCount<T> {
        @JsonProperty("pk")
        public T pk;

        @JsonProperty("vl")
        public T vl;

        @JsonProperty("el")
        public T el;

        @JsonProperty("il")
        public T il;
    }

}
