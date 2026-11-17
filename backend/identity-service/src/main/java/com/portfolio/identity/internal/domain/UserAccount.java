package com.portfolio.identity.internal.domain;

import com.portfolio.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "uk_users_email", columnList = "email", unique = true),
        @Index(name = "uk_users_google_subject", columnList = "google_subject", unique = true)
})
public class UserAccount extends BaseEntity {

    public enum Role { USER, ADMIN }

    public enum Provider { LOCAL, GOOGLE }

    @Column(nullable = false, length = 190)
    private String email;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    /** BCrypt hash; null for accounts that only sign in with Google. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider = Provider.LOCAL;

    /** Stable Google account id ("sub" claim). Email can change on Google's side, "sub" never does. */
    @Column(name = "google_subject", length = 64)
    private String googleSubject;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccount() {}

    public UserAccount(String email, String displayName, Provider provider) {
        this.email = email;
        this.displayName = displayName;
        this.provider = provider;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(int maxAttempts, Instant lockUntil) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = lockUntil;
            failedLoginAttempts = 0;
        }
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Provider getProvider() { return provider; }
    public String getGoogleSubject() { return googleSubject; }
    public void setGoogleSubject(String googleSubject) { this.googleSubject = googleSubject; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
