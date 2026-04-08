package com.dodo.todo.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.LocalDateTime;
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
    @DisplayName("Todo 생성 요청은 현재 회원 정보를 기준으로 Todo를 생성한다")
    void createTodoReturnsCreatedResponse() throws Exception {
        TodoResponse response = response(7L, "할 일 만들기");
        when(todoService.createTodo(eq(5L), any(TodoCreateRequest.class))).thenReturn(response);
        authenticate(5L);

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 10,
                                  "title": "할 일 만들기",
                                  "memo": "초안 작성",
                                  "priority": "HIGH",
                                  "sortOrder": 1,
                                  "dueAt": "2026-04-07T18:00:00",
                                  "tagIds": [20],
                                  "checklists": [
                                    { "content": "초안 점검" }
                                  ],
                                  "reminders": [
                                    { "reminderType": "ABSOLUTE_AT", "remindAt": "2026-04-07T09:00:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.categoryId").value(10))
                .andExpect(jsonPath("$.title").value("할 일 만들기"))
                .andExpect(jsonPath("$.tags[0].name").value("중요"));
    }

    @Test
    @DisplayName("Todo 생성 요청에서 필수 값이 비어 있으면 검증 오류를 반환한다")
    void createTodoReturnsValidationError() throws Exception {
        authenticate(5L);

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 10,
                                  "title": "",
                                  "priority": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.priority").exists());
    }

    @Test
    @DisplayName("Todo 목록 조회는 현재 회원의 Todo 목록을 반환한다")
    void getTodosReturnsTodoList() throws Exception {
        when(todoService.getTodos(5L)).thenReturn(new TodoListResponse(List.of(response(7L, "할 일 만들기"))));
        authenticate(5L);

        mockMvc.perform(get("/api/v1/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todos[0].id").value(7))
                .andExpect(jsonPath("$.todos[0].title").value("할 일 만들기"));
    }

    @Test
    @DisplayName("Todo 단건 조회는 Todo 응답을 반환한다")
    void getTodoReturnsTodo() throws Exception {
        when(todoService.getTodo(5L, 7L)).thenReturn(response(7L, "할 일 만들기"));
        authenticate(5L);

        mockMvc.perform(get("/api/v1/todos/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("할 일 만들기"));
    }

    @Test
    @DisplayName("Todo 목록 조회는 인증 정보가 없으면 401을 반환한다")
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

    private TodoResponse response(Long id, String title) {
        return new TodoResponse(
                id,
                10L,
                "업무",
                title,
                "초안 작성",
                TodoStatus.OPEN,
                "HIGH",
                1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                List.of(new TodoResponse.TagResponse(20L, "중요")),
                List.of(new TodoResponse.ChecklistResponse(30L, "초안 점검", false)),
                null,
                List.of()
        );
    }
}
