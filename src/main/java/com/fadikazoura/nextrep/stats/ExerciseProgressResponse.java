package com.fadikazoura.nextrep.stats;

import java.util.List;

public record ExerciseProgressResponse(Long exerciseId, String exerciseName, List<ProgressPoint> points) {
}
