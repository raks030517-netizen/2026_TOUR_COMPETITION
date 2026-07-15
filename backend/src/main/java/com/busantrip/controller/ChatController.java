package com.busantrip.controller;

import com.busantrip.dto.request.ChatRequest;
import com.busantrip.dto.response.ChatResponse;
import com.busantrip.service.TravelChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final TravelChatService travelChatService;

    public ChatController(TravelChatService travelChatService) {
        this.travelChatService = travelChatService;
    }

    @PostMapping
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return travelChatService.chat(request);
    }
}
