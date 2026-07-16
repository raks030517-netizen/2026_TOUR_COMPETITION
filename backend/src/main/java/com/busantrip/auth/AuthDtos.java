package com.busantrip.auth;

import com.busantrip.security.AuthenticatedUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 80) String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record UserResponse(Long id, String email, String displayName) {

        public static UserResponse from(AuthenticatedUser user) {
            return new UserResponse(user.userId(), user.email(), user.displayName());
        }
    }

    public record EmailAvailabilityResponse(String email, boolean available) {
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
