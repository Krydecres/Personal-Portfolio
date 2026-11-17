package com.portfolio.identity.web;

import com.portfolio.identity.api.AuthService;
import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.api.dto.IdentityDtos.AuthProvidersResponse;
import com.portfolio.identity.api.dto.IdentityDtos.LoginRequest;
import com.portfolio.identity.api.dto.IdentityDtos.PasswordRuleResponse;
import com.portfolio.identity.api.dto.IdentityDtos.RegisterRequest;
import com.portfolio.identity.api.dto.IdentityDtos.TokenResponse;
import com.portfolio.identity.api.validation.PasswordPolicy;
import com.portfolio.identity.internal.config.IdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

/** Public endpoint of the identity service. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookies cookies;
    private final OriginGuard originGuard;
    private final IdentityProperties props;

    public AuthController(AuthService authService, RefreshCookies cookies, OriginGuard originGuard,
                          IdentityProperties props) {
        this.authService = authService;
        this.cookies = cookies;
        this.originGuard = originGuard;
        this.props = props;
    }

    @GetMapping("/providers")
    public AuthProvidersResponse providers() {
        return new AuthProvidersResponse(
                props.getGoogle().isConfigured(),
                PasswordPolicy.RULES.stream().map(r -> new PasswordRuleResponse(r.id(), r.label())).toList());
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResult result = authService.register(request);
        return respond(HttpStatus.CREATED, result, response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        return respond(HttpStatus.OK, authService.login(request), response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        originGuard.check(request);
        try {
            return respond(HttpStatus.OK, authService.refresh(readCookie(request)), response);
        } catch (RuntimeException e) {
            cookies.clear(response);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        originGuard.check(request);
        authService.logout(readCookie(request));
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }

    private String readCookie(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, cookies.name());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseEntity<TokenResponse> respond(HttpStatus status, AuthResult result, HttpServletResponse response) {
        cookies.write(response, result);
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore()) // tokens must never be cached by browsers or proxies
                .body(new TokenResponse(result.accessToken(), "Bearer", result.accessTokenExpiresIn(), result.user()));
    }
}
