package com.dodo.todo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(
        @Schema(description = "소셜 로그인 제공자", example = "GOOGLE", allowableValues = {"GOOGLE"})
        @NotBlank
        String provider,

        @Schema(description = "소셜 제공자에서 발급받은 access token", example = "ya29.a0AfH6SM...")
        @NotBlank
        String accessToken
) {
}
