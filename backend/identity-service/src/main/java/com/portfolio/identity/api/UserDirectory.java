package com.portfolio.identity.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only contract other services use to show who wrote something. They reference users by id
 * only and never touch identity tables or entities directly.
 */
public interface UserDirectory {

    Optional<UserSummary> find(Long userId);

    Map<Long, UserSummary> findAll(Collection<Long> userIds);

    /** Public, non-sensitive view of a user (no email, no role). */
    record UserSummary(Long id, String displayName, String avatarUrl) {
        public static UserSummary unknown(Long id) {
            return new UserSummary(id, "Người dùng đã xoá", null);
        }
    }
}
