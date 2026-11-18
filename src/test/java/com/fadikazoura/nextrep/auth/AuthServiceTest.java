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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}
