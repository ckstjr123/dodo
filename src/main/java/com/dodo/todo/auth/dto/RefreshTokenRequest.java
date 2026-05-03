package com.dodo.todo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record RefreshTokenRequest(
        @Schema(description = "서비스 refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank
        String refreshToken
) {
}
