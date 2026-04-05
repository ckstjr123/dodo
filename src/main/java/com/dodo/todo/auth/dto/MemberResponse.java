package com.dodo.todo.auth.dto;

public record MemberResponse(
        Long id,
        String email,
        String nickname
) {
}
