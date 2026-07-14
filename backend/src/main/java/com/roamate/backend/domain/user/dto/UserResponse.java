package com.roamate.backend.domain.user.dto;

import com.roamate.backend.domain.user.User;

public record UserResponse(Long id, String email, String nickname) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
