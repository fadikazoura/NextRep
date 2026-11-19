package com.fadikazoura.nextrep.config;

import com.fadikazoura.nextrep.auth.TokenService;
import com.fadikazoura.nextrep.user.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

/**
 * JWTs are stateless, so a stolen or old token would normally stay valid until it expires.
 * Each token carries the user's token version; changing the password, "log out everywhere" or deleting
 * the account changes (or removes) that version, and every older token is rejected from then on.
 */
public class TokenVersionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "The token is no longer valid", null);

    private final UserRepository userRepository;

    public TokenVersionValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }
        Number claim = jwt.getClaim(TokenService.TOKEN_VERSION_CLAIM);
        int tokenVersion = claim == null ? 0 : claim.intValue();
        return userRepository.findTokenVersionById(userId)
                .filter(current -> Objects.equals(current, tokenVersion))
                .map(current -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(REVOKED));
    }
}
