package org.apache.hugegraph.entity.graphs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphEntity {
    public String cluster;
    public String graphSpace;
    public String graph;
}
