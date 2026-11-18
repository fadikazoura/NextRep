package com.fadikazoura.nextrep.bodyweight;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/body-weight")
@RequiredArgsConstructor
public class BodyWeightController {

    private final BodyWeightService bodyWeightService;

    @PutMapping
    public BodyWeightResponse logWeight(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BodyWeightRequest request) {
        return bodyWeightService.logWeight(currentUserId(jwt), request);
    }

    @GetMapping
    public List<BodyWeightResponse> history(@AuthenticationPrincipal Jwt jwt) {
        return bodyWeightService.getHistory(currentUserId(jwt));
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
