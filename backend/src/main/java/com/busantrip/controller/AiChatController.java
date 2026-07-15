package com.busantrip.controller;
import com.busantrip.service.GeminiChatService;import org.springframework.web.bind.annotation.*;import reactor.core.publisher.Mono;import java.util.*;
@RestController @RequestMapping("/api/ai") public class AiChatController{private final GeminiChatService service;public AiChatController(GeminiChatService service){this.service=service;}@PostMapping("/chat") public Mono<Response> chat(@RequestBody Request request){return service.chat(request);}public record Message(String role,String content,String createdAt){}public record Request(String message,List<Message> history,Map<String,Object> context){}public record Response(String message,List<String> suggestedActions){}
}
