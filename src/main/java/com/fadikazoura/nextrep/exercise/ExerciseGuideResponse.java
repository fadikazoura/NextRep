package com.fadikazoura.nextrep.exercise;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * How to perform an exercise. Both languages are returned so the client can switch language without another call.
 * The video link is a YouTube search, so it never breaks when a specific video is removed.
 */
public record ExerciseGuideResponse(Long exerciseId, String name, MuscleGroup muscleGroup,
                                    ExerciseEquipment equipment, Guide en, Guide he, URI videoSearchUrl) {

    public record Guide(List<String> steps, String commonMistake) {

        static Guide of(String instructions, String commonMistake) {
            List<String> steps = instructions == null ? List.of()
                    : Arrays.stream(instructions.split("\\R")).map(String::strip).filter(line -> !line.isEmpty()).toList();
            return new Guide(steps, commonMistake);
        }
    }

    public static ExerciseGuideResponse from(Exercise exercise) {
        String query = URLEncoder.encode(exercise.getName() + " proper form", StandardCharsets.UTF_8);
        return new ExerciseGuideResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                exercise.getEquipment(),
                Guide.of(exercise.getInstructionsEn(), exercise.getCommonMistakeEn()),
                Guide.of(exercise.getInstructionsHe(), exercise.getCommonMistakeHe()),
                URI.create("https://www.youtube.com/results?search_query=" + query));
    }
}
