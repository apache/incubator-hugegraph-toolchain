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
public class ResourceallocationEntity {
    @JsonProperty("vertex")
    private Object vertex;

    @JsonProperty("other")
    private Object other;

    @JsonProperty("direction")
    private Direction direction;

    @JsonProperty("label")
    private String label;

    @JsonProperty("max_degree")
    private long maxDegree = Traverser.DEFAULT_MAX_DEGREE;

    @JsonProperty("limit")
    private int limit;
}
