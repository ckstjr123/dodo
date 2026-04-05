package com.dodo.todo.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
