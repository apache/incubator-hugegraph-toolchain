package org.apache.hugegraph.entity.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.common.Identifiable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity implements Identifiable {

    @JsonProperty("role_id")
    private String id;

    @JsonProperty("role_name")
    private String name;
}
