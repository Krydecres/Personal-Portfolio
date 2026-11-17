package com.portfolio.identity.internal.service;

import com.portfolio.common.error.ServiceException;
import com.portfolio.common.util.Slugs;
import com.portfolio.identity.api.AuthService;
import com.portfolio.identity.api.dto.IdentityDtos.GoogleProfile;
import com.portfolio.identity.api.dto.IdentityDtos.LoginRequest;
import com.portfolio.identity.api.dto.IdentityDtos.RegisterRequest;
import com.portfolio.identity.internal.config.IdentityProperties;
import com.portfolio.identity.internal.domain.RefreshToken.RevokeReason;
import com.portfolio.identity.internal.domain.UserAccount;
import com.portfolio.identity.internal.domain.UserAccount.Provider;
import com.portfolio.identity.internal.domain.UserAccount.Role;
import com.portfolio.identity.internal.domain.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionIssuer sessions;
    private final RefreshTokenManager refreshTokens;
    private final IdentityProperties props;
    private final Clock clock;
    /** Hash of a random string, compared against when the account does not exist (equal response time). */
    private final String dummyHash;

    AuthServiceImpl(UserAccountRepository users, PasswordEncoder passwordEncoder, SessionIssuer sessions,
                    RefreshTokenManager refreshTokens, IdentityProperties props, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.props = props;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-" + System.nanoTime());
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw ServiceException.badRequest("PASSWORD_MISMATCH", "Mật khẩu nhập lại không khớp");
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ServiceException.conflict("EMAIL_TAKEN", "Email này đã được đăng ký");
        }
        UserAccount user = new UserAccount(email, request.displayName().trim(), Provider.LOCAL);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(decideRole(email));
        user.registerSuccessfulLogin(clock.instant());
        users.save(user);
        log.info("New local account {} registered with role {}", user.getId(), user.getRole());
        return issueSession(user);
    }

    @Override
    @Transactional(noRollbackFor = ServiceException.class) // failed-attempt counters must be committed
    public AuthResult login(LoginRequest request) {
        Instant now = clock.instant();
        UserAccount user = users.findByEmailIgnoreCase(normalizeEmail(request.email())).orElse(null);

        if (user == null || user.getPasswordHash() == null) {
            passwordEncoder.matches(request.password(), dummyHash); // same cost as a real check
            throw badCredentials();
        }
        if (user.isLocked(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw ServiceException.tooManyRequests(
                    "Tài khoản tạm khoá do đăng nhập sai nhiều lần. Thử lại sau khoảng " + minutes + " phút.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(props.getLockout().getMaxAttempts(), now.plus(props.getLockout().getDuration()));
            log.warn("Failed password login for account {}", user.getId());
            throw badCredentials();
        }
        if (!user.isEnabled()) {
            throw ServiceException.forbidden("Tài khoản đã bị vô hiệu hoá");
        }
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.registerSuccessfulLogin(now);
        return issueSession(user);
    }

    @Override
    @Transactional
    public AuthResult loginWithGoogle(GoogleProfile profile) {
        if (!profile.emailVerified() || profile.email() == null) {
            throw ServiceException.unauthorized("GOOGLE_EMAIL_UNVERIFIED", "Email Google chưa được xác minh");
        }
        String email = normalizeEmail(profile.email());
        UserAccount user = users.findByGoogleSubject(profile.subject())
                .or(() -> users.findByEmailIgnoreCase(email).map(existing -> linkGoogle(existing, profile)))
                .orElseGet(() -> createGoogleAccount(email, profile));

        if (!user.isEnabled()) {
            throw ServiceException.forbidden("Tài khoản đã bị vô hiệu hoá");
        }
        if (user.getAvatarUrl() == null && isHttps(profile.pictureUrl())) {
            user.setAvatarUrl(profile.pictureUrl());
        }
        user.registerSuccessfulLogin(clock.instant());
        return issueSession(user);
    }

    private UserAccount linkGoogle(UserAccount existing, GoogleProfile profile) {
        if (!existing.isEmailVerified()) {
            // Pre-account-takeover defence: someone may have registered this email before its real owner.
            // Google proved ownership now, so drop the unverified password and every session created with it.
            existing.setPasswordHash(null);
            refreshTokens.revokeAll(existing.getId(), RevokeReason.ACCOUNT_LINKED);
            log.warn("Unverified local account {} linked to Google; its password was cleared", existing.getId());
        }
        existing.setGoogleSubject(profile.subject());
        existing.setEmailVerified(true);
        return existing;
    }

    private UserAccount createGoogleAccount(String email, GoogleProfile profile) {
        String name = Slugs.clean(profile.name());
        UserAccount user = new UserAccount(email, name == null ? email.substring(0, email.indexOf('@')) : truncate(name, 80),
                Provider.GOOGLE);
        user.setGoogleSubject(profile.subject());
        user.setEmailVerified(true);
        user.setRole(decideRole(email));
        users.save(user);
        log.info("New Google account {} created with role {}", user.getId(), user.getRole());
        return user;
    }

    @Override
    @Transactional(noRollbackFor = ServiceException.class) // reuse-detection revocations must be committed
    public AuthResult refresh(String rawRefreshToken) {
        return sessions.rotate(rawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revokeFamilyOf(rawRefreshToken);
    }

    private AuthResult issueSession(UserAccount user) {
        return sessions.newSession(user);
    }

    private Role decideRole(String email) {
        boolean listed = props.getAdminEmails().stream().anyMatch(e -> e.trim().equalsIgnoreCase(email));
        if (listed) {
            return Role.ADMIN;
        }
        return props.isFirstUserAdmin() && users.count() == 0 ? Role.ADMIN : Role.USER;
    }

    private static ServiceException badCredentials() {
        // Same message whether the email exists or not: no account enumeration through login.
        return ServiceException.unauthorized("BAD_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isHttps(String url) {
        return url != null && url.startsWith("https://") && url.length() <= 1024;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
