package com.lcjian.lib.download.exception;

public class ConnectException extends Exception {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    public ConnectException(String message) {
        super(message);
    }

    public ConnectException(Throwable cause) {
        super(cause);
    }
}
