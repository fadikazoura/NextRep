package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.exercise.Exercise;
import com.fadikazoura.nextrep.exercise.ExerciseNotFoundException;
import com.fadikazoura.nextrep.exercise.ExerciseRepository;
import com.fadikazoura.nextrep.plan.PlanDay;
import com.fadikazoura.nextrep.plan.PlanDayNotFoundException;
import com.fadikazoura.nextrep.plan.PlanDayRepository;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserNotFoundException;
import com.fadikazoura.nextrep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final UserRepository userRepository;
    private final PlanDayRepository planDayRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutResponse start(Long userId, StartWorkoutRequest request) {
        workoutSessionRepository.findByUserIdAndCompletedAtIsNull(userId).ifPresent(open -> {
            throw new WorkoutInProgressException(open.getId());
        });

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        PlanDay planDay = null;
        if (request != null && request.planDayId() != null) {
            planDay = planDayRepository.findByIdAndPlanUserId(request.planDayId(), userId)
                    .orElseThrow(() -> new PlanDayNotFoundException(request.planDayId()));
        }
        return WorkoutResponse.from(workoutSessionRepository.saveAndFlush(new WorkoutSession(user, planDay)));
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getCurrent(Long userId) {
        return workoutSessionRepository.findByUserIdAndCompletedAtIsNull(userId)
                .map(WorkoutResponse::from)
                .orElseThrow(WorkoutNotFoundException::noneInProgress);
    }

    @Transactional(readOnly = true)
    public WorkoutResponse get(Long userId, Long workoutId) {
        return WorkoutResponse.from(findOwned(userId, workoutId));
    }

    public WorkoutSetResponse addSet(Long userId, Long workoutId, AddSetRequest request) {
        WorkoutSession session = findOwned(userId, workoutId);
        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> new ExerciseNotFoundException(request.exerciseId()));

        WorkoutSet set = session.addSet(exercise, request.weightKg(), request.reps());
        // Flush (not save) so the cascade persists this exact set instance and assigns its id.
        workoutSessionRepository.flush();
        return WorkoutSetResponse.from(set);
    }

    public void removeSet(Long userId, Long workoutId, Long setId) {
        findOwned(userId, workoutId).removeSet(setId);
    }

    public WorkoutResponse complete(Long userId, Long workoutId) {
        WorkoutSession session = findOwned(userId, workoutId);
        session.complete();
        return WorkoutResponse.from(session);
    }

    public void discard(Long userId, Long workoutId) {
        WorkoutSession session = findOwned(userId, workoutId);
        if (session.isCompleted()) {
            throw new WorkoutAlreadyCompletedException(workoutId);
        }
        workoutSessionRepository.delete(session);
    }

    private WorkoutSession findOwned(Long userId, Long workoutId) {
        return workoutSessionRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));
    }
}
