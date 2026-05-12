package com.dodo.todo.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "에러 메시지", example = "scheduledTime: HH:mm:ss 형식이어야 합니다")
        String message
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
