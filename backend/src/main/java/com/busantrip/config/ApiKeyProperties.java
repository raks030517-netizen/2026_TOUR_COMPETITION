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
    }
}

