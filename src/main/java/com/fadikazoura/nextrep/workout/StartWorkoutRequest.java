package com.fadikazoura.nextrep.workout;

/** Optional body: a workout can follow a day of the plan or be a free workout. */
public record StartWorkoutRequest(Long planDayId) {
}
