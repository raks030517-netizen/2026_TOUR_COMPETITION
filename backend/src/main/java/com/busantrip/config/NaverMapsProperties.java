package com.busantrip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "naver.maps")
public class NaverMapsProperties {

    private String keyId = "";
    private String key = "";
    private String baseUrl = "https://maps.apigw.ntruss.com/map-direction/v1";
}
