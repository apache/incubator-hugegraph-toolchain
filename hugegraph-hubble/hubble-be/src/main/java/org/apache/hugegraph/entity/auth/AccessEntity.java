package org.apache.hugegraph.entity.auth;

import java.util.List;
import java.util.Set;

import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.HugeResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessEntity {

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("target_name")
    private String targetName;

    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("graphspace")
    private String graphSpace;

    @JsonProperty("graph")
    private String graph;

    @JsonProperty("permissions")
    private Set<HugePermission> permissions;

    @JsonProperty("target_description")
    protected String description;

    @JsonProperty("target_resources")
    protected List<HugeResource> resources;

    public void addPermission(HugePermission p) {
        permissions.add(p);
    }
}
