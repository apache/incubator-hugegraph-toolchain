package org.apache.hugegraph.entity.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserView {

    @JsonProperty("user_id")
    private String id;

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("roles")
    private List<RoleEntity> roles;

    public void addRole(RoleEntity g) {
        this.roles.add(g);
    }

}
