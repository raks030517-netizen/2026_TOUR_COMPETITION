package com.busantrip.service;

import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NaverLocalSearchService {

    public List<PlaceResponse> search(String query) {
        // 추후 네이버 지역 검색 결과를 서비스 응답으로 변환합니다.
        throw new UnsupportedOperationException("네이버 지역 검색 API 연동 예정");
    }
}

