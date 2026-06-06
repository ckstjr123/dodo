package com.dodo.todo.reminder.service;

import static com.dodo.todo.util.TestFixture.createAbsoluteReminder;
import static com.dodo.todo.util.TestFixture.createCategory;
import static com.dodo.todo.util.TestFixture.createMember;
import static com.dodo.todo.util.TestFixture.createTodo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.member.domain.Member;
import com.dodo.todo.notification.service.NotificationService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ReminderScheduleServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ScheduledFuture<Object> scheduledFuture;

    @InjectMocks
    private ReminderScheduleService reminderScheduleService;

    @Test
    @DisplayName("미래 Reminder는 예약한다")
    void scheduleFutureReminder() {
        Reminder reminder = futureReminder(100L);
        doReturn(scheduledFuture)
                .when(taskScheduler)
                .schedule(any(Runnable.class), any(Instant.class));

        reminderScheduleService.schedule(reminder);

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("과거 Reminder는 예약하지 않는다")
    void skipPastReminder() {
        Reminder reminder = pastReminder(100L);

        reminderScheduleService.schedule(reminder);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("예약된 Reminder 발송 시 PushService를 호출한다")
    void sendReminder() {
        Long reminderId = 100L;
        Reminder reminder = pastReminder(reminderId);
        reminder.getMember().updateFcmToken("fcm-token");
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));

        reminderScheduleService.send(reminderId);

        verify(notificationService).send("fcm-token", "Todo 알림", reminder.getTodo().getTitle());
    }

    @Test
    @DisplayName("완료된 Todo의 Reminder는 발송하지 않는다")
    void skipDoneTodoReminder() {
        Long reminderId = 100L;
        Reminder reminder = pastReminder(reminderId, TodoStatus.DONE);
        reminder.getMember().updateFcmToken("fcm-token");
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));

        reminderScheduleService.send(reminderId);

        verify(notificationService, never()).send(any(), any(), any());
    }

    private Reminder futureReminder(Long reminderId) {
        return createReminder(reminderId, TodoStatus.TODO, LocalDateTime.now().plusMinutes(10));
    }

    private Reminder pastReminder(Long reminderId) {
        return pastReminder(reminderId, TodoStatus.TODO);
    }

    private Reminder pastReminder(Long reminderId, TodoStatus status) {
        return createReminder(reminderId, status, LocalDateTime.now().minusMinutes(10));
    }

    private Reminder createReminder(Long reminderId, TodoStatus status, LocalDateTime due) {
        Member member = createMember(1L);
        Todo todo = createTodo(10L, member, createCategory(member, "work"), "todo", status);
        return createAbsoluteReminder(reminderId, todo, member, due);
    }
}
