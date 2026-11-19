package com.fadikazoura.nextrep.workout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    /** Looks a workout up only within the user's own workouts, so other users' ids behave as "not found". */
    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);

    Optional<WorkoutSession> findByUserIdAndCompletedAtIsNull(Long userId);

    /** Completed workouts only, newest first. */
    Page<WorkoutSession> findByUserIdAndCompletedAtIsNotNullOrderByStartedAtDesc(Long userId, Pageable pageable);
}
