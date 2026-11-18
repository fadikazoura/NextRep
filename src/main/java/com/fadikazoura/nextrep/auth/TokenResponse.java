package com.fadikazoura.nextrep.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
