package com.busantrip.exception;

import org.springframework.http.HttpStatus;

public class RouteApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public RouteApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public RouteApiException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
