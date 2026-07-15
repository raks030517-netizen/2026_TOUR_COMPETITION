package com.busantrip.service;

import com.busantrip.client.NaverLocalClient;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NaverLocalSearchService {

    private final NaverLocalClient naverLocalClient;

    public NaverLocalSearchService(NaverLocalClient naverLocalClient) {
        this.naverLocalClient = naverLocalClient;
    }

    public Mono<List<PlaceResponse>> search(String query) {
        return naverLocalClient.search(query);
    }
}
