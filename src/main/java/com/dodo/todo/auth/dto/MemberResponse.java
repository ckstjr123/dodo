package com.dodo.todo.auth.dto;

public record MemberResponse(
        Long memberId,
        String email
) {
}
