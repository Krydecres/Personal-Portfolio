package com.portfolio.identity.web;

import com.portfolio.common.security.Caller;
import com.portfolio.identity.api.AccountService;
import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.api.dto.IdentityDtos.ChangePasswordRequest;
import com.portfolio.identity.api.dto.IdentityDtos.TokenResponse;
import com.portfolio.identity.api.dto.IdentityDtos.UpdateAccountRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final AccountService accountService;
    private final RefreshCookies cookies;

    public AccountController(AccountService accountService, RefreshCookies cookies) {
        this.accountService = accountService;
        this.cookies = cookies;
    }

    @GetMapping("/me")
    public UserResponse me(Caller caller) {
        return accountService.me(caller);
    }

    @PutMapping("/me")
    public UserResponse update(Caller caller, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(caller, request);
    }

    @PostMapping("/password")
    public ResponseEntity<TokenResponse> changePassword(Caller caller,
                                                        @Valid @RequestBody ChangePasswordRequest request,
                                                        HttpServletResponse response) {
        AuthResult result = accountService.changePassword(caller, request);
        cookies.write(response, result);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(result.accessToken(), "Bearer", result.accessTokenExpiresIn(), result.user()));
    }
}
