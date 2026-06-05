package com.dodo.todo.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "임시 푸시 알림 테스트 요청")
public record PushTestRequest(
        @Schema(description = "Firebase Cloud Messaging token", example = "fcm-token")
        @NotBlank
        String fcmToken,

        @Schema(description = "푸시 알림 제목", example = "Todo 알림 테스트")
        @NotBlank
        String title,

        @Schema(description = "푸시 알림 본문", example = "테스트 메시지입니다.")
        @NotBlank
        String body
) {
}
