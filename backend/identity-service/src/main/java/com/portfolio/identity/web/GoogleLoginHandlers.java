package com.portfolio.identity.web;

import com.portfolio.common.error.ServiceException;
import com.portfolio.identity.api.AuthService;
import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.api.dto.IdentityDtos.GoogleProfile;
import com.portfolio.identity.internal.config.IdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After Google returns to /login/oauth2/code/google and Spring Security has verified the ID token
 * (signature, issuer, audience, expiry, nonce) and the "state" parameter, we:
 * <ol>
 *   <li>find or create the local account,</li>
 *   <li>put a refresh token in the HttpOnly cookie,</li>
 *   <li>throw away the temporary server session used for the OAuth handshake,</li>
 *   <li>redirect to the React app, which calls /api/auth/refresh to get its access token.</li>
 * </ol>
 * No token ever appears in a URL, so nothing leaks through history, logs or the Referer header.
 */
@Component
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginHandlers.class);

    private final AuthService authService;
    private final RefreshCookies cookies;
    private final IdentityProperties props;

    public GoogleLoginHandlers(AuthService authService, RefreshCookies cookies, IdentityProperties props) {
        this.authService = authService;
        this.cookies = cookies;
        this.props = props;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidc)) {
                throw ServiceException.unauthorized("OAUTH_FAILED", "Không đọc được thông tin từ Google");
            }
            GoogleProfile profile = new GoogleProfile(
                    oidc.getSubject(),
                    oidc.getEmail(),
                    Boolean.TRUE.equals(oidc.getEmailVerified()),
                    oidc.getFullName(),
                    oidc.getPicture());
            AuthResult result = authService.loginWithGoogle(profile);
            cookies.write(response, result);
            endHandshakeSession(request);
            response.sendRedirect(frontend("/auth/callback", null));
        } catch (ServiceException e) {
            endHandshakeSession(request);
            response.sendRedirect(frontend("/login", e.getCode()));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google sign-in failed: {}", exception.getMessage());
        endHandshakeSession(request);
        response.sendRedirect(frontend("/login", "OAUTH_FAILED"));
    }

    private void endHandshakeSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Redirect target is built from configuration only, never from request input (no open redirect). */
    private String frontend(String path, String errorCode) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(props.getFrontendUrl()).path(path);
        if (errorCode != null) {
            b.queryParam("error", errorCode);
        }
        return b.build().encode().toUriString();
    }
}
