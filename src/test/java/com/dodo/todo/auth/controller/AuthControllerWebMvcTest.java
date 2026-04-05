package com.dodo.todo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dodo.todo.auth.dto.LoginResponse;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtAuthenticationFilter;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.auth.service.AuthService;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회원가입 요청이 성공하면 생성된 회원 정보를 반환한다")
    void signupReturnsCreatedMember() throws Exception {
        when(authService.signup(any())).thenReturn(new MemberResponse(1L, "user@example.com", "user"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "nickname": "user"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("user"));
    }

    @Test
    @DisplayName("회원가입 요청 값이 잘못되면 검증 오류를 반환한다")
    void signupReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.validationErrors.nickname").exists());
    }

    @Test
    @DisplayName("로그인 요청이 성공하면 토큰 응답을 반환한다")
    void loginReturnsTokenResponse() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(
                new TokenResponse("jwt-token", "Bearer", 604800L),
                new MemberResponse(1L, "login@example.com", "login-user")
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.member.email").value("login@example.com"));
    }

    @Test
    @DisplayName("로그인 요청 값이 잘못되면 검증 오류를 반환한다")
    void loginReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("로그인 실패 예외는 401 응답으로 매핑된다")
    void loginMapsApiExceptionToUnauthorized() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ApiException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("내 정보 조회 시 로그인 회원 정보를 반환한다")
    void meReturnsCurrentMember() throws Exception {
        when(authService.getCurrentMember(5L)).thenReturn(new MemberResponse(5L, "me@example.com", "me-user"));
        MemberPrincipal principal = new MemberPrincipal(5L, "me@example.com", "encoded", "me-user");
        SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.nickname").value("me-user"));
    }

    @Test
    @DisplayName("내 정보 조회 시 인증이 없으면 401 응답을 반환한다")
    void meMapsUnauthorizedException() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("회원가입 시 이메일이 이미 존재하면 409 응답을 반환한다")
    void signupReturnsConflictForDuplicateEmail() throws Exception {
        when(authService.signup(any()))
                .thenThrow(new ApiException("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dup@example.com",
                                  "password": "password123",
                                  "nickname": "dupuser"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }
}