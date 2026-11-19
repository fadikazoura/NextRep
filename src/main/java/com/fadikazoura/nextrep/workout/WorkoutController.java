package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse start(@AuthenticationPrincipal Jwt jwt,
                                 @RequestBody(required = false) StartWorkoutRequest request) {
        return workoutService.start(currentUserId(jwt), request);
    }

    @GetMapping
    public PageResponse<WorkoutSummaryResponse> history(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return workoutService.history(currentUserId(jwt), page, size);
    }

    @GetMapping("/current")
    public WorkoutResponse current(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.getCurrent(currentUserId(jwt));
    }

    @GetMapping("/{workoutId}")
    public WorkoutResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long workoutId) {
        return workoutService.get(currentUserId(jwt), workoutId);
    }

    @PostMapping("/{workoutId}/sets")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutSetResponse addSet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long workoutId,
                                     @Valid @RequestBody AddSetRequest request) {
        return workoutService.addSet(currentUserId(jwt), workoutId, request);
    }

    @DeleteMapping("/{workoutId}/sets/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long workoutId, @PathVariable Long setId) {
        workoutService.removeSet(currentUserId(jwt), workoutId, setId);
    }

    @PostMapping("/{workoutId}/complete")
    public WorkoutResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long workoutId) {
        return workoutService.complete(currentUserId(jwt), workoutId);
    }

    @DeleteMapping("/{workoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@AuthenticationPrincipal Jwt jwt, @PathVariable Long workoutId) {
        workoutService.discard(currentUserId(jwt), workoutId);
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
