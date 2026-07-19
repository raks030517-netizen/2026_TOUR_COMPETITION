package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.external.tourism.LocalTourismApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TourismServiceTest {

    @Mock
    private LocalTourismClient localTourismClient;

    @Mock
    private RelatedTourismClient relatedTourismClient;

    private TourismService service(boolean tourismApiConfigured) {
        ApiKeyProperties properties = new ApiKeyProperties();
        if (tourismApiConfigured) {
            properties.getTourism().setServiceKey("test-key");
        }
        return new TourismService(
                localTourismClient,
                relatedTourismClient,
                new CategoryKeywordResolver(),
                new DemoTourismCatalog(),
                properties
        );
    }

    @Test
    void localTourismReturnsAnEmptyListWhenTheLiveApiHasNoItems() {
        LocalTourismApiResponse.Body body = new LocalTourismApiResponse.Body(null, 0, 1, 0);
        LocalTourismApiResponse response = new LocalTourismApiResponse(
                new LocalTourismApiResponse.Response(null, body));
        when(localTourismClient.getLocalTourism("202504", "26350", 1, 10))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service(true).getLocalTourism("202504", "26350", 1, 10))
                .assertNext(result -> assertThat(result.places()).isEmpty())
                .verifyComplete();
    }

    @Test
    void searchReturnsAnEmptyListWhenTheConfiguredLiveApiHasNoItems() {
        LocalTourismApiResponse.Body body = new LocalTourismApiResponse.Body(null, 0, 1, 0);
        LocalTourismApiResponse response = new LocalTourismApiResponse(
                new LocalTourismApiResponse.Response(null, body));
        when(localTourismClient.getLocalTourism("202504", "26350", 1, 20))
                .thenReturn(Mono.just(response));

        StepVerifier.create(service(true).searchRelatedTourism("202504", "26350", "없는장소", 1, 10))
                .assertNext(result -> assertThat(result.places()).isEmpty())
                .verifyComplete();
    }

    @Test
    void searchReturnsDemoPlacesWhenTheTourismApiKeyIsMissing() {
        StepVerifier.create(service(false).searchRelatedTourism("202504", "26350", "야경", 1, 10))
                .assertNext(result -> {
                    assertThat(result.places()).isNotEmpty();
                    assertThat(result.places()).allSatisfy(place -> {
                        assertThat(place.latitude()).isNotNull();
                        assertThat(place.longitude()).isNotNull();
                    });
                })
                .verifyComplete();
    }
}
