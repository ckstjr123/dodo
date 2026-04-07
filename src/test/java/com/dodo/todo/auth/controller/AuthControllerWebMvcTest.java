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
    @DisplayName("소셜 로그인 요청이 성공하면 토큰 응답을 반환한다")
    void socialLoginReturnsTokenResponse() throws Exception {
        when(authService.login(any())).thenReturn(new TokenResponse(
                "new-access-token",
                "new-refresh-token",
                "Bearer"
        ));

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
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("소셜 로그인 요청값이 비어 있으면 검증 오류를 반환한다")
    void socialLoginReturnsValidationError() throws Exception {
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
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.provider").exists())
                .andExpect(jsonPath("$.validationErrors.authorizationCode").exists())
                .andExpect(jsonPath("$.validationErrors.redirectUri").exists());
    }

    @Test
    @DisplayName("리프레시 요청이 성공하면 토큰 응답을 반환한다")
    void refreshReturnsTokenResponse() throws Exception {
        when(authService.refresh(any())).thenReturn(new TokenResponse(
                "new-access-token",
                "new-refresh-token",
                "Bearer"
        ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("리프레시 요청값이 비어 있으면 검증 오류를 반환한다")
    void refreshReturnsValidationErrorForBlankToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.refreshToken").exists());
    }

    @Test
    @DisplayName("현재 회원 조회는 로그인한 회원 정보를 반환한다")
    void meReturnsCurrentMember() throws Exception {
        when(authService.getCurrentMember(5L)).thenReturn(new MemberResponse(5L, "me@example.com"));
        MemberPrincipal principal = new MemberPrincipal(5L);
        SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    @DisplayName("현재 회원 조회 시 인증 정보가 없으면 401 응답을 반환한다")
    void meMapsUnauthorizedException() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
