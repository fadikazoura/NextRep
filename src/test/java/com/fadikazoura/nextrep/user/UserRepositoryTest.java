package com.fadikazoura.nextrep.user;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.EnumSet;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesUserAndFindsItByEmail() {
        userRepository.saveAndFlush(new User("fadi", "fadi@example.com", "hashed-password"));

        var found = userRepository.findByEmail("fadi@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmailReturnsTrueOnlyForExistingEmail() {
        userRepository.saveAndFlush(new User("exists", "exists@example.com", "hashed-password"));

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(new User("dup1", "dup@example.com", "hash-1"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("dup2", "dup@example.com", "hash-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesProfileWithSharedIdAndTrainingDays() {
        User user = userRepository.save(new User("profile", "profile@example.com", "hashed-password"));
        UserProfile profile = new UserProfile(
                user,
                LocalDate.of(2003, 5, 10),
                Sex.MALE,
                (short) 180,
                ExperienceLevel.INTERMEDIATE,
                TrainingGoal.HYPERTROPHY,
                (short) 60,
                Equipment.FULL_GYM,
                EnumSet.of(MONDAY, WEDNESDAY, FRIDAY));

        userProfileRepository.saveAndFlush(profile);
        entityManager.clear();

        UserProfile found = userProfileRepository.findById(user.getId()).orElseThrow();

        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getTrainingDays()).containsExactlyInAnyOrder(MONDAY, WEDNESDAY, FRIDAY);
        assertThat(found.getWeightUnit()).isEqualTo(WeightUnit.KG);
    }

    @Test
    void databaseRejectsInvalidHeight() {
        User user = userRepository.save(new User("short", "short@example.com", "hashed-password"));
        UserProfile profile = new UserProfile(
                user,
                LocalDate.of(2003, 5, 10),
                Sex.MALE,
                (short) 50,
                ExperienceLevel.BEGINNER,
                TrainingGoal.STRENGTH,
                (short) 60,
                Equipment.FULL_GYM,
                EnumSet.of(MONDAY));

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(profile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
