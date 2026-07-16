package com.busantrip.controller;

import com.busantrip.service.GemmaChatService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GemmaChatService service;

    public AiChatController(GemmaChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public Mono<Response> chat(@RequestBody Request request) {
        return service.chat(request);
    }

    public record Message(String role, String content, String createdAt) {
    }

    public record Request(String message, List<Message> history, Map<String, Object> context) {
    }

    public record Response(String message, List<String> suggestedActions) {
    }
}
