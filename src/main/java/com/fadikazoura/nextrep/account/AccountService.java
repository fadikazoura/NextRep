package com.fadikazoura.nextrep.account;

import com.fadikazoura.nextrep.auth.TokenResponse;
import com.fadikazoura.nextrep.auth.TokenService;
import com.fadikazoura.nextrep.bodyweight.BodyWeightEntryRepository;
import com.fadikazoura.nextrep.bodyweight.BodyWeightResponse;
import com.fadikazoura.nextrep.feedback.AppRatingRepository;
import com.fadikazoura.nextrep.feedback.ContactMessageRepository;
import com.fadikazoura.nextrep.feedback.ContactMessageResponse;
import com.fadikazoura.nextrep.feedback.RatingResponse;
import com.fadikazoura.nextrep.plan.TrainingPlanRepository;
import com.fadikazoura.nextrep.plan.TrainingPlanResponse;
import com.fadikazoura.nextrep.user.PhoneNumbers;
import com.fadikazoura.nextrep.user.ProfileResponse;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserNotFoundException;
import com.fadikazoura.nextrep.user.UserProfileRepository;
import com.fadikazoura.nextrep.user.UserRepository;
import com.fadikazoura.nextrep.user.UserResponse;
import com.fadikazoura.nextrep.user.UsernameAlreadyUsedException;
import com.fadikazoura.nextrep.workout.WorkoutResponse;
import com.fadikazoura.nextrep.workout.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** Account settings: username, phone, avatar, password, sessions, data export and account deletion. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final UserProfileRepository userProfileRepository;
    private final BodyWeightEntryRepository bodyWeightEntryRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final AppRatingRepository appRatingRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    @Transactional
    public UserResponse updateAccount(Long userId, AccountUpdateRequest request) {
        User user = findUser(userId);
        String username = request.username().trim();
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyUsedException(username);
        }
        user.changeUsername(username);
        user.changePhone(PhoneNumbers.normalize(request.phone()));
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyUsedException(username); // taken in the same moment by someone else
        }
    }

    /** Changes the password and returns a fresh token: this device stays logged in, every other one is logged out. */
    @Transactional
    public TokenResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        requirePassword(user, request.currentPassword(), "currentPassword");
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
        return tokenService.issueToken(user);
    }

    @Transactional
    public void logOutEverywhere(Long userId) {
        User user = findUser(userId);
        user.revokeTokens();
    }

    /** Deletes the user; the database removes profile, plans, workouts, body weight and avatar (ON DELETE CASCADE). */
    @Transactional
    public void deleteAccount(Long userId, PasswordConfirmation confirmation) {
        User user = findUser(userId);
        requirePassword(user, confirmation.password(), "password");
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public DataExport export(Long userId) {
        User user = findUser(userId);
        return new DataExport(
                clock.instant(),
                UserResponse.from(user),
                userProfileRepository.findById(userId).map(ProfileResponse::from).orElse(null),
                bodyWeightEntryRepository.findByUserIdOrderByRecordedOnAsc(userId).stream().map(BodyWeightResponse::from).toList(),
                trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(TrainingPlanResponse::from).toList(),
                workoutSessionRepository.findByUserIdOrderByStartedAtAsc(userId).stream().map(WorkoutResponse::from).toList(),
                appRatingRepository.findById(userId).map(RatingResponse::from).orElse(null),
                contactMessageRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(ContactMessageResponse::from).toList());
    }

    /* ---------- avatar ---------- */

    @Transactional
    public UserResponse choosePreset(Long userId, String preset) {
        if (!AvatarImages.PRESETS.contains(preset)) throw new InvalidAvatarException("Unknown avatar: " + preset);
        User user = findUser(userId);
        user.changeAvatar(preset);
        userAvatarRepository.deleteById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse uploadPhoto(Long userId, byte[] data) {
        String contentType = AvatarImages.detectContentType(data);
        User user = findUser(userId);
        UserAvatar avatar = userAvatarRepository.findById(userId).orElseGet(() -> new UserAvatar(userId, contentType, data));
        avatar.replace(contentType, data);
        userAvatarRepository.save(avatar);
        user.changeAvatar(User.PHOTO_AVATAR);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse resetAvatar(Long userId) {
        User user = findUser(userId);
        user.changeAvatar(null);
        userAvatarRepository.deleteById(userId);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserAvatar> photo(Long userId) {
        return userAvatarRepository.findById(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void requirePassword(User user, String password, String field) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw new IncorrectPasswordException(field);
    }
}
