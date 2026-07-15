package com.busantrip.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> handleNotImplemented() {
        // 내부 설정이나 예외 원문이 응답에 포함되지 않도록 고정 문구만 반환합니다.
        return Map.of("message", "아직 구현되지 않은 기능입니다.");
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 올바르지 않습니다.");
        return Map.of("message", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpectedException(Exception ex) {
        // 응답엔 고정 문구만 내려주되, 서버 로그엔 원인을 남겨 디버깅 가능하게 한다.
        log.error("처리되지 않은 예외 발생", ex);
        return Map.of("message", "요청 처리 중 오류가 발생했습니다.");
    }
}

