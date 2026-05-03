package com.dodo.todo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 응답")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "회원 이메일", example = "user@example.com")
        String email
) {
}
