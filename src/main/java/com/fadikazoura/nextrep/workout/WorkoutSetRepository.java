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
}
