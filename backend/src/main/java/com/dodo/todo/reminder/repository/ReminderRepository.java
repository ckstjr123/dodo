package com.dodo.todo.reminder.repository;

import com.dodo.todo.reminder.domain.Reminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    int countByTodoId(Long todoId);

    List<Reminder> findAllByTodoId(Long todoId);

    @Query("""
            SELECT reminder.id
            FROM Reminder reminder
            WHERE reminder.todo.id = :todoId
            """)
    List<Long> findIdsByTodoId(@Param("todoId") Long todoId);

    @Query("""
            SELECT reminder.id
            FROM Reminder reminder
            WHERE reminder.todo.id IN (
                SELECT todo.id
                FROM Todo todo
                WHERE todo.mainTodo.id = :parentTodoId
            )
            """)
    List<Long> findIdsByParentTodoId(@Param("parentTodoId") Long parentTodoId);

    @Query("""
            SELECT reminder
            FROM Reminder reminder
            WHERE reminder.id = :reminderId
              AND reminder.member.id = :memberId
            """)
    Optional<Reminder> findByIdAndMemberId(@Param("reminderId") Long reminderId,
                                            @Param("memberId") Long memberId);

    @Modifying // clearAutomatically = true
    @Query("""
            DELETE FROM Reminder reminder
            WHERE reminder.todo.id IN (
                SELECT todo.id
                FROM Todo todo
                WHERE todo.mainTodo.id = :parentTodoId
            )
            """)
    void deleteByParentTodoId(@Param("parentTodoId") Long parentTodoId);

    @Modifying // clearAutomatically = true
    @Query("""
            DELETE FROM Reminder reminder
            WHERE reminder.todo.id = :todoId
            """)
    void deleteByTodoId(@Param("todoId") Long todoId);
}
