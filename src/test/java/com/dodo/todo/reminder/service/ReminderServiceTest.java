package com.dodo.todo.reminder.service;

import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.reminder.domain.*;
import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.repository.TodoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.dodo.todo.util.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private ReminderScheduleService reminderScheduleService;

    @InjectMocks
    private ReminderService reminderService;

    @Test
    @DisplayName("상대 알림을 등록한다")
    void saveReminderSuccess() {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(todoId, member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        Reminder reminder = createRelativeReminder(reminderId, todo, member, 10);
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);
        when(reminderRepository.save(any(Reminder.class))).thenReturn(reminder);

        Long savedReminderId = reminderService.saveReminder(memberId, new ReminderCreateRequest(todoId, null, 10, null));

        assertThat(savedReminderId).isEqualTo(reminderId);
        verify(reminderScheduleService).schedule(reminder);
    }

    @Test
    @DisplayName("절대 알림을 등록한다")
    void saveAbsoluteReminder() {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(todoId, member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        LocalDateTime due = LocalDateTime.of(2026, 5, 19, 8, 0);
        Reminder reminder = createAbsoluteReminder(reminderId, todo, member, due);
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);
        when(reminderRepository.save(any(Reminder.class))).thenReturn(reminder);

        Long savedReminderId = reminderService.saveReminder(
                memberId,
                new ReminderCreateRequest(todoId, ReminderType.ABSOLUTE, null, due)
        );

        assertThat(savedReminderId).isEqualTo(reminderId);
    }

    @Test
    @DisplayName("각 todo 알림은 5개 이하여야 한다")
    void rejectReminderLimitExceeded() {
        Long memberId = 1L;
        Long todoId = 10L;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(todoId, member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(5);

        assertThatThrownBy(() -> reminderService.saveReminder(memberId, new ReminderCreateRequest(todoId, null, 10, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_LIMIT_EXCEEDED.message());
    }

    @Test
    @DisplayName("상대 알림 설정을 수정한다")
    void updateReminderOverwritesSettings() {
        Long memberId = 1L;
        Long reminderId = 100L;
        int minuteOffset = 30;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        RelativeReminder reminder = RelativeReminder.create(todo, member, 10);
        when(reminderRepository.findByIdAndMemberId(reminderId, memberId))
                .thenReturn(Optional.of(reminder));

        reminderService.updateReminder(memberId, reminderId, new ReminderUpdateRequest(minuteOffset, null));

        assertThat(reminder.getType()).isEqualTo(ReminderType.RELATIVE);
        assertThat(reminder.getMinuteOffset()).isEqualTo(minuteOffset);
        verify(reminderScheduleService).schedule(reminder);
    }

    @Test
    @DisplayName("절대 알림의 due를 수정한다")
    void updateAbsoluteReminder() {
        Long memberId = 1L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        LocalDateTime due = LocalDateTime.of(2026, 5, 19, 8, 0);
        LocalDateTime changedDue = LocalDateTime.of(2026, 5, 21, 8, 0);
        AbsoluteReminder reminder = AbsoluteReminder.create(todo, member, due);
        when(reminderRepository.findByIdAndMemberId(reminderId, memberId))
                .thenReturn(Optional.of(reminder));

        reminderService.updateReminder(
                memberId,
                reminderId,
                new ReminderUpdateRequest(null, changedDue)
        );

        assertThat(reminder.getType()).isEqualTo(ReminderType.ABSOLUTE);
        assertThat(reminder.getDue()).isEqualTo(changedDue);
    }

    @Test
    @DisplayName("요청한 Member에 속한 알림만 삭제한다")
    void deleteReminder() {
        Long memberId = 1L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        Reminder reminder = AbsoluteReminder.create(todo, member, LocalDateTime.of(2026, 5, 19, 8, 0));
        when(reminderRepository.findByIdAndMemberId(reminderId, memberId))
                .thenReturn(Optional.of(reminder));

        reminderService.deleteReminder(memberId, reminderId);

        verify(reminderScheduleService).cancel(reminderId);
        verify(reminderRepository).delete(reminder);
    }

    @Test
    @DisplayName("Todo 생성 시 전달된 초기 알림 목록을 저장한다")
    void saveReminders() {
        Long todoId = 10L;
        Long reminderId1 = 100L, reminderId2 = 101L;
        Member member = createMember(1L);
        Todo todo = createScheduledTodo(todoId, member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        int minuteOffset1 = 10, minuteOffset2 = 30;
        Reminder reminder1 = createRelativeReminder(reminderId1, todo, member, minuteOffset1);
        Reminder reminder2 = createRelativeReminder(reminderId2, todo, member, minuteOffset2);
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);
        when(reminderRepository.saveAll(anyList())).thenReturn(List.of(reminder1, reminder2));

        List<Long> savedReminderIds = reminderService.saveReminders(todo, member, List.of(
                new ReminderCreateRequest(todoId, null, minuteOffset1, null),
                new ReminderCreateRequest(todoId, null, minuteOffset2, null)
        ));

        assertThat(savedReminderIds).containsExactly(reminderId1, reminderId2);
    }
}
