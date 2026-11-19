package com.fadikazoura.nextrep.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public List<ExerciseResponse> search(MuscleGroup muscleGroup, ExerciseEquipment equipment) {
        return exerciseRepository.search(muscleGroup, equipment).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    public ExerciseResponse getById(Long id) {
        return exerciseRepository.findById(id)
                .map(ExerciseResponse::from)
                .orElseThrow(() -> new ExerciseNotFoundException(id));
    }

    public ExerciseGuideResponse getGuide(Long id) {
        return exerciseRepository.findById(id)
                .map(ExerciseGuideResponse::from)
                .orElseThrow(() -> new ExerciseNotFoundException(id));
    }
}
