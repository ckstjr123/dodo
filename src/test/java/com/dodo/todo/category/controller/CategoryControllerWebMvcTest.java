package com.dodo.todo.category.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.resolver.LoginMemberArgumentResolver;
import com.dodo.todo.category.domain.CategoryError;
import com.dodo.todo.category.dto.CategoryListResponse;
import com.dodo.todo.category.dto.CategoryRequest;
import com.dodo.todo.category.dto.CategoryResponse;
import com.dodo.todo.category.service.CategoryService;
import com.dodo.todo.common.config.WebMvcConfig;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginMemberArgumentResolver.class})
class CategoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("카테고리 생성 응답으로 categoryId를 반환한다")
    void createCategoryReturnsCreatedResponse() throws Exception {
        Long memberId = 5L;
        Long categoryId = 10L;
        CategoryRequest request = new CategoryRequest("업무");

        when(categoryService.createCategory(eq(memberId), any(CategoryRequest.class))).thenReturn(categoryId);
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    @Test
    @DisplayName("카테고리 생성 요청 검증 실패 시 400을 반환한다")
    void createCategoryReturnsValidationError() throws Exception {
        Long memberId = 5L;
        CategoryRequest request = new CategoryRequest("");
        authenticate(memberId);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카테고리 목록 조회 응답으로 목록을 반환한다")
    void getCategoriesReturnsCategoryList() throws Exception {
        Long memberId = 5L;

        when(categoryService.getCategories(memberId)).thenReturn(new CategoryListResponse(List.of(
                new CategoryResponse(10L, "업무"),
                new CategoryResponse(11L, "개인")
        )));
        authenticate(memberId);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryId").value(10L))
                .andExpect(jsonPath("$.categories[0].name").value("업무"))
                .andExpect(jsonPath("$.categories[1].categoryId").value(11L))
                .andExpect(jsonPath("$.categories[1].name").value("개인"));
    }

    @Test
    @DisplayName("카테고리 수정 요청은 204를 반환한다")
    void updateCategoryReturnsNoContent() throws Exception {
        Long memberId = 5L;
        Long categoryId = 10L;
        CategoryRequest request = new CategoryRequest("개인");

        doNothing().when(categoryService).updateCategory(eq(memberId), eq(categoryId), any(CategoryRequest.class));
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("카테고리 수정 요청 검증 실패 시 400을 반환한다")
    void updateCategoryReturnsValidationError() throws Exception {
        Long memberId = 5L;
        Long categoryId = 10L;
        CategoryRequest request = new CategoryRequest("");
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카테고리 삭제 요청은 204를 반환한다")
    void deleteCategoryReturnsNoContent() throws Exception {
        Long memberId = 5L;
        Long categoryId = 10L;

        doNothing().when(categoryService).deleteCategory(memberId, categoryId);
        authenticate(memberId);

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증 없이 카테고리 목록을 조회하면 401을 반환한다")
    void getCategoriesReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("서비스 예외는 지정된 상태 코드로 매핑된다")
    void serviceExceptionReturnsConfiguredStatus() throws Exception {
        Long memberId = 5L;
        doThrow(new BusinessException(CategoryError.CATEGORY_DUPLICATED))
                .when(categoryService)
                .updateCategory(eq(memberId), eq(10L), any(CategoryRequest.class));
        authenticate(memberId);

        mockMvc.perform(patch("/api/v1/categories/{categoryId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("개인"))))
                .andExpect(status().isConflict());
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
