package com.fadikazoura.nextrep.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/records")
    public List<PersonalRecordResponse> records(@AuthenticationPrincipal Jwt jwt) {
        return statsService.personalRecords(currentUserId(jwt));
    }

    @GetMapping("/exercises/{exerciseId}/progress")
    public ExerciseProgressResponse progress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long exerciseId) {
        return statsService.progress(currentUserId(jwt), exerciseId);
    }

    /** Sets per muscle group in the last {@code days} days (default 7), for the muscle heat map. */
    @GetMapping("/muscles/volume")
    public List<MuscleVolumeResponse> muscleVolume(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "7") int days) {
        return statsService.muscleVolume(currentUserId(jwt), days);
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
