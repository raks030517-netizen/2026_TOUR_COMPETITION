package com.roamate.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public static LoginResponse of(String accessToken, long expiresInSeconds) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
