package com.dodo.todo.category.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final TodoRepository todoRepository;

    /**
     * 카테고리 생성
     * 같은 회원에게 동일한 이름의 카테고리가 있으면 기존 카테고리 ID를 반환함.
     */
    @Transactional
    public Long createCategory(Long memberId, CategoryRequest request) {
        return categoryRepository.findByMemberIdAndName(memberId, request.name())
                .map(Category::getId)
                .orElseGet(() -> saveCategory(memberId, request.name()));
    }

    /**
     * 카테고리 목록 조회
     * 현재 회원이 소유한 카테고리를 생성 순서대로 조회함.
     */
    @Transactional(readOnly = true)
    public CategoryListResponse getCategories(Long memberId) {
        List<CategoryResponse> categories = categoryRepository.findAllByMemberIdOrderByCreatedAtAscIdAsc(memberId)
                .stream()
                .map(CategoryResponse::from)
                .toList();

        return new CategoryListResponse(categories);
    }

    /**
     * 카테고리 수정
     * 같은 회원의 다른 카테고리와 이름이 중복되면 수정을 거부함.
     */
    @Transactional
    public void updateCategory(Long memberId, Long categoryId, CategoryRequest request) {
        Category category = findCategory(categoryId, memberId);
        validateDuplicateName(category, memberId, request.name());

        category.updateName(request.name());
    }

    /**
     * 카테고리 삭제
     * Todo가 연결된 카테고리는 삭제하지 않음.
     */
    @Transactional
    public void deleteCategory(Long memberId, Long categoryId) {
        Category category = findCategory(categoryId, memberId);

        if (todoRepository.existsByCategoryIdAndMemberId(categoryId, memberId)) {
            throw new BusinessException(CategoryError.CATEGORY_IN_USE);
        }

        categoryRepository.delete(category);
    }

    private Long saveCategory(Long memberId, String name) {
        Member member = memberService.findById(memberId);
        Category category = Category.create(member, name);

        return categoryRepository.save(category).getId();
    }

    private Category findCategory(Long categoryId, Long memberId) {
        return categoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> new BusinessException(CategoryError.CATEGORY_NOT_FOUND));
    }

    /**
     * 카테고리명 중복 검증
     * 대상 카테고리가 아니면서 동일한 이름을 가진 자신의 카테고리가 이미 있으면 중복.
     */
    private void validateDuplicateName(Category category, Long memberId, String name) {
        categoryRepository.findByMemberIdAndName(memberId, name)
                .filter(findCategory -> !findCategory.getId().equals(category.getId()))
                .ifPresent(duplicatedCategory -> {
                    throw new BusinessException(CategoryError.CATEGORY_DUPLICATED);
                });
    }
}
