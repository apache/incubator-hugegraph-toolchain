package org.apache.hugegraph.entity.space;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuiltInEntity {
    @JsonProperty("init_space")
    public boolean initSpace = true;
    @JsonProperty("init_hlm")
    public boolean initHlm = true;
    @JsonProperty("init_covid19")
    public boolean initCovid19 = true;
}
