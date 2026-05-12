package com.dodo.todo.reminder.controller;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.reminder.dto.ReminderResponse;
import com.dodo.todo.reminder.service.ReminderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
import static org.mockito.Mockito.doNothing;
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
        when(reminderService.createReminder(eq(memberId), eq(todoId), any(ReminderRequest.class)))
                .thenReturn(new ReminderResponse(100L, 10, LocalDateTime.of(2026, 5, 20, 8, 50)));
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/todos/{todoId}/reminders", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderRequest(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderId").value(100L))
                .andExpect(jsonPath("$.minuteOffset").value(10))
                .andExpect(jsonPath("$.remindAt").value("2026-05-20T08:50:00"));
    }

    @Test
    @DisplayName("minuteOffset이 없으면 알림 생성 요청에 실패한다")
    void createReminderRejectsMissingMinuteOffset() throws Exception {
        authenticate(1L);

        mockMvc.perform(post("/api/v1/todos/{todoId}/reminders", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("minuteOffset이 null이면 알림 생성 요청에 실패한다")
    void createReminderRejectsNullMinuteOffset() throws Exception {
        authenticate(1L);

        mockMvc.perform(post("/api/v1/todos/{todoId}/reminders", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minuteOffset\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("minuteOffset이 음수면 알림 생성 요청에 실패한다")
    void createReminderRejectsNegativeMinuteOffset() throws Exception {
        authenticate(1L);

        mockMvc.perform(post("/api/v1/todos/{todoId}/reminders", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderRequest(-1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("알림 수정 요청은 수정된 알림 정보를 반환한다")
    void updateReminderReturnsResponse() throws Exception {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        when(reminderService.updateReminder(eq(memberId), eq(todoId), eq(reminderId), any(ReminderRequest.class)))
                .thenReturn(new ReminderResponse(reminderId, 30, LocalDateTime.of(2026, 5, 20, 8, 30)));
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/todos/{todoId}/reminders/{reminderId}", todoId, reminderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderRequest(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderId").value(reminderId))
                .andExpect(jsonPath("$.minuteOffset").value(30));
    }

    @Test
    @DisplayName("알림 삭제 요청은 204를 반환한다")
    void deleteReminderReturnsNoContent() throws Exception {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        doNothing().when(reminderService).deleteReminder(memberId, todoId, reminderId);
        authenticate(memberId);

        mockMvc.perform(delete("/api/v1/todos/{todoId}/reminders/{reminderId}", todoId, reminderId))
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
