package org.apache.hugegraph.entity.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SameneighborsbatchView {

    @JsonProperty("same_neighbors")
    public List<List<Object>> sameNeighbors;

}
