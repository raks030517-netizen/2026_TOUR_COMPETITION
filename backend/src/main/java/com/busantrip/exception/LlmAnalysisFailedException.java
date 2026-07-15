package com.busantrip.exception;

public class LlmAnalysisFailedException extends RuntimeException {

    public LlmAnalysisFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
