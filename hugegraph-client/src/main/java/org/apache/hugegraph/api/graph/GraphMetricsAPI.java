package org.apache.hugegraph.api.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.client.api.API;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GraphMetricsAPI extends API {

    private static final String GRAPH_METRICS_PATH = "graphspaces/%s/graphs" +
            "/%s/%s";

    private final String elementCountPath = "elementcount";
    private final String typeCountPath = "typecount";

    private final String evPath;
    private final String evPathByMonth;
    private final String typePath;
    private final String typePathByMonth;

    public GraphMetricsAPI(RestClient client, String graphSpace, String graph) {
        super(client);
        this.path(GRAPH_METRICS_PATH, graphSpace, graph, this.type());

        evPath = String.format("%s/%s", this.path(), elementCountPath);
        evPathByMonth = String.format("%s/%s/month", this.path(),
                elementCountPath);
        typePath = String.format("%s/%s", this.path(), typeCountPath);
        typePathByMonth = String.format("%s/%s/month", this.path(),
                typeCountPath);
    }

    @Override
    protected String type() {
        return HugeType.METRICS.string();
    }

    public long createEVCountJob() {
        RestResult result = this.client.post(evPath, ImmutableMap.of());
        return ((Number) result.readObject(Map.class).get("task_id")).longValue();
    }

    public ElementCount getEVCount(String dateStr) {
        try {
            RestResult result = this.client.get(evPath, dateStr);
            return result.readObject(ElementCount.class);
        } catch (ServerException e) {
            log.warn(e.exception());
            return null;
        }
    }

    public Map<String, ElementCount> getEVCountByMonth(String monthStr) {
        Map<String, ElementCount> result = new HashMap<>();

        RestResult resp = this.client.get(evPathByMonth, monthStr);

        // convert json to Map<String, TypeCount>
        Map<String, Object> elementCounts = resp.readObject(Map.class);
        for (Map.Entry<String, Object> entry : elementCounts.entrySet()) {
            String strDate = entry.getKey();
            Object elementCountMap = entry.getValue();
            ElementCount elementCount =
                    JsonUtil.fromJson(JsonUtil.toJson(elementCountMap), ElementCount.class);

            result.put(strDate, elementCount);
        }

        return result;
    }

    public long createTypeCountJob() {
        this.client.post(typePath, ImmutableMap.of());
        return 0L;
    }

    public TypeCount getTypeCount(String dateStr) {
        try {
            RestResult result = this.client.get(typePath, dateStr);
            return result.readObject(TypeCount.class);
        } catch (ServerException e) {
            log.warn(e.exception());
            return null;
        }
    }

    /**
     * @param from 起始时间
     * @param to   终止时间
     */
    public TypeCounts getTypeCounts(String from, String to) {
        try {
            RestResult result = this.client.get(typePath,
                    ImmutableMap.of("from", from, "to", to));
            return result.readObject(TypeCounts.class);
        } catch (ServerException e) {
            log.warn(e.exception());
            return null;
        }
    }

    public Map<String, TypeCount> getTypeCountByMonth(String monthStr) {
        Map<String, TypeCount> result = new HashMap<>();

        RestResult resp = this.client.get(typePathByMonth, monthStr);

        // convert json to Map<String, TypeCount>
        Map<String, Object> typeCounts = resp.readObject(Map.class);
        for (Map.Entry<String, Object> entry : typeCounts.entrySet()) {
            String strDate = entry.getKey();
            Object typeCountMap = entry.getValue();
            TypeCount typeCount =
                    JsonUtil.fromJson(JsonUtil.toJson(typeCountMap), TypeCount.class);

            result.put(strDate, typeCount);
        }

        return result;
    }

    @Data
    public static class ElementCount {
        // 结果统计时间
        String datetime;
        long vertices;
        long edges;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeCounts {
        @JsonProperty("type_counts")
        Map<String, TypeCount> typeCounts;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeCount {
        // 结果统计时间
        @JsonProperty("datetime")
        String datetime;
        @JsonProperty("vertices")
        Map<String, Long> vertices;
        @JsonProperty("edges")
        Map<String, Long> edges;
        @JsonProperty("schemas")
        Map<String, Long> schemas;
    }
}
