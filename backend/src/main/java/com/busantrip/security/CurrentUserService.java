package com.busantrip.security;

import com.busantrip.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthException("AUTHENTICATION_REQUIRED", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public Long requireUserId(Authentication authentication) {
        return require(authentication).userId();
    }
}
