package com.dodo.todo.auth.dto;

public record LoginResponse(
        TokenResponse token,
        MemberResponse member
) {
}
