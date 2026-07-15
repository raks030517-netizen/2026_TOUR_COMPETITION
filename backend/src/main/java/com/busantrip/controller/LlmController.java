package com.busantrip.controller;

import com.busantrip.dto.llm.LlmAnalyzeRequest;
import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.service.LlmQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmQueryService llmQueryService;

    public LlmController(LlmQueryService llmQueryService) {
        this.llmQueryService = llmQueryService;
    }

    @PostMapping("/analyze")
    public Mono<SearchCondition> analyze(@Valid @RequestBody LlmAnalyzeRequest request) {
        return llmQueryService.analyze(request.message());
    }
}
