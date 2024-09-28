package org.apache.hugegraph.entity.query;

import org.apache.hugegraph.structure.traverser.Ranks;
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
public class RanksView {
    @JsonProperty("ranks")
    private Ranks ranks;

    @JsonProperty("rankslist")
    private List<Ranks> ranksList;
}
