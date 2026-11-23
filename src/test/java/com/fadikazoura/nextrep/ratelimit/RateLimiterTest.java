package com.fadikazoura.nextrep.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    /** A clock the test can move forward. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsUpToTheLimitThenSaysHowLongToWait() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("login:1.2.3.4", 3, Duration.ofMinutes(1))).isEmpty();
        }
        clock.now = clock.now.plusSeconds(20);

        assertThat(limiter.tryAcquire("login:1.2.3.4", 3, Duration.ofMinutes(1))).contains(Duration.ofSeconds(40));
    }

    @Test
    void aNewWindowStartsAfterThePeriod() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("k", 3, Duration.ofMinutes(1));
        clock.now = clock.now.plus(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isEmpty();
    }

    @Test
    void keysAreCountedSeparately() {
        limiter.tryAcquire("login:1.1.1.1", 1, Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("login:2.2.2.2", 1, Duration.ofMinutes(1))).isEmpty();
        assertThat(limiter.tryAcquire("login:1.1.1.1", 1, Duration.ofMinutes(1))).isPresent();
    }

    @Test
    void finishedWindowsAreForgotten() {
        limiter.tryAcquire("a", 1, Duration.ofMinutes(1));
        limiter.tryAcquire("b", 1, Duration.ofHours(1));
        clock.now = clock.now.plus(Duration.ofMinutes(2));

        limiter.removeExpired();

        assertThat(limiter.size()).isEqualTo(1);
    }
}
