package com.fadikazoura.nextrep.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

    @Test
    void acceptsA256BitSecret() {
        assertThatCode(() -> new JwtProperties(STRONG_SECRET, Duration.ofHours(1))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAMissingOrShortSecret() {
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsANonPositiveTokenLifetime() {
        assertThatThrownBy(() -> new JwtProperties(STRONG_SECRET, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ttl");
    }
}
