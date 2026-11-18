package com.fadikazoura.nextrep.bodyweight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyWeightEntryRepository extends JpaRepository<BodyWeightEntry, Long> {

    Optional<BodyWeightEntry> findByUserIdAndRecordedOn(Long userId, LocalDate recordedOn);

    List<BodyWeightEntry> findByUserIdOrderByRecordedOnAsc(Long userId);
}