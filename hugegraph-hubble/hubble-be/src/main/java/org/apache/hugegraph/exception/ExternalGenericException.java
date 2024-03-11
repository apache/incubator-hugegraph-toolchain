package org.apache.hugegraph.exception;

import org.apache.hugegraph.rest.ClientException;

public class ExternalGenericException extends ParameterizedException{

    private int status;

    public ExternalGenericException(Exception e, Object... args) {
        this(400, e.getMessage(), e.getCause(), args);
    }

    public ExternalGenericException(ServerException e, Object... args) {
        this(400, e.getMessage(), e.getCause(), args);
    }

    public ExternalGenericException(String message, Object... args) {
        this(400, message, args);
    }

    public ExternalGenericException(int status, String message, Object... args) {
        this(status, message, null, args);
    }

    public ExternalGenericException(String message, Throwable cause, Object... args) {
        this(400, message, cause, args);
    }

    public ExternalGenericException(int status, String message, Throwable cause,
                             Object... args) {
        super(message, cause, args);
        this.status = status;
    }

    public int status() {
        return this.status;
    }
}
