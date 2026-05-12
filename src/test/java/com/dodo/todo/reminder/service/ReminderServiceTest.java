package com.dodo.todo.reminder.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.domain.ReminderError;
import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.reminder.dto.ReminderResponse;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.repository.TodoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.dodo.todo.util.TestFixture.createCategory;
import static com.dodo.todo.util.TestFixture.createMember;
import static com.dodo.todo.util.TestFixture.createReminder;
import static com.dodo.todo.util.TestFixture.createScheduledTodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ReminderService reminderService;

    @Test
    @DisplayName("Todo에 알림을 생성한다")
    void createReminderSuccess() {
        Long memberId = 1L;
        Long todoId = 10L;
        Member member = createMember(memberId);
        Todo todo = scheduledTodo(todoId, member);
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);
        when(reminderRepository.existsByTodoIdAndMinuteOffset(todoId, 10)).thenReturn(false);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderResponse response = reminderService.createReminder(memberId, todoId, new ReminderRequest(10));

        assertThat(response.minuteOffset()).isEqualTo(10);
        assertThat(response.remindAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 8, 50));
    }

    @Test
    @DisplayName("Todo에 날짜/시간이 없으면 알림을 생성할 수 없다")
    void rejectCreateReminderWithoutSchedule() {
        Long memberId = 1L;
        Long todoId = 10L;
        Member member = createMember(memberId);
        Todo todo = Todo.builder()
                .member(member)
                .category(createCategory(member, "work"))
                .title("todo")
                .status(TodoStatus.TODO)
                .build();
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> reminderService.createReminder(memberId, todoId, new ReminderRequest(10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_SCHEDULE_REQUIRED.message());
    }

    @Test
    @DisplayName("Todo 하나에 알림을 5개 초과로 생성할 수 없다")
    void rejectReminderLimitExceeded() {
        Long memberId = 1L;
        Long todoId = 10L;
        Member member = createMember(memberId);
        Todo todo = scheduledTodo(todoId, member);
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(5);

        assertThatThrownBy(() -> reminderService.createReminder(memberId, todoId, new ReminderRequest(10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_LIMIT_EXCEEDED.message());
    }

    @Test
    @DisplayName("같은 Todo에 같은 minuteOffset 알림을 중복 생성할 수 없다")
    void rejectDuplicateMinuteOffset() {
        Long memberId = 1L;
        Long todoId = 10L;
        Member member = createMember(memberId);
        Todo todo = scheduledTodo(todoId, member);
        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);
        when(reminderRepository.existsByTodoIdAndMinuteOffset(todoId, 10)).thenReturn(true);

        assertThatThrownBy(() -> reminderService.createReminder(memberId, todoId, new ReminderRequest(10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_OFFSET_DUPLICATED.message());
    }

    @Test
    @DisplayName("수정할 minuteOffset이 이미 존재하면 실패한다")
    void rejectUpdateReminderToDuplicateMinuteOffset() {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = scheduledTodo(todoId, member);
        Reminder reminder = createReminder(reminderId, todo, member, 10);
        when(reminderRepository.findByIdAndTodoIdAndMemberId(reminderId, todoId, memberId))
                .thenReturn(Optional.of(reminder));
        when(reminderRepository.existsByTodoIdAndMinuteOffset(todoId, 30)).thenReturn(true);

        assertThatThrownBy(() -> reminderService.updateReminder(memberId, todoId, reminderId, new ReminderRequest(30)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_OFFSET_DUPLICATED.message());
    }

    @Test
    @DisplayName("요청한 Todo와 Member에 속한 알림만 삭제한다")
    void deleteReminder() {
        Long memberId = 1L;
        Long todoId = 10L;
        Long reminderId = 100L;
        Member member = createMember(memberId);
        Todo todo = scheduledTodo(todoId, member);
        Reminder reminder = createReminder(reminderId, todo, member, 10);
        when(reminderRepository.findByIdAndTodoIdAndMemberId(reminderId, todoId, memberId))
                .thenReturn(Optional.of(reminder));

        reminderService.deleteReminder(memberId, todoId, reminderId);

        verify(reminderRepository).delete(reminder);
    }

    @Test
    @DisplayName("Todo 생성 시 전달된 초기 알림 목록을 저장한다")
    void createInitialReminders() {
        Long todoId = 10L;
        Member member = createMember(1L);
        Todo todo = scheduledTodo(todoId, member);
        when(reminderRepository.countByTodoId(todoId)).thenReturn(0);

        reminderService.createReminders(todo, member, List.of(new ReminderRequest(10), new ReminderRequest(30)));

        ArgumentCaptor<List<Reminder>> captor = ArgumentCaptor.forClass(List.class);
        verify(reminderRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Reminder::getMinuteOffset)
                .containsExactly(10, 30);
    }

    private Todo scheduledTodo(Long todoId, Member member) {
        Category category = createCategory(member, "work");
        return createScheduledTodo(todoId, member, category, "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
    }
}
