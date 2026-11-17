package com.portfolio.identity.internal.service;

import com.portfolio.identity.api.dto.IdentityDtos.UserResponse;
import com.portfolio.identity.internal.domain.UserAccount;

final class UserMapper {

    private UserMapper() {}

    static UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getPasswordHash() != null,
                user.getGoogleSubject() != null);
    }
}
