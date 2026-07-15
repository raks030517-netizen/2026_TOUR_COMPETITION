package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.busantrip.exception.NonBusanRequestException;
import org.junit.jupiter.api.Test;

class BusanRequestValidatorTest {

    private final BusanRequestValidator validator = new BusanRequestValidator();

    @Test
    void acceptsBusanAreaRequestWithoutBusanPrefix() {
        assertThatCode(() -> validator.validateRequest("광안리에서 조개구이를 먹고 싶어요."))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExplicitOutsideBusanRequest() {
        assertThatThrownBy(() -> validator.validateRequest("서울 강남에서 관광지를 찾고 싶어요."))
                .isInstanceOf(NonBusanRequestException.class);
    }
}
