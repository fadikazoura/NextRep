package com.fadikazoura.nextrep.insights;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RestController
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    /**
     * Streak, this week / last week / the week before, and achievements.
     *
     * @param tz       IANA time zone of the browser, e.g. Asia/Jerusalem (unknown values fall back to UTC)
     * @param firstDay first day of the week
     */
    @GetMapping("/api/users/me/insights")
    public InsightsResponse insights(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) String tz,
                                     @RequestParam(defaultValue = "MONDAY") DayOfWeek firstDay) {
        return insightsService.insights(Long.valueOf(jwt.getSubject()), zone(tz), firstDay);
    }

    private static ZoneId zone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
