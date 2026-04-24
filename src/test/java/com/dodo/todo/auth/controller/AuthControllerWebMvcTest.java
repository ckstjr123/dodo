package com.dodo.todo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.auth.service.AuthService;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("소셜 로그인 응답으로 토큰을 반환한다")
    void socialLoginReturnsTokenResponse() throws Exception {
        String accessToken = "new-access-token";
        String refreshToken = "new-refresh-token";
        String tokenType = "Bearer";
        when(authService.login(any())).thenReturn(new TokenResponse(accessToken, refreshToken, tokenType));

        mockMvc.perform(post("/api/v1/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "authorizationCode": "google-code",
                                  "redirectUri": "http://localhost:5173/auth/callback"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.tokenType").value(tokenType));
    }

    @Test
    @DisplayName("소셜 로그인 요청 검증 실패 시 오류를 반환한다")
    void socialLoginReturnsValidationError() throws Exception {
        String validationErrorCode = "VALIDATION_ERROR";

        mockMvc.perform(post("/api/v1/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "",
                                  "authorizationCode": "",
                                  "redirectUri": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(validationErrorCode))
                .andExpect(jsonPath("$.validationErrors.provider").exists())
                .andExpect(jsonPath("$.validationErrors.authorizationCode").exists())
                .andExpect(jsonPath("$.validationErrors.redirectUri").exists());
    }

    @Test
    @DisplayName("refresh 요청 응답으로 토큰을 반환한다")
    void refreshReturnsTokenResponse() throws Exception {
        String accessToken = "new-access-token";
        String refreshToken = "new-refresh-token";
        String tokenType = "Bearer";
        when(authService.refresh(any())).thenReturn(new TokenResponse(accessToken, refreshToken, tokenType));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.tokenType").value(tokenType));
    }

    @Test
    @DisplayName("refresh 요청 검증 실패 시 오류를 반환한다")
    void refreshReturnsValidationErrorForBlankToken() throws Exception {
        String validationErrorCode = "VALIDATION_ERROR";

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(validationErrorCode))
                .andExpect(jsonPath("$.validationErrors.refreshToken").exists());
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원 정보를 반환한다")
    void meReturnsCurrentMember() throws Exception {
        Long memberId = 5L;
        String email = "me@example.com";
        when(authService.getCurrentMember(memberId)).thenReturn(new MemberResponse(memberId, email));
        MemberPrincipal principal = new MemberPrincipal(memberId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("인증 없이 현재 회원을 조회하면 401을 반환한다")
    void meMapsUnauthorizedException() throws Exception {
        String unauthorizedCode = "UNAUTHORIZED";

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(unauthorizedCode));
    }
}
