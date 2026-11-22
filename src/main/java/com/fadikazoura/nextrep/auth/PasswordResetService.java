package com.fadikazoura.nextrep.auth;

import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * "Forgot password" with a 6-digit code sent by email.
 * <ul>
 *   <li>The answer is the same whether the email exists or not, so attackers can't find out who is registered.</li>
 *   <li>Only a BCrypt hash of the code is stored; it expires after 15 minutes and allows 5 guesses.</li>
 *   <li>At most 3 codes per hour per account, so nobody can flood a mailbox.</li>
 *   <li>A successful reset logs the user out on every device (token version).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final int MAX_CODES_PER_HOUR = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public void requestCode(PasswordResetRequest request) {
        Optional<User> found = userRepository.findByEmail(normalize(request.email()));
        if (found.isEmpty()) {
            return; // same response as for a real account
        }
        User user = found.get();
        Instant now = clock.instant();
        if (codeRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1))) >= MAX_CODES_PER_HOUR) {
            log.info("Password reset limit reached for user {}", user.getId());
            return;
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        codeRepository.closeOpenCodes(user.getId(), now);
        codeRepository.save(new PasswordResetCode(user.getId(), passwordEncoder.encode(code), now.plus(CODE_TTL)));
        events.publishEvent(new PasswordResetRequested(user.getEmail(), user.getUsername(), code, CODE_TTL.toMinutes()));
    }

    /** A wrong guess must be counted even though we throw, so the transaction does not roll back for it. */
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public void resetPassword(PasswordResetConfirmation confirmation) {
        User user = userRepository.findByEmail(normalize(confirmation.email()))
                .orElseThrow(InvalidResetCodeException::new);
        Instant now = clock.instant();
        PasswordResetCode code = codeRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(candidate -> candidate.usableAt(now))
                .orElseThrow(InvalidResetCodeException::new);

        if (!passwordEncoder.matches(confirmation.code(), code.getCodeHash())) {
            code.recordWrongAttempt();
            throw new InvalidResetCodeException();
        }
        code.markUsed(now);
        user.changePassword(passwordEncoder.encode(confirmation.newPassword()));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
