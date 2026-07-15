package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;


@Component
public class RelatedTourismClient {

     private static final String AREA_BASED_LIST_PATH = "/areaBasedList1";
     private static final String SEARCH_KEYWORD_PATH = "/searchKeyword1";

     private static final String BUSAN_AREA_CODE = "26";
     private static final String MOBILE_OS = "ETC";
     private static final String MOBILE_APP = "busan-ai-map";

     private final WebClient webClient;
     private final String serviceKey;

    public RelatedTourismClient(
            WebClient.Builder webClientBuilder,
            ApiKeyProperties apiKeyProperties
    ) {
        String baseUrl = apiKeyProperties
                .getTourism()
                .getRelated()
                .getBaseUrl()
                .trim();

        this.serviceKey = apiKeyProperties
                .getTourism()
                .getServiceKey()
                .trim();

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }


    public Mono<String> getRelatedTourismByArea(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ){
         return request(
                 AREA_BASED_LIST_PATH,
                 baseYm,
                 signguCd,
                 null,
                 pageNo,
                 numOfRows
         );
    }


    /**
     * 관광지 이름을 검색하여 연관 관광지를 조회한다.
     *
     * @param baseYm    조회 기준연월(YYYYMM)
     * @param signguCd  시군구 코드
     * @param keyword   관광지 검색어
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     */
    public Mono<String> searchRelatedTourism(
            String baseYm,
            String signguCd,
            String keyword,
            int pageNo,
            int numOfRows
    ) {
         if (keyword == null || keyword.isBlank()) {
              return Mono.error(
                      new IllegalArgumentException("검색어는 비어 있을 수 없습니다.")
              );
         }

         return request(
                 SEARCH_KEYWORD_PATH,
                 baseYm,
                 signguCd,
                 keyword,
                 pageNo,
                 numOfRows
         );
    }

    private Mono<String> request(
            String path,
            String baseYm,
            String signguCd,
            String keyword,
            int pageNo,
            int numOfRows
    ) {
        return webClient.get()

                .uri(uriBuilder -> {

                    UriBuilder builder = uriBuilder
                            // 전달받은 API 경로 사용
                            .path(path)
                            .queryParam("serviceKey", "{serviceKey}")
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("baseYm", baseYm)
                            .queryParam("areaCd", BUSAN_AREA_CODE)
                            .queryParam("signguCd", signguCd)
                            .queryParam("_type", "json");

                    // 검색 API를 호출할 때만 검색어 추가
                    if (keyword != null && !keyword.isBlank()) {
                        builder.queryParam("keyword", keyword);
                    }

                    return builder.build(serviceKey);
                })
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("응답 본문 없음")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "연관 관광지 API 호출 실패: "
                                                        + response.statusCode()
                                                        + ", 응답: " + errorBody
                                        )
                                ))
                )
                .bodyToMono(String.class);
    }


}
