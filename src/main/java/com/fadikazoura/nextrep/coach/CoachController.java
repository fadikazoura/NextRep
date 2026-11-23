package com.fadikazoura.nextrep.coach;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RestController
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    /** 200 with the answer; 429 when today's questions are used up; 503 when the AI is unavailable. */
    @PostMapping("/api/users/me/coach")
    public CoachResponse ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CoachRequest request,
                             @RequestParam(required = false) String tz) {
        return coachService.ask(Long.valueOf(jwt.getSubject()), request, zone(tz));
    }

    private static ZoneId zone(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneOffset.UTC : ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
