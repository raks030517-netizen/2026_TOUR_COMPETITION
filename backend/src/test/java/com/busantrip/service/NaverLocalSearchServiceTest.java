package com.busantrip.service;

import static org.mockito.Mockito.when;

import com.busantrip.client.NaverLocalClient;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NaverLocalSearchServiceTest {

    @Mock
    private NaverLocalClient naverLocalClient;

    @Test
    void NaverLocalClient의_결과를_그대로_전달한다() {
        List<PlaceResponse> expected = List.of(new PlaceResponse("해운대", "부산 해운대구", 35.16, 129.16));
        when(naverLocalClient.search("해운대")).thenReturn(Mono.just(expected));

        NaverLocalSearchService service = new NaverLocalSearchService(naverLocalClient);

        StepVerifier.create(service.search("해운대"))
                .expectNext(expected)
                .verifyComplete();
    }
}
