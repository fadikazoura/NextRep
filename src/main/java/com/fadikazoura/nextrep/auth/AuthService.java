package com.fadikazoura.nextrep.auth;

import com.fadikazoura.nextrep.user.EmailAlreadyUsedException;
import com.fadikazoura.nextrep.user.PhoneNumbers;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserRepository;
import com.fadikazoura.nextrep.user.UserResponse;
import com.fadikazoura.nextrep.user.UsernameAlreadyUsedException;
import com.fadikazoura.nextrep.user.Usernames;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        String username = request.username() != null
                ? request.username().trim()
                : Usernames.fromEmail(email, userRepository::existsByUsernameIgnoreCase);
        if (request.username() != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyUsedException(username);
        }
        String phone = PhoneNumbers.normalize(request.phone());

        User user = new User(username, email, passwordEncoder.encode(request.password()));
        user.changePhone(phone);
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups at the same moment: the unique indexes decide, we only translate the error.
            if (userRepository.existsByUsernameIgnoreCase(username)) throw new UsernameAlreadyUsedException(username);
            throw new EmailAlreadyUsedException(email);
        }
    }

    /** Logs in with the email or the username; the error never says which part was wrong. */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String login = request.login().trim();
        Optional<User> candidate = login.contains("@")
                ? userRepository.findByEmail(normalizeEmail(login))
                : userRepository.findByUsernameIgnoreCase(login);
        User user = candidate
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokenService.issueToken(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
