package com.fadikazoura.nextrep.progression;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgressionController {

    private final ProgressionService progressionService;

    @GetMapping("/api/users/me/plan/days/{planDayId}/recommendations")
    public PlanDayRecommendationsResponse recommendations(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long planDayId) {
        return progressionService.recommendationsFor(Long.valueOf(jwt.getSubject()), planDayId);
    }
}
