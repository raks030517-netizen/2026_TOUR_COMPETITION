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
    private Gemma gemma = new Gemma();
    private Weather weather = new Weather();
    private Tourism tourism = new Tourism();
    private Avi avi = new Avi();

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
    public static class Gemma {
        private String apiKey = "";
        private String model = "";
        private String fallbackModel = "gemini-3.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }

    @Getter
    @Setter
    public static class Weather {
        private String serviceKey = "";
        private String baseUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    }

    @Getter
    @Setter
    public static class Tourism {
        private String serviceKey = "";
        private Local local = new Local();
        private Related related = new Related();

        @Getter
        @Setter
        public static class Local {
            private String baseUrl = "https://apis.data.go.kr/B551011/LocgoHubTarService1";
        }

        @Getter
        @Setter
        public static class Related {
            private String baseUrl = "https://apis.data.go.kr/B551011/TarRlteTarService1";
        }
    }

    @Getter
    @Setter
    public static class Avi {
        private String serviceKey = "";
        private String baseUrl = "https://apis.data.go.kr/6260000/BusanITSAVI";
    }
}
