package com.fadikazoura.nextrep.account;

import com.fadikazoura.nextrep.bodyweight.BodyWeightResponse;
import com.fadikazoura.nextrep.feedback.ContactMessageResponse;
import com.fadikazoura.nextrep.feedback.RatingResponse;
import com.fadikazoura.nextrep.plan.TrainingPlanResponse;
import com.fadikazoura.nextrep.user.ProfileResponse;
import com.fadikazoura.nextrep.user.UserResponse;
import com.fadikazoura.nextrep.workout.WorkoutResponse;

import java.time.Instant;
import java.util.List;

/** Everything NextRep stores about a user, in one file ("right of access" / data portability). */
public record DataExport(Instant exportedAt, UserResponse account, ProfileResponse profile,
                         List<BodyWeightResponse> bodyWeight, List<TrainingPlanResponse> plans,
                         List<WorkoutResponse> workouts, RatingResponse rating,
                         List<ContactMessageResponse> contactMessages) {
}
