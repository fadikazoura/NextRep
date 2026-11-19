package com.fadikazoura.nextrep.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/rating")
    public RatingResponse rating(@AuthenticationPrincipal Jwt jwt) {
        return feedbackService.getRating(userId(jwt));
    }

    @PutMapping("/rating")
    public RatingResponse rate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RatingRequest request) {
        return feedbackService.rate(userId(jwt), request);
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public void contact(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ContactRequest request) {
        feedbackService.contact(userId(jwt), request);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
