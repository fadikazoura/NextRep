package com.fadikazoura.nextrep.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled        turned off in the automated tests, which send many requests from one address
 * @param clientIpHeader header that holds the visitor's real IP when the app runs behind a proxy. On Render it is
 *                       True-Client-IP (set by Cloudflare, cannot be faked); X-Forwarded-For is NOT used because
 *                       a client can put anything in it. Without the header, the connection's address is used.
 */
@ConfigurationProperties("nextrep.rate-limit")
public record RateLimitProperties(boolean enabled, String clientIpHeader) {
}
