package com.dodo.todo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM token 등록 요청")
public record FcmTokenRequest(
        @Schema(description = "Firebase Cloud Messaging token", example = "fcm-token")
        @NotBlank
        String fcmToken
) {
}
