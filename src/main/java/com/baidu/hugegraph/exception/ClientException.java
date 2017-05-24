package com.baidu.hugegraph.exception;

public class ClientException extends RuntimeException {

    private static final long serialVersionUID = -8711375282196157058L;

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
