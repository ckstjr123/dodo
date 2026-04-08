package com.dodo.todo.todo.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            SELECT todo
            FROM Todo todo
            JOIN FETCH todo.category
            LEFT JOIN FETCH todo.repeat
            WHERE todo.member.id = :memberId
            ORDER BY todo.sortOrder ASC, todo.id DESC
            """)
    List<Todo> findWithCategoryAndRepeatByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT todo
            FROM Todo todo
            JOIN FETCH todo.category
            LEFT JOIN FETCH todo.repeat
            WHERE todo.id = :todoId
            """)
    Optional<Todo> findWithCategoryAndRepeatById(@Param("todoId") Long todoId);
}
