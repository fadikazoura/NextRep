package com.fadikazoura.nextrep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** JWT settings, checked at startup so a weak or missing secret stops the app instead of weakening security. */
@ConfigurationProperties("nextrep.jwt")
public record JwtProperties(String secret, Duration ttl) {

    /** HS256 needs a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "nextrep.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("nextrep.jwt.ttl must be a positive duration");
        }
    }
}
