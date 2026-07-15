package com.busantrip.exception;

public class GemmaApiException extends RuntimeException {

    public GemmaApiException(String message) {
        super(message);
    }

    public GemmaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
