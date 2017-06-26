package com.baidu.hugegraph.structure.gremlin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/6/23.
 */
public class Response {

    private static class Status {
        @JsonProperty
        private String message;
        @JsonProperty
        private int code;
        @JsonProperty
        private Map<String, ?> attributes;
    }

    @JsonProperty
    private String requestId;
    @JsonProperty
    private Status status;
    @JsonProperty
    private ResultSet result;

    public String requestId() {
        return requestId;
    }

    public Status status() {
        return status;
    }

    public ResultSet result() {
        return result;
    }
}
