package com.busantrip.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NonBusanRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleNonBusanRequestException() {
        return Map.of("message", "부산 지역 요청만 분석할 수 있습니다.");
    }

    @ExceptionHandler(GemmaConfigurationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleGemmaConfigurationException() {
        return Map.of("message", "Gemma API 설정을 확인해 주세요.");
    }

    @ExceptionHandler(GemmaAuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleGemmaAuthenticationException() {
        return Map.of("message", "Gemma API 인증에 실패했습니다.");
    }

    @ExceptionHandler(GemmaApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleGemmaApiException() {
        return Map.of("message", "Gemma API 호출 중 오류가 발생했습니다.");
    }

    @ExceptionHandler(LlmAnalysisFailedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleLlmAnalysisFailedException() {
        return Map.of("message", "요청을 분석하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            WebExchangeBindException.class,
            ServerWebInputException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException() {
        return Map.of("message", "요청 파라미터를 확인해 주세요.");
    }

    @ExceptionHandler(NaverLocalAuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleNaverAuthenticationException() {
        return Map.of("message", "네이버 지역 검색 API 인증에 실패했습니다.");
    }

    @ExceptionHandler(NaverLocalApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleNaverApiException() {
        return Map.of("message", "네이버 지역 검색 API 호출 중 오류가 발생했습니다.");
    }

    @ExceptionHandler(AviTrafficConfigurationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleAviConfigurationException() {
        return Map.of("message", "AVI 교통량 API 설정을 확인해 주세요.");
    }

    @ExceptionHandler(AviTrafficAuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleAviAuthenticationException() {
        return Map.of("message", "AVI 교통량 API 인증에 실패했습니다.");
    }

    @ExceptionHandler(AviTrafficApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleAviApiException() {
        return Map.of("message", "AVI 교통량 정보를 불러오지 못했습니다.");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> handleNotImplemented() {
        // 내부 설정이나 예외 원문이 응답에 포함되지 않도록 고정 문구만 반환합니다.
        return Map.of("message", "아직 구현되지 않은 기능입니다.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpectedException(Exception e) {

        log.error("예상하지 못한 요청 처리 오류가 발생했습니다.", e);

        return Map.of("message", "요청 처리 중 오류가 발생했습니다.");
    }
}
