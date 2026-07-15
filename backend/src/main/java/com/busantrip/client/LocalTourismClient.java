package com.busantrip.client;


import com.busantrip.config.ApiKeyProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class LocalTourismClient {

    private static final String AREA_BASED_LIST_PATH = "/areaBasedList1";

    private static final String BUSAN_AREA_CODE = "26";

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "busan-ai-map";

    private final WebClient webClient;
    private final String serviceKey;

    public LocalTourismClient(WebClient.Builder webClientBuilder, ApiKeyProperties apiKeyProperties) {
        this.webClient = webClientBuilder
                .baseUrl(
                        apiKeyProperties
                                .getTourism()
                                .getLocal()
                                .getBaseUrl()
                )
                .build();

        this.serviceKey = apiKeyProperties
                .getTourism()
                .getServiceKey()
                .trim();
    }

    /**
     * 부산의 기초지자체를 기준으로 중심 관광지 정보를 조회한다.
     * @param baseYm    조회 기준연월(YYYYMM)
     * @param signguCd  시군구 코드
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return 한국관광공사 API의 JSON 응답
     */
    public Mono<String> getLocalTourism(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(AREA_BASED_LIST_PATH)
                        .queryParam("serviceKey", "{serviceKey}")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("MobileOS", MOBILE_OS)
                        .queryParam("MobileApp", MOBILE_APP)
                        .queryParam("baseYm", baseYm)
                        .queryParam("areaCd", BUSAN_AREA_CODE)
                        .queryParam("signguCd", signguCd)
                        .queryParam("_type", "json")
                        .build(serviceKey)
                ).retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("응답 본문 없음")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "기초지자체 중심 관광지 API 호출 실패: "
                                                        + response.statusCode()
                                                        + ", 응답: " + errorBody
                                        )
                                ))
                )
                .bodyToMono(String.class);
    }
}
