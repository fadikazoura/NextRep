package com.fadikazoura.nextrep.plan;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/plan")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    @GetMapping
    public TrainingPlanResponse activePlan(@AuthenticationPrincipal Jwt jwt) {
        return trainingPlanService.getActivePlan(currentUserId(jwt));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingPlanResponse generate(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(defaultValue = "false") boolean ai) {
        return trainingPlanService.generatePlan(currentUserId(jwt), ai);
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
