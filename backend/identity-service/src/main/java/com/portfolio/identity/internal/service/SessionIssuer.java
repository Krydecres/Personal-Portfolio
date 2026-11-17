package com.portfolio.identity.internal.service;

import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.internal.domain.UserAccount;
import org.springframework.stereotype.Component;

/** Creates a new sign-in session: an access token plus a refresh token in a new family. */
@Component
class SessionIssuer {

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenManager refreshTokens;

    SessionIssuer(AccessTokenIssuer accessTokens, RefreshTokenManager refreshTokens) {
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
    }

    public AuthResult newSession(UserAccount user) {
        RefreshTokenManager.Issued refresh = refreshTokens.issue(user);
        return new AuthResult(accessTokens.issue(user), accessTokens.ttlSeconds(),
                refresh.rawToken(), refresh.expiresAt(), UserMapper.toResponse(user));
    }

    public AuthResult rotate(String rawRefreshToken) {
        RefreshTokenManager.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        UserAccount user = rotation.user();
        return new AuthResult(accessTokens.issue(user), accessTokens.ttlSeconds(),
                rotation.next().rawToken(), rotation.next().expiresAt(), UserMapper.toResponse(user));
    }
}
