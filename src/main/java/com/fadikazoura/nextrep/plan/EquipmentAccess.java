package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.user.Equipment;

import java.util.EnumSet;
import java.util.Set;

/** Which catalog equipment a user can actually use. Shared by the default generator and the AI plan validator. */
public final class EquipmentAccess {

    private EquipmentAccess() {
    }

    public static Set<ExerciseEquipment> allowedFor(Equipment equipment) {
        return switch (equipment) {
            case FULL_GYM -> EnumSet.allOf(ExerciseEquipment.class);
            case DUMBBELLS_ONLY -> EnumSet.of(ExerciseEquipment.DUMBBELL, ExerciseEquipment.BODYWEIGHT);
            case HOME -> EnumSet.of(ExerciseEquipment.BODYWEIGHT);
        };
    }
}
