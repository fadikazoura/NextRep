package com.fadikazoura.nextrep.auth;

import com.fadikazoura.nextrep.user.EmailAlreadyUsedException;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserRepository;
import com.fadikazoura.nextrep.user.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmail("fadi@example.com")).thenReturn(false);
        when(passwordEncoder.encode("supersecret123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  Fadi@Example.COM ", "supersecret123"));

        assertThat(response.email()).isEqualTo("fadi@example.com");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("fadi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("fadi@example.com", "supersecret123")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerTranslatesUniqueViolationIntoEmailAlreadyUsed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("race@example.com", "supersecret123")))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User("fadi@example.com", "hashed-password");
        TokenResponse token = new TokenResponse("jwt-token", "Bearer", 3600);
        when(userRepository.findByEmail("fadi@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("supersecret123", "hashed-password")).thenReturn(true);
        when(tokenService.issueToken(user)).thenReturn(token);

        TokenResponse response = authService.login(new LoginRequest(" FADI@example.com ", "supersecret123"));

        assertThat(response).isEqualTo(token);
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("fadi@example.com", "hashed-password");
        when(userRepository.findByEmail("fadi@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("fadi@example.com", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "supersecret123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenService);
    }

}
