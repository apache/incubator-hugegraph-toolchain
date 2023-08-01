package org.apache.hugegraph.spark.connector.exception;

public class LoadException extends RuntimeException {

    private static final long serialVersionUID = 5504623124963497613L;

    public LoadException(String message) {
        super(message);
    }

    public LoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadException(String message, Object... args) {
        super(String.format(message, args));
    }

    public LoadException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }
}
