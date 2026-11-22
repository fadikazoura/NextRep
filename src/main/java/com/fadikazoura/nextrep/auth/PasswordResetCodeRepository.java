package com.fadikazoura.nextrep.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    /** The newest code that was not used yet (older ones are closed when a new one is created). */
    Optional<PasswordResetCode> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    /** Closes every open code of the user, so only the newest email's code works. */
    @Modifying
    @Query("UPDATE PasswordResetCode c SET c.usedAt = :now WHERE c.userId = :userId AND c.usedAt IS NULL")
    void closeOpenCodes(@Param("userId") Long userId, @Param("now") Instant now);
}
