package com.busantrip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "odsay.api")
public class OdsayProperties {

    private String key = "";
    private String baseUrl = "https://api.odsay.com/v1/api";
}
