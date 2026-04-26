package com.dodo.todo.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import com.dodo.todo.todo.service.TodoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

@WebMvcTest(TodoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class TodoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService todoService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Todo 생성 응답으로 todoId를 반환한다")
    void createTodoReturnsCreatedResponse() throws Exception {
        Long memberId = 5L;
        Long todoId = 7L;
        String requestBody = """
                {
                  "categoryId": 10,
                  "title": "보고서 작성",
                  "memo": "초안부터 작성",
                  "sortOrder": 1,
                  "dueAt": "2026-04-07T18:00:00",
                  "scheduledDate": "2026-04-07",
                  "scheduledTime": "14:00:00"
                }
                """;
        when(todoService.saveTodo(eq(memberId), any(TodoCreateRequest.class))).thenReturn(todoId);
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.todoId").value(todoId));
    }

    @Test
    @DisplayName("Todo 생성 요청 검증 실패 시 오류를 반환한다")
    void createTodoReturnsValidationError() throws Exception {
        Long memberId = 5L;
        String requestBody = """
                {
                  "categoryId": 10,
                  "title": ""
                }
                """;
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("Todo 목록 조회 응답으로 목록을 반환한다")
    void getTodosReturnsTodoList() throws Exception {
        Long memberId = 5L;
        Long todoId = 7L;
        when(todoService.getTodos(memberId)).thenReturn(new TodoListResponse(List.of(response(todoId, "보고서 작성"))));
        authenticate(memberId);

        mockMvc.perform(get("/api/v1/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todos[0].todoId").value(todoId))
                .andExpect(jsonPath("$.todos[0].title").value("보고서 작성"))
                .andExpect(jsonPath("$.todos[0].subTodos[0].title").value("초안 작성"));
    }

    @Test
    @DisplayName("Todo 상세 조회 응답으로 Todo를 반환한다")
    void getTodoReturnsTodo() throws Exception {
        Long memberId = 5L;
        Long todoId = 7L;
        when(todoService.getTodo(memberId, todoId)).thenReturn(response(todoId, "보고서 작성"));
        authenticate(memberId);

        mockMvc.perform(get("/api/v1/todos/{todoId}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todoId").value(todoId))
                .andExpect(jsonPath("$.title").value("보고서 작성"))
                .andExpect(jsonPath("$.subTodos[0].title").value("초안 작성"));
    }

    @Test
    @DisplayName("Todo 완료 요청은 204를 반환한다")
    void completeTodoReturnsNoContent() throws Exception {
        Long memberId = 5L;
        Long todoId = 7L;
        doNothing().when(todoService).completeTodo(memberId, todoId);
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/todos/{todoId}/complete", todoId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Todo 완료 취소 요청은 204를 반환한다")
    void undoTodoReturnsNoContent() throws Exception {
        Long memberId = 5L;
        Long todoId = 7L;
        doNothing().when(todoService).undoTodo(memberId, todoId);
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/todos/{todoId}/undo", todoId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증 없이 Todo 목록을 조회하면 401을 반환한다")
    void getTodosReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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

    private TodoResponse response(Long todoId, String title) {
        return new TodoResponse(
                todoId,
                null,
                10L,
                "업무",
                title,
                "초안부터 작성",
                TodoStatus.TODO,
                1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDate.of(2026, 4, 7),
                LocalTime.of(14, 0),
                null,
                List.of(new TodoResponse(
                        30L,
                        todoId,
                        10L,
                        "업무",
                        "초안 작성",
                        "초안 문장 정리",
                        TodoStatus.TODO,
                        2,
                        LocalDateTime.of(2026, 4, 7, 15, 0),
                        LocalDate.of(2026, 4, 7),
                        LocalTime.of(15, 0),
                        null,
                        List.of()
                ))
        );
    }
}
