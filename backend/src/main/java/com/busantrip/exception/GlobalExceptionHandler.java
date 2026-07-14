package com.busantrip.exception;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> handleNotImplemented() {
        // 내부 설정이나 예외 원문이 응답에 포함되지 않도록 고정 문구만 반환합니다.
        return Map.of("message", "아직 구현되지 않은 기능입니다.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpectedException(Exception e) {
        log.error("요청 처리 중 예기치 않은 오류가 발생했습니다.", e);

        return Map.of("message", "요청 처리 중 오류가 발생했습니다.");
    }
}

