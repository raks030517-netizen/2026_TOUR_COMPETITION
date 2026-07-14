package com.busantrip.client;

import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaverLocalClient {

    public List<PlaceResponse> search(String query) {
        // 추후 WebClient로 네이버 지역 검색 API를 호출합니다.
        throw new UnsupportedOperationException("네이버 지역 검색 API 호출 예정");
    }
}

