package com.fadikazoura.nextrep.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    @Query("""
            SELECT e FROM Exercise e
            WHERE (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup)
              AND (:equipment IS NULL OR e.equipment = :equipment)
            ORDER BY e.name
            """)
    List<Exercise> search(@Param("muscleGroup") MuscleGroup muscleGroup,
                          @Param("equipment") ExerciseEquipment equipment);
}
