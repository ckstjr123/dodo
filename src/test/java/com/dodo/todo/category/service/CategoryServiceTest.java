package com.dodo.todo.category.service;

import static com.dodo.todo.util.TestFixture.createCategory;
import static com.dodo.todo.util.TestFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.domain.CategoryError;
import com.dodo.todo.category.dto.CategoryListResponse;
import com.dodo.todo.category.dto.CategoryRequest;
import com.dodo.todo.category.dto.CategoryResponse;
import com.dodo.todo.category.repository.CategoryRepository;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.todo.repository.TodoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("새 카테고리를 생성하고 생성된 카테고리 ID를 반환한다")
    void createCategoryReturnsNewCategoryId() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Member member = createMember(memberId);
        Category category = createCategory(categoryId, member, "work");
        CategoryRequest request = new CategoryRequest("work");

        when(categoryRepository.findByMemberIdAndName(memberId, request.name())).thenReturn(Optional.empty());
        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Long savedCategoryId = categoryService.createCategory(memberId, request);

        assertThat(savedCategoryId).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("카테고리 추가 시 동일한 이름의 카테고리가 있으면 기존 카테고리 ID를 반환한다")
    void createCategoryReturnsExistingCategoryIdWhenNameDuplicated() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");
        CategoryRequest request = new CategoryRequest("work");

        when(categoryRepository.findByMemberIdAndName(memberId, request.name())).thenReturn(Optional.of(category));

        Long savedCategoryId = categoryService.createCategory(memberId, request);

        assertThat(savedCategoryId).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("회원이 소유한 카테고리 목록을 조회한다")
    void getCategoriesReturnsMemberCategories() {
        Long memberId = 1L;
        Member member = createMember(memberId);
        Category work = createCategory(10L, member, "work");
        Category personal = createCategory(11L, member, "personal");

        when(categoryRepository.findAllByMemberIdOrderByCreatedAtAscIdAsc(memberId))
                .thenReturn(List.of(work, personal));

        CategoryListResponse response = categoryService.getCategories(memberId);

        assertThat(response.categories()).containsExactly(
                new CategoryResponse(10L, "work"),
                new CategoryResponse(11L, "personal")
        );
    }

    @Test
    @DisplayName("본인 카테고리 명을 수정한다")
    void updateCategoryChangesName() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByMemberIdAndName(memberId, request.name())).thenReturn(Optional.empty());

        categoryService.updateCategory(memberId, categoryId, request);

        assertThat(category.getName()).isEqualTo("personal");
    }

    @Test
    @DisplayName("다른 본인 카테고리와 이름이 중복되면 수정할 수 없다")
    void updateCategoryRejectsDuplicatedName() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");
        Category duplicatedCategory = createCategory(11L, createMember(memberId), "personal");
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByMemberIdAndName(memberId, request.name())).thenReturn(Optional.of(duplicatedCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(memberId, categoryId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_DUPLICATED.message());
    }

    @Test
    @DisplayName("존재하지 않거나 소유자가 다른 카테고리는 수정할 수 없다")
    void updateCategoryRejectsNotFoundCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(memberId, categoryId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_NOT_FOUND.message());
    }

    @Test
    @DisplayName("Todo가 연결된 카테고리는 삭제할 수 없다")
    void deleteCategoryRejectsCategoryInUse() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.of(category));
        when(todoRepository.existsByCategoryIdAndMemberId(categoryId, memberId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(memberId, categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_IN_USE.message());
    }

    @Test
    @DisplayName("Todo가 연결되지 않은 본인 카테고리를 삭제한다")
    void deleteCategoryDeletesOwnedCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.of(category));
        when(todoRepository.existsByCategoryIdAndMemberId(categoryId, memberId)).thenReturn(false);

        categoryService.deleteCategory(memberId, categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("존재하지 않거나 소유자가 다른 카테고리는 삭제할 수 없다")
    void deleteCategoryRejectsNotFoundCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;

        when(categoryRepository.findByIdAndMemberId(categoryId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(memberId, categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_NOT_FOUND.message());
    }
}
