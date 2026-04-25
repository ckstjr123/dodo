package com.dodo.todo.todo.repository;

import com.dodo.todo.todo.domain.TodoHistory;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoHistoryRepository extends JpaRepository<TodoHistory, Long> {

    @Query("""
            select history
            from TodoHistory history
            where history.member.id = :memberId
              and (:parentTodoId is null or history.parentTodoId = :parentTodoId)
              and (
                    :cursorCompletedAt is null
                    or history.completedAt < :cursorCompletedAt
                    or (history.completedAt = :cursorCompletedAt and history.id < :cursorId)
              )
            order by history.completedAt desc, history.id desc
            """)
    Slice<TodoHistory> findHistories(
            @Param("memberId") Long memberId,
            @Param("parentTodoId") Long todoId,
            @Param("cursorCompletedAt") LocalDateTime cursorCompletedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
