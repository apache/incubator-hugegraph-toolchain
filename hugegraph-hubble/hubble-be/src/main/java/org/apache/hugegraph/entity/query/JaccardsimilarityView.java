package org.apache.hugegraph.entity.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JaccardsimilarityView {

    @JsonProperty("jaccardsimilarity")
    private Object jaccardsimilarity;
}
