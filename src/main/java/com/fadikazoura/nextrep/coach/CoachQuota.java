package com.fadikazoura.nextrep.coach;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Questions per user per day (UTC). The free AI quota is shared by everyone, so one user can't use it all up.
 * A failed AI call gives the question back. In memory, like the rate limiter: one instance, resets on restart.
 */
@Component
class CoachQuota {

    static final int DAILY_QUESTIONS = 20;

    private record Usage(LocalDate day, int count) {
    }

    private final ConcurrentHashMap<Long, Usage> usage = new ConcurrentHashMap<>();
    private final Clock clock;

    CoachQuota(Clock clock) {
        this.clock = clock;
    }

    /** Takes one question; returns how many are left today, or -1 if none were left. */
    int take(Long userId) {
        LocalDate today = LocalDate.now(clock);
        int[] left = {-1};
        usage.compute(userId, (id, current) -> {
            Usage fresh = current == null || !current.day().equals(today) ? new Usage(today, 0) : current;
            if (fresh.count() >= DAILY_QUESTIONS) return fresh;
            left[0] = DAILY_QUESTIONS - fresh.count() - 1;
            return new Usage(today, fresh.count() + 1);
        });
        // Old days are dropped as users come back; entries are tiny, so no separate cleanup is needed.
        return left[0];
    }

    void giveBack(Long userId) {
        usage.computeIfPresent(userId, (id, current) -> new Usage(current.day(), Math.max(0, current.count() - 1)));
    }
}
