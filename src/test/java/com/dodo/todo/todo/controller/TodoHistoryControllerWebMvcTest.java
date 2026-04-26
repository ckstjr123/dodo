package com.dodo.todo.todo.controller;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.dodo.todo.util.TestFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class TodoHistoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoHistoryRepository todoHistoryRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("완료 이력은 커서 기반 응답을 반환한다")
    void getHistoriesReturnsCursorResponse() throws Exception {
        Long memberId = 5L;
        LocalDateTime completedAt = LocalDateTime.of(2026, 4, 7, 12, 0);
        Member member = createMember(memberId);
        Todo todo = createTodo(7L, member, createCategory(member, "업무"), "보고서 작성", TodoStatus.TODO);
        TodoHistory history = TodoHistory.create(todo, completedAt);

        when(todoHistoryRepository.findHistories(eq(memberId), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(history), PageRequest.of(0, 30), false));
        authenticate(memberId);

        mockMvc.perform(get("/api/v1/todos/histories")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.histories[0].todoId").value(7L))
                .andExpect(jsonPath("$.histories[0].title").value("보고서 작성"))
                .andExpect(jsonPath("$.hasNext").value(false));
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
