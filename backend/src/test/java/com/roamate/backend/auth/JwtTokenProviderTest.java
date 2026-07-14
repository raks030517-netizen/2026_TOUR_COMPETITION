package com.roamate.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-key-must-be-long-enough-for-hs512-signing-0123456789";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600);

    @Test
    void 발급한_토큰에서_사용자_id를_그대로_복원한다() {
        String token = jwtTokenProvider.createToken(42L);

        Long userId = jwtTokenProvider.parseUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void 형식이_잘못된_토큰은_null을_반환한다() {
        Long userId = jwtTokenProvider.parseUserId("not-a-jwt-token");

        assertThat(userId).isNull();
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_검증에_실패한다() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "another-completely-different-secret-key-also-long-enough-9876543210", 3600);
        String token = otherProvider.createToken(1L);

        Long userId = jwtTokenProvider.parseUserId(token);

        assertThat(userId).isNull();
    }
}
