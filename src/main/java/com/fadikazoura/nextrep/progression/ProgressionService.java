package com.fadikazoura.nextrep.progression;

import com.fadikazoura.nextrep.exercise.Exercise;
import com.fadikazoura.nextrep.plan.PlanDay;
import com.fadikazoura.nextrep.plan.PlanDayNotFoundException;
import com.fadikazoura.nextrep.plan.PlanDayRepository;
import com.fadikazoura.nextrep.plan.PlanExercise;
import com.fadikazoura.nextrep.workout.WorkoutSet;
import com.fadikazoura.nextrep.workout.WorkoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressionService {

    /** Older sessions say little about today's strength. */
    static final Duration LOOKBACK = Duration.ofDays(120);
    /** The engine needs at most a few recent sessions per exercise (deload looks at 3). */
    static final int MAX_SESSIONS_PER_EXERCISE = 5;

    private final PlanDayRepository planDayRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final ProgressionEngine progressionEngine;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PlanDayRecommendationsResponse recommendationsFor(Long userId, Long planDayId) {
        PlanDay day = planDayRepository.findByIdAndPlanUserId(planDayId, userId)
                .orElseThrow(() -> new PlanDayNotFoundException(planDayId));

        List<Long> exerciseIds = day.getExercises().stream()
                .map(planExercise -> planExercise.getExercise().getId())
                .toList();
        Map<Long, List<SessionPerformance>> history = recentHistory(userId, exerciseIds);

        List<ExerciseRecommendationResponse> recommendations = day.getExercises().stream()
                .map(planExercise -> recommend(planExercise, history))
                .toList();
        return new PlanDayRecommendationsResponse(day.getId(), day.getDayOfWeek(), day.getName(), recommendations);
    }

    private ExerciseRecommendationResponse recommend(PlanExercise planExercise,
                                                     Map<Long, List<SessionPerformance>> history) {
        Exercise exercise = planExercise.getExercise();
        ExerciseTarget target = new ExerciseTarget(exercise.getId(), exercise.getMovementType(),
                exercise.getEquipment(), planExercise.getSets(), planExercise.getRepMin(), planExercise.getRepMax());
        Recommendation recommendation = progressionEngine.recommend(target,
                history.getOrDefault(exercise.getId(), List.of()));
        return ExerciseRecommendationResponse.from(exercise.getName(), recommendation);
    }

    /** One query for all exercises of the day, grouped into exercise → sessions (newest first) → sets. */
    private Map<Long, List<SessionPerformance>> recentHistory(Long userId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Map.of();
        }
        Instant since = clock.instant().minus(LOOKBACK);
        List<WorkoutSet> sets = workoutSetRepository.findCompletedSets(userId, exerciseIds, since);

        Map<Long, Map<Long, List<SetPerformance>>> setsByExerciseAndSession = new HashMap<>();
        Map<Long, Instant> sessionDates = new HashMap<>();
        for (WorkoutSet set : sets) {
            Long sessionId = set.getSession().getId();
            sessionDates.put(sessionId, set.getSession().getStartedAt());
            setsByExerciseAndSession
                    .computeIfAbsent(set.getExercise().getId(), id -> new LinkedHashMap<>())
                    .computeIfAbsent(sessionId, id -> new ArrayList<>())
                    .add(new SetPerformance(set.getWeightKg(), set.getReps()));
        }

        Map<Long, List<SessionPerformance>> history = new HashMap<>();
        setsByExerciseAndSession.forEach((exerciseId, sessions) -> history.put(exerciseId,
                sessions.entrySet().stream()
                        .limit(MAX_SESSIONS_PER_EXERCISE)
                        .map(entry -> new SessionPerformance(sessionDates.get(entry.getKey()), entry.getValue()))
                        .toList()));
        return history;
    }
}
