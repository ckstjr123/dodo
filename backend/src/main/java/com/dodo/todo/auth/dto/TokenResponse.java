package com.dodo.todo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 응답")
public record TokenResponse(
        @Schema(description = "서비스 access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "서비스 refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType
) {
}
