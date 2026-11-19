package com.fadikazoura.nextrep.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    /**
     * Sets of the given exercises from the user's completed workouts since a date, newest workout first.
     * The session is fetched in the same query (JOIN FETCH) to avoid N+1.
     */
    @Query("""
            SELECT s FROM WorkoutSet s
            JOIN FETCH s.session ws
            WHERE ws.user.id = :userId
              AND s.exercise.id IN :exerciseIds
              AND ws.completedAt IS NOT NULL
              AND ws.startedAt >= :since
            ORDER BY ws.startedAt DESC, s.id
            """)
    List<WorkoutSet> findCompletedSets(@Param("userId") Long userId,
                                       @Param("exerciseIds") Collection<Long> exerciseIds,
                                       @Param("since") Instant since);

    /**
     * Every distinct (exercise, weight, reps) the user completed, with the first time it was done.
     * The database collapses repeated sets, so the result stays small even after years of training.
     */
    @Query("""
            SELECT s.exercise.id AS exerciseId, s.weightKg AS weightKg, s.reps AS reps,
                   MIN(ws.startedAt) AS firstPerformedAt
            FROM WorkoutSet s
            JOIN s.session ws
            WHERE ws.user.id = :userId
              AND ws.completedAt IS NOT NULL
            GROUP BY s.exercise.id, s.weightKg, s.reps
            """)
    List<SetSummary> findCompletedSetSummaries(@Param("userId") Long userId);

    @Query("""
            SELECT s FROM WorkoutSet s
            JOIN FETCH s.session ws
            WHERE ws.user.id = :userId
              AND s.exercise.id = :exerciseId
              AND ws.completedAt IS NOT NULL
            ORDER BY ws.startedAt, s.id
            """)
    List<WorkoutSet> findCompletedSetsOfExercise(@Param("userId") Long userId,
                                                 @Param("exerciseId") Long exerciseId);

    /** Completed sets per muscle group since a moment in time, counted by the database (GROUP BY). */
    @Query("""
            SELECT s.exercise.muscleGroup AS muscleGroup, COUNT(s) AS sets
            FROM WorkoutSet s
            JOIN s.session ws
            WHERE ws.user.id = :userId
              AND ws.completedAt IS NOT NULL
              AND ws.startedAt >= :since
            GROUP BY s.exercise.muscleGroup
            """)
    List<MuscleSetCount> countCompletedSetsPerMuscle(@Param("userId") Long userId, @Param("since") Instant since);
}
