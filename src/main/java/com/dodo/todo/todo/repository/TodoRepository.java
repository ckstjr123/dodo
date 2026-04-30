package com.dodo.todo.todo.repository;

import com.dodo.todo.todo.domain.Todo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            SELECT todo
            FROM Todo todo
            WHERE todo.id = :parentTodoId
              and todo.member.id = :memberId
            """)
    Optional<Todo> findByIdAndMemberId(@Param("parentTodoId") Long todoId, @Param("memberId") Long memberId);

    /**
     * Todo 목록과 바로 아래 하위 작업을 함께 조회한다.
     * 현재 Todo 계층은 하위 작업 1단계까지만 허용하므로 깊이는 최대 2다.
     */
    @Query("""
            SELECT todo
            FROM Todo todo
            JOIN FETCH todo.category
            LEFT JOIN FETCH todo.subTodos subTodo
            LEFT JOIN FETCH subTodo.category
            WHERE todo.member.id = :memberId
              and todo.mainTodo is null
              and todo.status = com.dodo.todo.todo.domain.TodoStatus.TODO
            ORDER BY todo.sortOrder asc, todo.id desc
            """)
    List<Todo> findWithSubTodos(@Param("memberId") Long memberId);

    /**
     * Todo 단건과 바로 아래 하위 작업을 함께 조회한다.
     * 현재 Todo 계층은 하위 작업 1단계까지만 허용하므로 깊이는 최대 2다.
     */
    @Query("""
            SELECT todo
            FROM Todo todo
            JOIN FETCH todo.category
            LEFT JOIN FETCH todo.subTodos subTodo
            LEFT JOIN FETCH subTodo.category
            WHERE todo.id = :todoId
              and todo.member.id = :memberId
            """)
    Optional<Todo> findWithSubTodos(@Param("todoId") Long todoId,
                                    @Param("memberId") Long memberId);
}
