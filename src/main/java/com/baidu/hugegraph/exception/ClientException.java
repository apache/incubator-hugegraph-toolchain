package com.baidu.hugegraph.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientException extends IllegalArgumentException {

    @JsonProperty
    private String exception;
    @JsonProperty
    private String message;
    @JsonProperty
    private String cause;

    public String exception() {
        return exception;
    }

    public String message() {
        return message;
    }

    public String cause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
