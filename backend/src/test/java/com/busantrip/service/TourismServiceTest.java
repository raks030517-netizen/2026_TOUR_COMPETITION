package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import com.busantrip.dto.external.tourism.LocalTourismApiResponse;
import com.busantrip.dto.external.tourism.RelatedTourismApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

// 2026-07-15 실호출로 확인: 공공데이터포털 API가 결과 0건일 때 "items" 필드를 빈 문자열("")로
// 내려준다. WebClientConfig의 Jackson 관대 역직렬화 설정으로 이게 null Items가 되는데,
// TourismService가 body.items().item()을 null 체크 없이 바로 부르다 NPE로 500이 났었다.
@ExtendWith(MockitoExtension.class)
class TourismServiceTest {

    @Mock
    private LocalTourismClient localTourismClient;

    @Mock
    private RelatedTourismClient relatedTourismClient;

    private TourismService service() {
        return new TourismService(localTourismClient, relatedTourismClient);
    }

    @Test
    void 기초지자체_관광지_결과가_없으면_빈_장소목록을_돌려준다() {
        LocalTourismApiResponse.Body body = new LocalTourismApiResponse.Body(null, 0, 1, 0);
        LocalTourismApiResponse response = new LocalTourismApiResponse(
                new LocalTourismApiResponse.Response(null, body));
        when(localTourismClient.getLocalTourism("202504", "26350", 1, 10))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service().getLocalTourism("202504", "26350", 1, 10))
                .assertNext(result -> assertThat(result.places()).isEmpty())
                .verifyComplete();
    }

    @Test
    void 연관_관광지_검색결과가_없으면_빈_장소목록을_돌려준다() {
        RelatedTourismApiResponse.Body body = new RelatedTourismApiResponse.Body(null, 0, 1, 0);
        RelatedTourismApiResponse response = new RelatedTourismApiResponse(
                new RelatedTourismApiResponse.Response(null, body));
        when(relatedTourismClient.searchRelatedTourism("202504", "26350", "존재하지않는장소12345", 1, 10))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service().searchRelatedTourism(
                        "202504", "26350", "존재하지않는장소12345", 1, 10))
                .assertNext(result -> assertThat(result.places()).isEmpty())
                .verifyComplete();
    }
}
