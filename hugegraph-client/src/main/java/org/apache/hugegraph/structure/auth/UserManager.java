package org.apache.hugegraph.structure.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserManager {

    @JsonProperty("user")
    private String user;
    @JsonProperty("type")
    private HugePermission type;
    @JsonProperty("graphspace")
    private String graphSpace;

    public String user() {
        return user;
    }

    public void user(String user) {
        this.user = user;
    }

    public HugePermission type() {
        return type;
    }

    public void type(HugePermission type) {
        this.type = type;
    }

    public String graphSpace() {
        return graphSpace;
    }

    public void graphSpace(String graphSpace) {
        this.graphSpace = graphSpace;
    }
}
