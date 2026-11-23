package com.fadikazoura.nextrep.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts attempts per key ("login:203.0.113.9", "coach:42") in fixed time windows, in memory.
 * <p>
 * In memory is enough for one server instance (the free hosting runs exactly one); with several instances the
 * counters would move to a shared store such as Redis, behind the same {@link #tryAcquire} method.
 * A restart resets the counters, which is acceptable for abuse protection.
 */
@Component
public class RateLimiter {

    /** Count of attempts in the window that ends at {@code resetAt}. */
    private record Window(Instant resetAt, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Records one attempt. Returns empty when it is allowed, or how long to wait when the limit is reached.
     * Thread-safe: the read-check-increment happens atomically inside {@code compute}.
     */
    public Optional<Duration> tryAcquire(String key, int limit, Duration period) {
        Instant now = clock.instant();
        boolean[] allowed = {false};
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || !now.isBefore(current.resetAt())) {
                current = new Window(now.plus(period), 0);
            }
            if (current.count() < limit) {
                allowed[0] = true;
                return new Window(current.resetAt(), current.count() + 1);
            }
            return current;
        });
        return allowed[0] ? Optional.empty() : Optional.of(Duration.between(now, window.resetAt()));
    }

    /** Forgets finished windows so the map doesn't grow forever. */
    @Scheduled(fixedDelayString = "PT10M")
    public void removeExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(window -> !now.isBefore(window.resetAt()));
    }

    int size() {
        return windows.size();
    }
}
