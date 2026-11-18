package com.fadikazoura.nextrep.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record ProfileRequest(

        @NotNull
        @Past
        LocalDate birthDate,

        @NotNull
        Sex sex,

        @NotNull
        @Min(100)
        @Max(250)
        Integer heightCm,

        @NotNull
        ExperienceLevel experienceLevel,

        @NotNull
        TrainingGoal goal,

        @NotNull
        @Min(20)
        @Max(180)
        Integer sessionMinutes,

        @NotNull
        Equipment equipment,

        @NotNull
        WeightUnit weightUnit,

        @NotEmpty
        Set<DayOfWeek> trainingDays,

        @Size(max = 500)
        String injuries
) {
}
