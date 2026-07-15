package com.busantrip.exception;

public class NaverLocalApiException extends RuntimeException {

    public NaverLocalApiException(String message) {
        super(message);
    }

    public NaverLocalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
