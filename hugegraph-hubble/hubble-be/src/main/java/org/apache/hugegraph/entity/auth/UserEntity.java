package org.apache.hugegraph.entity.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.common.Identifiable;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity implements Identifiable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("user_nickname")
    private String nickname;

    @JsonProperty("user_email")
    private String email;

    @JsonProperty("user_password")
    private String password;

    @JsonProperty("user_phone")
    private String phone;

    @JsonProperty("user_avatar")
    private String avatar;

    @JsonProperty("user_description")
    private String description;

    @JsonProperty("user_create")
    protected Date create;
    @JsonProperty("user_update")
    protected Date update;
    @JsonProperty("user_creator")
    protected String creator;

    @JsonProperty("adminSpaces")
    protected List<String> adminSpaces;

    @JsonProperty("resSpaces")
    protected List<String> resSpaces;

    @JsonProperty("spacenum")
    protected Integer spacenum;

    @JsonProperty("is_superadmin")
    private boolean isSuperadmin;
}


