package com.portfolio.common.security;

/**
 * The authenticated principal as seen by business services. Services receive this explicit value
 * object instead of reaching into the security framework, which keeps their contracts independent
 * of how authentication is done (JWT today, something else tomorrow).
 */
public record Caller(Long userId, String role) {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean owns(Long ownerId) {
        return userId != null && userId.equals(ownerId);
    }
}
