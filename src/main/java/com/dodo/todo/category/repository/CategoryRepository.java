package com.dodo.todo.category.repository;

import com.dodo.todo.category.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByMember_IdOrderByCreatedAtAscIdAsc(Long memberId);

    Optional<Category> findByIdAndMember_Id(Long categoryId, Long memberId);

    Optional<Category> findByMember_IdAndName(Long memberId, String name);
}
