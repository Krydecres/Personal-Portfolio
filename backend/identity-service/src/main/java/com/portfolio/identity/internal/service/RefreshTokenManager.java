package com.portfolio.identity.internal.service;

import com.portfolio.common.error.ServiceException;
import com.portfolio.identity.internal.config.IdentityProperties;
import com.portfolio.identity.internal.domain.RefreshToken;
import com.portfolio.identity.internal.domain.RefreshToken.RevokeReason;
import com.portfolio.identity.internal.domain.RefreshTokenRepository;
import com.portfolio.identity.internal.domain.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Opaque, rotating refresh tokens.
 * <ul>
 *   <li>256 bits from {@link SecureRandom}; only a SHA-256 hash is persisted.</li>
 *   <li>Every refresh revokes the presented token and issues a new one in the same family.</li>
 *   <li>Presenting a token that was already rotated (outside a short grace window) means it was stolen
 *       and replayed: the whole family is revoked, logging out both the thief and the victim.</li>
 * </ul>
 */
@Component
class RefreshTokenManager {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenManager.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    record Issued(String rawToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "Issued[***]";
        }
    }

    private final RefreshTokenRepository repository;
    private final IdentityProperties props;
    private final Clock clock;

    RefreshTokenManager(RefreshTokenRepository repository, IdentityProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    /** Starts a new sign-in session that ends {@code app.security.refresh.ttl} (default 1 day) from now. */
    @Transactional
    public Issued issue(UserAccount user) {
        return issue(user, UUID.randomUUID().toString(), clock.instant().plus(props.getRefresh().getTtl()));
    }

    private Issued issue(UserAccount user, String familyId, Instant expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(user, hash(raw), familyId, expiresAt));
        return new Issued(raw, expiresAt);
    }

    /** @return the user owning the token plus a freshly rotated token. */
    @Transactional(noRollbackFor = ServiceException.class)
    public Rotation rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw invalid();
        }
        Instant now = clock.instant();
        RefreshToken token = repository.findByHashForUpdate(hash(rawToken)).orElseThrow(this::invalid);

        if (token.getRevokedAt() != null) {
            boolean benignRace = token.getRevokeReason() == RevokeReason.ROTATED
                    && token.getRevokedAt().plus(props.getRefresh().getReuseGrace()).isAfter(now);
            if (!benignRace) {
                int revoked = repository.revokeFamily(token.getFamilyId(), RevokeReason.REUSE_DETECTED, now);
                log.warn("Refresh token reuse detected for user {} (family {}): revoked {} token(s)",
                        token.getUser().getId(), token.getFamilyId(), revoked);
            }
            throw invalid();
        }
        if (!token.isActive(now)) {
            throw invalid();
        }
        UserAccount user = token.getUser();
        if (!user.isEnabled()) {
            repository.revokeFamily(token.getFamilyId(), RevokeReason.LOGOUT, now);
            throw invalid();
        }
        token.revoke(RevokeReason.ROTATED, now);
        // Absolute session lifetime: the rotated token inherits the family's original expiry, so refreshing
        // never extends a session past "1 day after sign-in". After that, the user must sign in again.
        return new Rotation(user, issue(user, token.getFamilyId(), token.getExpiresAt()));
    }

    record Rotation(UserAccount user, Issued next) {}

    @Transactional
    public void revokeFamilyOf(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            return;
        }
        repository.findByHashForUpdate(hash(rawToken)).ifPresent(t ->
                repository.revokeFamily(t.getFamilyId(), RevokeReason.LOGOUT, clock.instant()));
    }

    @Transactional
    public void revokeAll(Long userId, RevokeReason reason) {
        repository.revokeAllForUser(userId, reason, clock.instant());
    }

    /** Housekeeping: expired tokens are useless, keep the table small. Runs nightly at 03:15. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpiredBefore(clock.instant().minus(1, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Purged {} expired refresh token(s)", deleted);
        }
    }

    private ServiceException invalid() {
        return ServiceException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
