package com.portfolio.identity.internal.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

    private static final Logger log = LoggerFactory.getLogger(IdentityConfig.class);
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    PasswordEncoder passwordEncoder() {
        // Cost 12 ~ 250ms per hash on a laptop: slow enough to hurt offline cracking, fast enough for logins.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey jwtSigningKey(IdentityProperties props) {
        String secret = props.getJwt().getSecret();
        byte[] keyBytes;
        if (secret == null || secret.isBlank()) {
            keyBytes = new byte[64];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET is not set: using a random signing key. Access tokens become invalid on restart "
                    + "(users are silently re-authenticated through their refresh cookie). Set JWT_SECRET in production.");
        } else {
            keyBytes = decodeSecret(secret);
            if (keyBytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits)");
            }
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private static byte[] decodeSecret(String secret) {
        if (secret.startsWith("base64:")) {
            return Base64.getDecoder().decode(secret.substring("base64:".length()));
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, IdentityProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256) // pin the algorithm: no "alg: none" or algorithm confusion
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.getJwt().getIssuer()));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /** Registers Google only when GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET are provided. */
    @Bean
    @Conditional(GoogleConfigured.class)
    ClientRegistrationRepository clientRegistrationRepository(IdentityProperties props) {
        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(props.getGoogle().getClientId())
                        .clientSecret(props.getGoogle().getClientSecret())
                        .scope("openid", "profile", "email")
                        .build());
    }

    static class GoogleConfigured implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String id = context.getEnvironment().getProperty("app.security.google.client-id", "");
            String secret = context.getEnvironment().getProperty("app.security.google.client-secret", "");
            return !id.isBlank() && !secret.isBlank();
        }
    }
}
