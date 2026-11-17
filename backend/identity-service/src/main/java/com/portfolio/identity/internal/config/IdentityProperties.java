package com.portfolio.identity.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** All security knobs of the identity service, bound from {@code app.security.*}. */
@ConfigurationProperties(prefix = "app.security")
public class IdentityProperties {

    /** URL of the React app; used for redirects after Google sign-in. */
    private String frontendUrl = "http://localhost:5173";

    /** Origins allowed by CORS and by the Origin check on cookie-authenticated endpoints. */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    /** Emails that become ADMIN (portfolio owner) when they register or first sign in with Google. */
    private List<String> adminEmails = new ArrayList<>();

    /** Demo convenience: the very first account becomes ADMIN. Turn off in production. */
    private boolean firstUserAdmin = true;

    private final Jwt jwt = new Jwt();
    private final Refresh refresh = new Refresh();
    private final Lockout lockout = new Lockout();
    private final Google google = new Google();

    public static class Jwt {
        /** HMAC secret, at least 32 bytes. When empty a random key is generated at startup. */
        private String secret = "";
        private String issuer = "portfolio-api";
        private Duration accessTokenTtl = Duration.ofMinutes(15);

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Duration getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
    }

    public static class Refresh {
        private Duration ttl = Duration.ofDays(1);
        private String cookieName = "pf_refresh";
        private String cookiePath = "/api/auth";
        private boolean cookieSecure = true;
        /** Window in which a just-rotated token is treated as a benign race between tabs, not as theft. */
        private Duration reuseGrace = Duration.ofSeconds(10);

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public String getCookieName() { return cookieName; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
        public String getCookiePath() { return cookiePath; }
        public void setCookiePath(String cookiePath) { this.cookiePath = cookiePath; }
        public boolean isCookieSecure() { return cookieSecure; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
        public Duration getReuseGrace() { return reuseGrace; }
        public void setReuseGrace(Duration reuseGrace) { this.reuseGrace = reuseGrace; }
    }

    public static class Lockout {
        private int maxAttempts = 5;
        private Duration duration = Duration.ofMinutes(15);

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public Duration getDuration() { return duration; }
        public void setDuration(Duration duration) { this.duration = duration; }
    }

    public static class Google {
        private String clientId = "";
        private String clientSecret = "";

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    public List<String> getAdminEmails() { return adminEmails; }
    public void setAdminEmails(List<String> adminEmails) { this.adminEmails = adminEmails; }
    public boolean isFirstUserAdmin() { return firstUserAdmin; }
    public void setFirstUserAdmin(boolean firstUserAdmin) { this.firstUserAdmin = firstUserAdmin; }
    public Jwt getJwt() { return jwt; }
    public Refresh getRefresh() { return refresh; }
    public Lockout getLockout() { return lockout; }
    public Google getGoogle() { return google; }
}
