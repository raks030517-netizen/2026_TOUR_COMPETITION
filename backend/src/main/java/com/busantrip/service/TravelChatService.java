package com.busantrip.service;

import com.busantrip.dto.request.ChatRequest;
import com.busantrip.dto.response.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class TravelChatService {

    public ChatResponse chat(ChatRequest request) {
        // 추후 LLM 분석, 장소 검색, 날씨 조회 결과를 조합합니다.
        throw new UnsupportedOperationException("채팅 추천 기능 연동 예정");
    }
}

