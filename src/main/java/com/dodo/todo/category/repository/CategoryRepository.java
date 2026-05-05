package com.dodo.todo.category.repository;

import com.dodo.todo.category.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByMemberIdOrderByCreatedAtAscIdAsc(Long memberId);

    Optional<Category> findByIdAndMemberId(Long categoryId, Long memberId);

    Optional<Category> findByMemberIdAndName(Long memberId, String name);
}
