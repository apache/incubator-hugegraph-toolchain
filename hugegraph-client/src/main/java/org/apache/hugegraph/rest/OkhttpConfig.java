package org.apache.hugegraph.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class OkhttpConfig {
    private String user;
    private String password;
    private String token;
    private Integer timeout;
    private Integer maxTotal;
    private Integer maxPerRoute;
    private Integer idleTime;
    private String trustStoreFile;
    private String trustStorePassword;
}
