package com.busantrip.exception;

public class AviTrafficApiException extends RuntimeException {

    public AviTrafficApiException(String message) {
        super(message);
    }

    public AviTrafficApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
