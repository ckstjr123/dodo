package com.dodo.todo.auth.controller;

import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SocialLoginRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApiDocs {

    @Operation(summary = "소셜 로그인")
    TokenResponse socialLogin(SocialLoginRequest request);

    @Operation(summary = "리프레시 토큰으로 토큰 재발급")
    TokenResponse refresh(RefreshTokenRequest request);

    @Operation(summary = "현재 로그인한 회원 조회")
    @SecurityRequirement(name = "bearerAuth")
    MemberResponse me(Long memberId);
}
