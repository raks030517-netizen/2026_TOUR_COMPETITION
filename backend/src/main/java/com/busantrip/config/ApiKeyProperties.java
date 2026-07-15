package com.busantrip.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external-api")
public class ApiKeyProperties {

    private Naver naver = new Naver();
    private Gemini gemini = new Gemini();
    private Weather weather = new Weather();
    private Tourism tourism = new Tourism();

    @Getter
    @Setter
    public static class Naver {
        private Search search = new Search();

        @Getter
        @Setter
        public static class Search {
            private String clientId = "";
            private String clientSecret = "";
        }
    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey = "";
    }

    @Getter
    @Setter
    public static class Weather {
        private String serviceKey = "";
        private String baseUrl = "";
    }

    /**
     * 한국관광공사 API
     */
    @Getter
    @Setter
    public static class Tourism {

        private String serviceKey = "";
        private Local local = new Local();
        private Related related = new Related();

        /**
         * 기초지자체 중심 관광지 API 설정
         * - 여행의 첫 장소를 찾는 역할
         */
        @Getter
        @Setter
        public static class Local{
            private String baseUrl = "";
        }

        /**
         * 관광지별 연관 관광지 API 설정
         * - 첫 장소에서 이어지는 다음 장소를 찾는 역할
         */
        @Getter
        @Setter
        public static class Related{
            private String baseUrl = "";
        }

    }


}

