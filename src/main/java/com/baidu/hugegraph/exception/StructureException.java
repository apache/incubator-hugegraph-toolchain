package com.baidu.hugegraph.exception;

/**
 * Created by liningrui on 2017/6/5.
 */
public class StructureException extends RuntimeException {

    private static final long serialVersionUID = -8711375282196157058L;

    public StructureException(String message) {
        super(message);
    }

    public StructureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
