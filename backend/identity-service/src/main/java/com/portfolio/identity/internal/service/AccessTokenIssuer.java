package com.portfolio.identity.internal.service;

import com.portfolio.identity.internal.config.IdentityProperties;
import com.portfolio.identity.internal.domain.UserAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Issues short-lived, signed access tokens (JWT, HS256). */
@Component
class AccessTokenIssuer {

    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final IdentityProperties props;
    private final Clock clock;

    AccessTokenIssuer(JwtEncoder encoder, IdentityProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    String issue(UserAccount user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getJwt().getIssuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(props.getJwt().getAccessTokenTtl()))
                .claim(ROLE_CLAIM, user.getRole().name())
                // deliberately minimal: no email or other personal data inside a token the browser can read
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    long ttlSeconds() {
        return props.getJwt().getAccessTokenTtl().toSeconds();
    }
}
