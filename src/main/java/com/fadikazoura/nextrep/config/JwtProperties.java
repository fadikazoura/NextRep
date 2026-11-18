package com.fadikazoura.nextrep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("nextrep.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
