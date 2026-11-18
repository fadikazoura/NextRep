package com.fadikazoura.nextrep.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MIN_AGE = 13;
    private static final int MAX_AGE = 100;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .map(ProfileResponse::from)
                .orElseThrow(ProfileNotFoundException::new);
    }

    @Transactional
    public ProfileResponse upsertProfile(Long userId, ProfileRequest request) {
        validateAge(request.birthDate());

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> createProfile(userId, request));
        applyRequest(profile, request);

        return ProfileResponse.from(userProfileRepository.saveAndFlush(profile));
    }

    private UserProfile createProfile(Long userId, ProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return new UserProfile(
                user,
                request.birthDate(),
                request.sex(),
                request.heightCm().shortValue(),
                request.experienceLevel(),
                request.goal(),
                request.sessionMinutes().shortValue(),
                request.equipment(),
                request.trainingDays());
    }

    private static void applyRequest(UserProfile profile, ProfileRequest request) {
        profile.setBirthDate(request.birthDate());
        profile.setSex(request.sex());
        profile.setHeightCm(request.heightCm().shortValue());
        profile.setExperienceLevel(request.experienceLevel());
        profile.setGoal(request.goal());
        profile.setSessionMinutes(request.sessionMinutes().shortValue());
        profile.setEquipment(request.equipment());
        profile.setWeightUnit(request.weightUnit());
        profile.setInjuries(request.injuries());
        profile.replaceTrainingDays(request.trainingDays());
    }

    private static void validateAge(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new InvalidProfileException("Age must be between " + MIN_AGE + " and " + MAX_AGE);
        }
    }
}
