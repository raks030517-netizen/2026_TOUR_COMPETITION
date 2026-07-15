package com.busantrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BusanTripApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusanTripApplication.class, args);
    }
}