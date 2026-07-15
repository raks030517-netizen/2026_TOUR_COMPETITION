package com.busantrip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.busantrip.dto.request.ChatRequest;
import com.busantrip.dto.response.ChatResponse;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.service.TravelChatService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TravelChatService travelChatService;

    @Test
    void 정상_메시지는_200과_함께_TravelChatService의_응답을_그대로_돌려준다() {
        List<PlaceResponse> places = List.of(new PlaceResponse("카페", "주소", 35.1, 129.1));
        when(travelChatService.chat(any()))
                .thenReturn(Mono.just(new ChatResponse("이런 곳은 어떠세요? 1곳을 찾았어요.", places)));

        webTestClient.post().uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequest("부산 카페 추천"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("이런 곳은 어떠세요? 1곳을 찾았어요.")
                .jsonPath("$.places[0].name").isEqualTo("카페");
    }

    @Test
    void 빈_메시지는_400을_반환한다() {
        webTestClient.post().uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequest(""))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
