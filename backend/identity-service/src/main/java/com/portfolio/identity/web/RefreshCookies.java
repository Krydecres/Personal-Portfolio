package com.portfolio.identity.web;

import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.internal.config.IdentityProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Writes the refresh token as a cookie that JavaScript cannot read (HttpOnly), that is only sent over
 * HTTPS (Secure), never on cross-site requests (SameSite=Strict) and only to /api/auth (Path).
 */
@Component
public class RefreshCookies {

    private final IdentityProperties props;

    public RefreshCookies(IdentityProperties props) {
        this.props = props;
    }

    public String name() {
        return props.getRefresh().getCookieName();
    }

    public void write(HttpServletResponse response, AuthResult result) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), result.refreshTokenExpiresAt()).toSeconds());
        response.addHeader(HttpHeaders.SET_COOKIE, build(result.refreshToken(), Duration.ofSeconds(maxAge)).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(props.getRefresh().getCookieName(), value)
                .httpOnly(true)
                .secure(props.getRefresh().isCookieSecure())
                .sameSite("Strict")
                .path(props.getRefresh().getCookiePath())
                .maxAge(maxAge)
                .build();
    }
}
