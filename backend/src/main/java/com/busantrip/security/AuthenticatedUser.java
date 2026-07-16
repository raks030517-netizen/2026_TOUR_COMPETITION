package com.busantrip.security;

import java.security.Principal;

public record AuthenticatedUser(Long userId, String email, String displayName) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
