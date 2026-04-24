package com.dodo.todo.todo.repository;

import com.dodo.todo.todo.domain.TodoHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoHistoryRepository extends JpaRepository<TodoHistory, Long> {

    @Query("""
            select history
            from TodoHistory history
            left join fetch history.todo todo
            where history.member.id = :memberId
              and (:todoId is null or todo.id = :todoId)
              and (
                    :cursorCompletedAt is null
                    or history.completedAt < :cursorCompletedAt
                    or (history.completedAt = :cursorCompletedAt and history.id < :cursorId)
              )
            order by history.completedAt desc, history.id desc
            """)
    List<TodoHistory> findHistories(
            @Param("memberId") Long memberId,
            @Param("todoId") Long todoId,
            @Param("cursorCompletedAt") LocalDateTime cursorCompletedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
