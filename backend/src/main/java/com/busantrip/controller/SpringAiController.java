package com.busantrip.controller;

import com.busantrip.dto.ai.AiTravelBrief;
import com.busantrip.dto.llm.LlmAnalyzeRequest;
import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.service.SpringAiMappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
public class SpringAiController {

    private final SpringAiMappingService mappingService;

    public SpringAiController(SpringAiMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @PostMapping("/search-condition")
    public Mono<SearchCondition> searchCondition(@Valid @RequestBody LlmAnalyzeRequest request) {
        return mappingService.mapSearchCondition(request.message());
    }

    @PostMapping("/travel-brief")
    public Mono<AiTravelBrief> travelBrief(@Valid @RequestBody LlmAnalyzeRequest request) {
        return mappingService.mapTravelBrief(request.message());
    }
}
