package com.dodo.todo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank
        String provider,

        @NotBlank
        String authorizationCode,

        @NotBlank
        String redirectUri
) {
}
