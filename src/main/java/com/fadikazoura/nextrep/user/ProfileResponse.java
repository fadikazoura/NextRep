package com.fadikazoura.nextrep.user;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProfileResponse(
        LocalDate birthDate,
        Sex sex,
        int heightCm,
        ExperienceLevel experienceLevel,
        TrainingGoal goal,
        int sessionMinutes,
        Equipment equipment,
        WeightUnit weightUnit,
        List<DayOfWeek> trainingDays,
        String injuries,
        Instant updatedAt
) {

    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getBirthDate(),
                profile.getSex(),
                profile.getHeightCm(),
                profile.getExperienceLevel(),
                profile.getGoal(),
                profile.getSessionMinutes(),
                profile.getEquipment(),
                profile.getWeightUnit(),
                profile.getTrainingDays().stream().sorted().toList(),
                profile.getInjuries(),
                profile.getUpdatedAt());
    }
}