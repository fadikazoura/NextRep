package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.user.Equipment;
import com.fadikazoura.nextrep.user.ExperienceLevel;
import com.fadikazoura.nextrep.user.TrainingGoal;
import com.fadikazoura.nextrep.user.UserProfile;

import java.time.DayOfWeek;
import java.util.Set;

public record PlanGenerationInput(Set<DayOfWeek> trainingDays, Equipment equipment, TrainingGoal goal,
                                  ExperienceLevel experienceLevel, int sessionMinutes) {

    public static PlanGenerationInput from(UserProfile profile) {
        return new PlanGenerationInput(Set.copyOf(profile.getTrainingDays()), profile.getEquipment(),
                profile.getGoal(), profile.getExperienceLevel(), profile.getSessionMinutes());
    }
}
