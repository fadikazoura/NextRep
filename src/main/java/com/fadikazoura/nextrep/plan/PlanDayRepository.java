package com.fadikazoura.nextrep.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanDayRepository extends JpaRepository<PlanDay, Long> {

    /** Finds a plan day only among the user's own plans. */
    Optional<PlanDay> findByIdAndPlanUserId(Long id, Long userId);
}
