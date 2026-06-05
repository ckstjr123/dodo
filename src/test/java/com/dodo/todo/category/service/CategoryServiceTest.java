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
    @DisplayName("create category returns new category id")
    void createCategoryReturnsNewCategoryId() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Member member = createMember(memberId);
        CategoryRequest request = new CategoryRequest("work");

        when(categoryRepository.findByMember_IdAndName(memberId, request.name())).thenReturn(Optional.empty());
        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.save(any(Category.class))).thenReturn(createCategory(categoryId, member, "work"));

        Long savedCategoryId = categoryService.saveCategory(memberId, request);

        assertThat(savedCategoryId).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("create category returns existing category id when name duplicated")
    void createCategoryReturnsExistingCategoryIdWhenNameDuplicated() {
        Long memberId = 1L;
        Long categoryId = 10L;
        String categoryName = "work";
        Category category = createCategory(categoryId, createMember(memberId), categoryName);
        CategoryRequest request = new CategoryRequest(categoryName);

        when(categoryRepository.findByMember_IdAndName(memberId, request.name())).thenReturn(Optional.of(category));

        Long savedCategoryId = categoryService.saveCategory(memberId, request);

        assertThat(savedCategoryId).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("get categories returns member categories")
    void getCategoriesReturnsMemberCategories() {
        Long memberId = 1L;
        Member member = createMember(memberId);
        Category work = createCategory(10L, member, "work");
        Category personal = createCategory(11L, member, "personal");

        when(categoryRepository.findAllByMember_IdOrderByCreatedAtAscIdAsc(memberId))
                .thenReturn(List.of(work, personal));

        CategoryListResponse response = categoryService.getCategories(memberId);

        assertThat(response.categories()).containsExactly(
                new CategoryResponse(10L, "work"),
                new CategoryResponse(11L, "personal")
        );
    }

    @Test
    @DisplayName("update category changes name")
    void updateCategoryChangesName() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(createMember(memberId), "work");
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByMember_IdAndName(memberId, request.name())).thenReturn(Optional.empty());

        categoryService.updateCategory(memberId, categoryId, request);

        assertThat(category.getName()).isEqualTo("personal");
    }

    @Test
    @DisplayName("update category rejects duplicated name")
    void updateCategoryRejectsDuplicatedName() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(categoryId, createMember(memberId), "work");
        Category duplicatedCategory = createCategory(11L, createMember(memberId), "personal");
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByMember_IdAndName(memberId, request.name())).thenReturn(Optional.of(duplicatedCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(memberId, categoryId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_DUPLICATED.message());
    }

    @Test
    @DisplayName("update category rejects missing category")
    void updateCategoryRejectsNotFoundCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;
        CategoryRequest request = new CategoryRequest("personal");

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(memberId, categoryId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_NOT_FOUND.message());
    }

    @Test
    @DisplayName("delete category rejects category in use")
    void deleteCategoryRejectsCategoryInUse() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(createMember(memberId), "work");

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.of(category));
        when(todoRepository.existsByCategoryIdAndMemberId(categoryId, memberId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(memberId, categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_IN_USE.message());
    }

    @Test
    @DisplayName("delete category deletes owned category")
    void deleteCategoryDeletesOwnedCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Category category = createCategory(createMember(memberId), "work");

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.of(category));
        when(todoRepository.existsByCategoryIdAndMemberId(categoryId, memberId)).thenReturn(false);

        categoryService.deleteCategory(memberId, categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("delete category rejects missing category")
    void deleteCategoryRejectsNotFoundCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;

        when(categoryRepository.findByIdAndMember_Id(categoryId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(memberId, categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CategoryError.CATEGORY_NOT_FOUND.message());
    }
}
