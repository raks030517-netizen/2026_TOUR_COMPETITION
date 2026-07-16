package com.busantrip.controller;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.response.ConfigStatusResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final ApiKeyProperties apiKeyProperties;

    public SystemController(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/config-status")
    public ConfigStatusResponse configStatus() {
        ApiKeyProperties.Naver.Search search = apiKeyProperties.getNaver().getSearch();
        return new ConfigStatusResponse(
                isConfigured(search.getClientId()) && isConfigured(search.getClientSecret()),
                isConfigured(apiKeyProperties.getGemma().getApiKey()),
                isConfigured(apiKeyProperties.getWeather().getServiceKey())
        );
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}

