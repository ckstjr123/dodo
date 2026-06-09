package com.dodo.todo.reminder.controller;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import com.dodo.todo.reminder.service.ReminderService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class ReminderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService reminderService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("알림 생성 요청은 생성된 알림 정보를 반환한다")
    void createReminderReturnsResponse() throws Exception {
        Long memberId = 1L;
        Long todoId = 10L;
        when(reminderService.saveReminder(eq(memberId), any(ReminderCreateRequest.class)))
                .thenReturn(100L);
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReminderCreateRequest(todoId, null, 10, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderId").value(100L));
    }

    @Test
    @DisplayName("minuteOffset이 음수면 알림 생성 요청에 실패한다")
    void createReminderRejectsNegativeMinuteOffset() throws Exception {
        authenticate(1L);

        mockMvc.perform(post("/api/v1/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderCreateRequest(10L, null, -1, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("알림 수정 요청은 수정된 알림 정보를 반환한다")
    void updateReminderReturnsResponse() throws Exception {
        Long memberId = 1L;
        Long reminderId = 100L;
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/reminders/{reminderId}", reminderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReminderUpdateRequest(30, null))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("알림 삭제 요청은 204를 반환한다")
    void deleteReminderReturnsNoContent() throws Exception {
        Long memberId = 1L;
        Long reminderId = 100L;
        authenticate(memberId);

        mockMvc.perform(delete("/api/v1/reminders/{reminderId}", reminderId))
                .andExpect(status().isNoContent());
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
