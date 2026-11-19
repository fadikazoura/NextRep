package com.fadikazoura.nextrep.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public List<ExerciseResponse> search(@RequestParam(required = false) MuscleGroup muscleGroup,
                                         @RequestParam(required = false) ExerciseEquipment equipment) {
        return exerciseService.search(muscleGroup, equipment);
    }

    @GetMapping("/{id}")
    public ExerciseResponse getById(@PathVariable Long id) {
        return exerciseService.getById(id);
    }
}
