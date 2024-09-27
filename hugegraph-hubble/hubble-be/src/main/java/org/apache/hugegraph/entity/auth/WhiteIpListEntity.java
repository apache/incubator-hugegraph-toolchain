package org.apache.hugegraph.entity.auth;

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
public class WhiteIpListEntity {
    @JsonProperty("action")
    private String action;

    @JsonProperty("ips")
    private List<String> ips;
}
