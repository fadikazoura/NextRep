package com.fadikazoura.nextrep.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Limits how often one address may call the endpoints that are worth abusing: guessing passwords, mass sign-ups,
 * creating demo accounts, sending email, and anything that spends the AI quota.
 * Runs before Spring Security, so rejected requests cost almost nothing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitFilter extends OncePerRequestFilter {

    /** One rule per endpoint; the limits are per client IP address. */
    record Rule(String name, String method, String path, int limit, Duration period) {
    }

    static final List<Rule> RULES = List.of(
            new Rule("login", "POST", "/api/auth/login", 10, Duration.ofMinutes(1)),
            new Rule("register", "POST", "/api/auth/register", 5, Duration.ofHours(1)),
            new Rule("demo", "POST", "/api/auth/demo", 5, Duration.ofHours(1)),
            new Rule("reset", "POST", "/api/auth/password-reset", 5, Duration.ofHours(1)),
            new Rule("reset-confirm", "POST", "/api/auth/password-reset/confirm", 20, Duration.ofHours(1)),
            new Rule("contact", "POST", "/api/users/me/contact", 10, Duration.ofHours(1)),
            new Rule("plan", "POST", "/api/users/me/plan/generate", 20, Duration.ofHours(1)),
            new Rule("coach", "POST", "/api/users/me/coach", 40, Duration.ofHours(1)));

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || rule(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = rule(request).orElseThrow();
        Optional<Duration> wait = rateLimiter.tryAcquire(rule.name() + ":" + clientIp(request), rule.limit(), rule.period());
        if (wait.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        long seconds = Math.max(1, wait.get().toSeconds());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(seconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too many requests","status":429,\
                "detail":"Too many attempts. Please try again in %s.","retryAfterSeconds":%d}"""
                .formatted(humanize(seconds), seconds));
    }

    private static Optional<Rule> rule(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return RULES.stream()
                .filter(rule -> rule.method().equalsIgnoreCase(request.getMethod()) && rule.path().equals(path))
                .findFirst();
    }

    String clientIp(HttpServletRequest request) {
        String header = properties.clientIpHeader();
        if (header != null && !header.isBlank()) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return request.getRemoteAddr();
    }

    private static String humanize(long seconds) {
        if (seconds < 90) return seconds + " seconds";
        long minutes = (seconds + 59) / 60;
        return minutes < 90 ? minutes + " minutes" : ((minutes + 59) / 60) + " hours";
    }
}
