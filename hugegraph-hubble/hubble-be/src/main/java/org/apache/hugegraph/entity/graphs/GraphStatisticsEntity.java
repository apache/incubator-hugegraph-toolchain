package org.apache.hugegraph.entity.graphs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphStatisticsEntity {
    @JsonProperty("storage")
    public long storage;
    @JsonProperty("vertex_count")
    public String vertexCount;
    @JsonProperty("edge_count")
    public String edgeCount;
    @JsonProperty("update_time")
    public String updateTime;
    @JsonProperty("vertices")
    public Map<String, Object> vertices;
    @JsonProperty("edges")
    public Map<String, Object> edges;

    public static GraphStatisticsEntity emptyEntity() {
        GraphStatisticsEntity empty = new GraphStatisticsEntity();
        empty.setStorage(0);
        empty.setVertexCount("0");
        empty.setEdgeCount("0");
        empty.setVertices(ImmutableMap.of());
        empty.setEdges(ImmutableMap.of());
        empty.setUpdateTime("1970-01-01 00:00:00.000");
        return empty;
    }
}
