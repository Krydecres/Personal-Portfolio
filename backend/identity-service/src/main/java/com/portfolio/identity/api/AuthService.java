package com.portfolio.identity.api;

import com.portfolio.identity.api.dto.IdentityDtos.GoogleProfile;
import com.portfolio.identity.api.dto.IdentityDtos.LoginRequest;
import com.portfolio.identity.api.dto.IdentityDtos.RegisterRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UserResponse;

import java.time.Instant;

/** Service contract: authentication and session lifecycle. */
public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult loginWithGoogle(GoogleProfile profile);

    /** Exchanges a refresh token for a new access token and a new (rotated) refresh token. */
    AuthResult refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    /**
     * Result of a successful authentication.
     *
     * @param refreshToken raw refresh token; only ever written to an HttpOnly cookie, never logged
     */
    record AuthResult(
            String accessToken,
            long accessTokenExpiresIn,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            UserResponse user) {

        @Override
        public String toString() {
            return "AuthResult[user=" + user.id() + ", tokens=***]";
        }
    }
}
