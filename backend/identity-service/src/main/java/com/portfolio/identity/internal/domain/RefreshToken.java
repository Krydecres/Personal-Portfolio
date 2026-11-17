package com.portfolio.identity.internal.domain;

import com.portfolio.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A refresh token. Only the SHA-256 hash is stored, so a database leak does not leak usable tokens.
 * Tokens of one sign-in share a {@code familyId}; presenting an already rotated token revokes the family
 * (refresh-token reuse detection).
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "uk_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "ix_refresh_family", columnList = "family_id"),
        @Index(name = "ix_refresh_user", columnList = "user_id")
})
public class RefreshToken extends BaseEntity {

    public enum RevokeReason { ROTATED, LOGOUT, REUSE_DETECTED, PASSWORD_CHANGED, ACCOUNT_LINKED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 24)
    private RevokeReason revokeReason;

    protected RefreshToken() {}

    public RefreshToken(UserAccount user, String tokenHash, String familyId, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(RevokeReason reason, Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
            revokeReason = reason;
        }
    }

    public UserAccount getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public String getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public RevokeReason getRevokeReason() { return revokeReason; }
}
