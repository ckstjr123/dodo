package com.dodo.todo.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.dodo.todo.member.dto.FcmTokenRequest;
import com.dodo.todo.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class MemberControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FCM token 변경 요청은 204를 반환한다")
    void updateFcmTokenReturnsNoContent() throws Exception {
        Long memberId = 1L;
        doNothing().when(memberService).updateFcmToken(eq(memberId), any(FcmTokenRequest.class));
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/members/me/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FcmTokenRequest("fcm-token"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("빈 FCM token 요청은 400을 반환한다")
    void updateFcmTokenRejectsBlankToken() throws Exception {
        authenticate(1L);

        mockMvc.perform(patch("/api/v1/members/me/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FcmTokenRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 FCM token을 변경하면 401을 반환한다")
    void updateFcmTokenRejectsUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FcmTokenRequest("fcm-token"))))
                .andExpect(status().isUnauthorized());
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = new MemberPrincipal(memberId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
