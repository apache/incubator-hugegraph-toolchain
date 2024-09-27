package org.apache.hugegraph.entity.algorithm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KneighborEntity {
    @JsonProperty("source")
    private Object source;

    @JsonProperty("direction")
    private Direction direction;

    @JsonProperty("max_depth")
    private int maxDepth;

    @JsonProperty("label")
    private String label;

    @JsonProperty("max_degree")
    private long maxDegree = Traverser.DEFAULT_MAX_DEGREE;

    @JsonProperty("limit")
    private int limit;
}
