package com.baidu.hugegraph.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientException extends IllegalArgumentException {

    private int status;
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
        super("");
        this.message = message;
    }

    public ClientException(String message, Throwable cause) {
        super("", cause);
        this.message = message;
    }

    public void status(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
